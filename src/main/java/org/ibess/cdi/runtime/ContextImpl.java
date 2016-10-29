package org.ibess.cdi.runtime;

import org.ibess.cdi.*;
import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.exceptions.ImpossibleError;
import org.ibess.cdi.internal.$CdiObject;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.internal.$Descriptor;
import org.ibess.cdi.internal.$Instantiator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.ibess.cdi.enums.CdiErrorType.ILLEGAL_ACCESS;

/**
 * @author ibessonov
 */
public final class ContextImpl implements $Context {

    private static final AtomicInteger counter = new AtomicInteger();

    final Map<Class, ArrayList<ValueTransformer>> valueTransformers = new HashMap<>();
    final Map<Class, ArrayList<MethodTransformer>> methodTransformers = new HashMap<>();
    final Map<Class, Provider> providers = new HashMap<>();
    final String contextId = Integer.toString(counter.getAndIncrement());
    final InheritorGenerator generator = new InheritorGenerator(this, contextId);

    public ContextImpl(Extension... extensions) {
        if (extensions.length != 0) {
            Registrar registrar = new RegistrarImpl();
            for (Extension extension : extensions) {
                extension.register(registrar);
            }
            // reduce memory consumption
            for (ArrayList<ValueTransformer> list : valueTransformers.values()) {
                list.trimToSize();
            }
            for (ArrayList<MethodTransformer> list : methodTransformers.values()) {
                list.trimToSize();
            }
        }
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

    @SuppressWarnings("unchecked")
    public <T extends Annotation> ValueTransformer<T> getValueTransformer(Class<T> clazz) {
        ArrayList<ValueTransformer> list = this.valueTransformers.get(clazz);
        if (list == null) return null;
        if (list.size() == 1) {
            return list.get(0);
        }
        return (annotation, c, object) -> {
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
        ArrayList<MethodTransformer> list = this.methodTransformers.get(clazz);
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

    private static final ConcurrentMap<Class, $Instantiator> instantiators = new ConcurrentHashMap<>();
    private $CdiObject instantiate($Descriptor d) {
        return instantiators.computeIfAbsent(d.c, this::getInstantiator).$create(this, d.p);
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
