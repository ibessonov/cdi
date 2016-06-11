package org.ibess.cdi.runtime;

import org.ibess.cdi.Context;
import org.ibess.cdi.Extension;
import org.ibess.cdi.Registrar;
import org.ibess.cdi.Transformer;
import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.exceptions.ImpossibleError;
import org.ibess.cdi.internal.$CdiObject;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.internal.$Descriptor;
import org.ibess.cdi.internal.$Instantiator;

import java.lang.annotation.Annotation;
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

    private static final Map<Class, Class> defaults = new HashMap<>();
    private static final AtomicInteger counter = new AtomicInteger();

    private final Map<Class, ArrayList<Transformer>> transformers = new HashMap<>();
    private final InheritorGenerator generator = new InheritorGenerator(this, Integer.toString(counter.getAndIncrement()));

    public ContextImpl(Extension... extensions) {
        if (extensions.length != 0) {
            Registrar registrar = new RegistrarImpl();
            for (Extension extension : extensions) {
                extension.register(registrar);
            }
            for (ArrayList<Transformer> list : transformers.values()) {
                list.trimToSize(); // reduce memory consumption
            }
        }
    }

    private class RegistrarImpl implements Registrar {

        @Override
        public <T extends Annotation> void registerTransformer(Class<T> clazz, Transformer<T> transformer) {
            transformers.computeIfAbsent(clazz, c -> new ArrayList<>()).add(transformer);
        }
    }

    @SuppressWarnings("unchecked")
    public Transformer getTransformer(Class clazz) {
        ArrayList<Transformer> list = this.transformers.get(clazz);
        if (list == null) return null;
        return (object, annotation) -> {
            for (int i = 0, len = list.size(); i < len; i++) {
                object = list.get(i).transform(object, annotation);
            }
            return object;
        };
    }

    public boolean transformerRegistered(Class clazz) {
        return transformers.containsKey(clazz);
    }

    @Override public Object $unscoped(Class clazz) {
        if (clazz.isInterface()) {
            clazz = defaults.get(clazz);
        }
        return instantiate(clazz);
    }

    private final Map<$Descriptor, Object> singletons = new HashMap<>();
    private final ReadWriteLock            rwLock     = new ReentrantReadWriteLock();
    @Override public Object $singleton($Descriptor d) {
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
                    return cdiObject;
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        return object;
    }

    private final ThreadLocal<Map<$Descriptor, Object>> dejaVu = ThreadLocal.withInitial(HashMap::new);
    @Override public Object $stateless($Descriptor d) {
        Map<$Descriptor, Object> dejaVu = this.dejaVu.get();
        Object object = dejaVu.get(d);
        if (object == null) {
            $CdiObject cdiObject = instantiate(d);
            dejaVu.put(d, cdiObject);
            try {
                cdiObject.$construct();
            } finally {
                dejaVu.remove(d);
            }
            return cdiObject;
        }
        return object;
    }

    private static Object instantiate(Class clazz) {
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

    private $Instantiator getInstantiator(Class clazz) {
        Class cdiImpl = generator.getSubclass(clazz);
        try {
            return ($Instantiator) cdiImpl.getDeclaredField("$i").get(null);
        } catch (Throwable t) {
            throw new ImpossibleError(t);
        }
    }

    private static void bind(Class sup, Class sub) {
        defaults.put(sup, sub);
    }

    static {
        bind(Context.class, ContextImpl.class);
        bind(List.class, ArrayList.class);
        bind(Map.class, HashMap.class);
        bind(Set.class, HashSet.class);
        bind(SortedMap.class, TreeMap.class);
        bind(SortedSet.class, TreeSet.class);
        bind(ConcurrentMap.class, ConcurrentHashMap.class);
    }
}
