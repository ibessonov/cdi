package org.ibess.cdi;

import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.exceptions.ImpossibleError;
import org.ibess.cdi.internal.$CdiObject;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.internal.$Descriptor;
import org.ibess.cdi.internal.$Instantiator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.ibess.cdi.enums.CdiErrorType.ILLEGAL_ACCESS;
import static org.ibess.cdi.runtime.InheritorGenerator.getSubclass;

/**
 * @author ibessonov
 */
final class ContextImpl implements $Context {

    private static final Map<Class, Class> defaults = new HashMap<>();
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

    @Override
    public void cleanupThreadLocals() {
        requestScoped.get().clear();
    }

    private final ThreadLocal<Map<$Descriptor, Object>> requestScoped = ThreadLocal.withInitial(HashMap::new);
    @Override public Object $request($Descriptor d) {
        Map<$Descriptor, Object> requestScoped = this.requestScoped.get();
        Object object = requestScoped.get(d);
        if (object == null) {
            $CdiObject cdiObject = instantiate(d);
            requestScoped.put(d, cdiObject);
            cdiObject.$construct();
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
        $Instantiator instantiator = instantiators.computeIfAbsent(d.c, this::getInstantiator);
        return instantiator.$create(this, d.p);
    }

    private $Instantiator getInstantiator(Class clazz) {
        Class cdiImpl = getSubclass(clazz);
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
        bind(SortedMap.class, TreeMap.class);
        bind(ConcurrentMap.class, ConcurrentHashMap.class);
    }
}
