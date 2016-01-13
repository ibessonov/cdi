package org.ibess.cdi;

import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.enums.Scope;
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

import static org.ibess.cdi.reflection.InheritorGenerator.getSubclass;
import static org.ibess.cdi.reflection.ReflectionUtil.newInstance;
import static org.ibess.cdi.util.Cdi.silent;

/**
 * @author ibessonov
 */
final class ContextImpl implements $Context {

    @Override
    public Object $lookup($Descriptor d) {
        Scope scope = scope(d.c);
        if (scope == null) {
            return newUnscoped(d.c);
        }
        switch (scope) {
            case SINGLETON:
                return lookupSingleton(d);
            case STATELESS:
                return lookupStateless(d);
            case REQUEST:
                return lookupRequestScoped(d);
            default:
                throw new ImpossibleError();
        }
    }

    private static final Map<Class, Class> defaults = new HashMap<>();
    private Object newUnscoped(Class clazz) {
        if (clazz.isInterface()) {
            clazz = defaults.get(clazz);
        }
        return newInstance(clazz);
    }

    private final Map<$Descriptor, Object> singletons = new HashMap<>();
    private final ReadWriteLock            rwLock     = new ReentrantReadWriteLock();
    private Object lookupSingleton($Descriptor d) {
        rwLock.readLock().lock();
        Object object = singletons.get(d);
        rwLock.readLock().unlock();

        if (object == null) {
            rwLock.writeLock().lock();
            try {
                if ((object = singletons.get(d)) == null) {
                    object = instantiate(d);
                    singletons.put(d, object);
                    try {
                        construct(object);
                    } catch (RuntimeException | Error e) {
                        singletons.remove(d);
                        throw e;
                    }
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        return object;
    }

    private final ThreadLocal<Map<$Descriptor, Object>> dejaVu = ThreadLocal.withInitial(HashMap::new);
    private Object lookupStateless($Descriptor d) {
        Map<$Descriptor, Object> dejaVu = this.dejaVu.get();
        Object object = dejaVu.get(d);
        if (object == null) {
            object = instantiate(d);
            dejaVu.put(d, object);
            try {
                construct(object);
            } finally {
                dejaVu.remove(d);
            }
        }
        return object;
    }

    @Override
    public void startRequest() {}

    @Override
    public void finishRequest() {
        requestScoped.get().clear();
    }

    private final ThreadLocal<Map<$Descriptor, Object>> requestScoped = ThreadLocal.withInitial(HashMap::new);
    private Object lookupRequestScoped($Descriptor d) {
        Map<$Descriptor, Object> requestScoped = this.requestScoped.get();
        Object object = requestScoped.get(d);
        if (object == null) {
            object = instantiate(d);
            requestScoped.put(d, object);
            construct(object);
        }
        return object;
    }

    private void construct(Object object) {
        if (object instanceof $CdiObject) {
            (($CdiObject) object).$construct();
        }
    }

    private static final ConcurrentMap<Class, $Instantiator> instantiators = new ConcurrentHashMap<>();
    private Object instantiate($Descriptor d) {
        $Instantiator instantiator = instantiators.computeIfAbsent(getSubclass(d.c), c -> ($Instantiator) silent(() ->
            c.getDeclaredField("$i").get(null)
        ));
        return instantiator.$create(this, d.p);
    }

    private Scope scope(Class<?> clazz) {
        Scoped scoped = clazz.getAnnotation(Scoped.class);
        return (scoped == null) ? null : scoped.value();
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
