package com.github.ibessonov.cdi.runtime;

import com.github.ibessonov.cdi.Extension;
import com.github.ibessonov.cdi.Provider;
import com.github.ibessonov.cdi.Registrar;
import com.github.ibessonov.cdi.annotations.MethodTransformer;
import com.github.ibessonov.cdi.annotations.ValueTransformer;
import com.github.ibessonov.cdi.exceptions.CdiException;
import com.github.ibessonov.cdi.exceptions.ImpossibleError;
import com.github.ibessonov.cdi.internal.$CdiObject;
import com.github.ibessonov.cdi.internal.$Context;
import com.github.ibessonov.cdi.internal.$Descriptor;
import com.github.ibessonov.cdi.internal.$Instantiator;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.ibessonov.cdi.enums.CdiErrorType.ILLEGAL_ACCESS;
import static com.github.ibessonov.cdi.runtime.CdiClassLoader.defineClass;
import static com.github.ibessonov.cdi.runtime.StCompiler.compile;
import static com.github.ibessonov.cdi.runtime.st.Dsl.*;

/**
 * @author ibessonov
 */
public final class ContextImpl implements $Context {

    private static final AtomicInteger counter = new AtomicInteger();
    private static final Map<String, WeakReference<ContextImpl>> contexts = new ConcurrentHashMap<>();

    final Map<Class, ArrayList<ValueTransformer<?>>> valueTransformers = new HashMap<>();
    final Map<Class, ArrayList<MethodTransformer<?>>> methodTransformers = new HashMap<>();
    final Map<Class, Provider> providers = new HashMap<>();
    final String contextId = Integer.toString(counter.getAndIncrement());
    final InheritorGenerator generator;

    public ContextImpl(Extension... extensions) {
        if (extensions.length != 0) {
            Registrar registrar = new RegistrarImpl();
            for (Extension extension : extensions) {
                extension.register(registrar);
            }
            // reduce memory consumption
            for (ArrayList<ValueTransformer<?>> list : valueTransformers.values()) {
                list.trimToSize();
            }
            for (ArrayList<MethodTransformer<?>> list : methodTransformers.values()) {
                list.trimToSize();
            }
        }
        contexts.put(contextId, new WeakReference<>(this));

        String contextHolderName = ContextImpl.class.getPackage() + ".$ContextHolder$" + contextId;
        defineClass(compile(_class(_named(contextHolderName), _extends(Object.class), _implements(), _withFields(
            _staticField("$context", $Context.class)
        ), _withMethods(
            _staticMethod(_named("<clinit>"), _withoutParameterTypes(), _returnsNothing(), _withBody(
                _assignStatic(_named("$context"), _ofClass(contextHolderName), _withType($Context.class), _value(
                    _invokeDynamic("", _withoutParameterTypes(), _returns($Context.class),
                        "context", CdiMetafactory.class, _withoutParameters(), contextId
                    )
                ))
            ))
        ))));

        generator = new InheritorGenerator(this, contextId, contextHolderName);
    }

    public static ContextImpl findContext(String contextId) {
        WeakReference<ContextImpl> contextReference = contexts.get(contextId);
        if (contextReference != null) {
            ContextImpl context = contextReference.get();
            if (context != null) {
                return context;
            } else {
                contexts.remove(contextId);
            }
        }
        throw new IllegalArgumentException("Unable to find cdi context with id " + contextId);
    }

    private class RegistrarImpl implements Registrar {

        @Override
        public <T extends Annotation> void registerValueTransformer(Class<T> clazz, ValueTransformer<T> valueTransformer) {
            valueTransformers.computeIfAbsent(clazz, c -> new ArrayList<>(1)).add(valueTransformer);
        }

        @Override
        public <T extends Annotation> void registerMethodTransformer(Class<T> clazz, MethodTransformer<T> methodTransformer) {
            methodTransformers.computeIfAbsent(clazz, c -> new ArrayList<>(1)).add(methodTransformer);
        }

        @Override
        public <T> void registerProvider(Class<T> clazz, Provider<T> provider) {
            addProvider(clazz, provider);
        }
    }

    public boolean valueTransformerRegistered(Class<?> clazz) {
        return valueTransformers.containsKey(clazz);
    }

    public void assertValueCanBeTransformed(Class<? extends Annotation> clazz, Class<?> parameterType) throws CdiException {
        ArrayList<ValueTransformer<?>> valueTransformers = this.valueTransformers.get(clazz);
        if (valueTransformers != null) {
            for (ValueTransformer<?> transformer : valueTransformers) {
                if (!transformer.isApplicable(parameterType)) {
                    String message = "ValueTransformer '" + transformer + "'"
                            + " for annotation '" + clazz.getSimpleName() + "'"
                            + " cannot be applied to type '" + parameterType.getSimpleName() + "'";
                    throw new CdiException(message);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> ValueTransformer<T> getValueTransformer(Class<T> clazz) {
        List<? extends ValueTransformer<T>> list = (List) valueTransformers.get(clazz);
        if (list == null) return null;
        if (list.size() == 1) {
            return list.get(0);
        }
        return (annotation, c, object) -> {
            //noinspection ForLoopReplaceableByForEach, it's known that "list" is RandomAccess
            for (int i = 0, len = list.size(); i < len; i++) {
                object = list.get(i).transform(annotation, c, object);
            }
            return object;
        };
    }

    public boolean methodTransformerRegistered(Class<?> clazz) {
        return methodTransformers.containsKey(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> MethodTransformer<T> getMethodTransformer(Class<T> clazz) {
        List<? extends MethodTransformer<T>> list = (List) methodTransformers.get(clazz);
        if (list == null) return null;
        if (list.size() == 1) {
            return list.get(0);
        }
        return (annotation, method, statement) -> {
            for (int i = list.size() - 1; i >= 0; i--) {
                statement = list.get(i).transform(annotation, method, statement);
            }
            return statement;
        };
    }

    public boolean canBeInjected(Class<?> unscoped) {
        if (providers.containsKey(unscoped)) return true;
        if (Modifier.isAbstract(unscoped.getModifiers())) return false;
        try {
            Constructor<?> constructor = unscoped.getConstructor();
            return Modifier.isPublic(constructor.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Override
    public <T> T $unscoped(Class<T> clazz) {
        assert canBeInjected(clazz); //TODO move to the proper place
        Provider<?> provider = providers.get(clazz);
        return (provider != null) ? clazz.cast(provider.get()) : instantiate(clazz);
    }

    private final Map<$Descriptor<?>, Object> singletons = new HashMap<>();
    private final ReadWriteLock               rwLock     = new ReentrantReadWriteLock();
    @Override
    public <T> T $singleton($Descriptor<T> d) {
        rwLock.readLock().lock();
        Object object = singletons.get(d);
        rwLock.readLock().unlock();

        if (object == null) {
            rwLock.writeLock().lock();
            try {
                if ((object = singletons.get(d)) == null) {
                    $CdiObject cdiObject = instantiate(d);
                    singletons.put(d, cdiObject);
                    try {
                        cdiObject.$construct();
                    } catch (RuntimeException | Error e) {
                        singletons.remove(d);
                        throw e;
                    }
                    object = cdiObject;
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        return d.c.cast(object);
    }

    private final ThreadLocal<Map<$Descriptor<?>, Object>> dejaVu = ThreadLocal.withInitial(HashMap::new);
    @Override
    public <T> T $stateless($Descriptor<T> d) {
        Map<$Descriptor<?>, Object> dejaVu = this.dejaVu.get();
        Object object = dejaVu.get(d);
        if (object == null) {
            $CdiObject cdiObject = instantiate(d);
            dejaVu.put(d, cdiObject);
            try {
                cdiObject.$construct();
            } finally {
                dejaVu.remove(d);
            }
            object = cdiObject;
        }
        return d.c.cast(object);
    }

    private static <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException ie) {
            throw new ImpossibleError(ie);
        } catch (IllegalAccessException iae) {
            throw new CdiException(iae, ILLEGAL_ACCESS);
        }
    }

    private final ConcurrentMap<Class, $Instantiator> instantiators = new ConcurrentHashMap<>();
    private $CdiObject instantiate($Descriptor d) {
        return instantiators.computeIfAbsent(d.c, this::getInstantiator).$create(d.p);
    }

    private $Instantiator getInstantiator(Class<?> clazz) {
        Class<?> cdiImpl = generator.getSubclass(clazz);
        try {
            return ($Instantiator) cdiImpl.getDeclaredField("$i").get(null);
        } catch (Throwable t) {
            throw new ImpossibleError(t);
        }
    }

    private <T> void addProvider(Class<T> clazz, Provider<T> provider) {
        Provider oldProvider = providers.put(clazz, provider);
        assert oldProvider == null;
    }

    {
        addProvider(List.class, ArrayList::new);
        addProvider(Map.class, HashMap::new);
        addProvider(Set.class, HashSet::new);
        addProvider(SortedMap.class, TreeMap::new);
        addProvider(SortedSet.class, TreeSet::new);
        addProvider(ConcurrentMap.class, ConcurrentHashMap::new);
    }
}
