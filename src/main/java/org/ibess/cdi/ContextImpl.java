package org.ibess.cdi;

import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.enums.Scope;
import org.ibess.cdi.exceptions.ImpossibleError;
import org.ibess.cdi.internal.$CdiObject;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.reflection.Descriptor;
import org.ibess.cdi.reflection.InheritorGenerator;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.ibess.cdi.reflection.ReflectionUtil.newInstance;
import static org.ibess.cdi.util.Cdi.silent;

/**
 * @author ibessonov
 */
final class ContextImpl implements $Context {

    @Override
    public <T> T lookup(Descriptor<T> d) {
        return lookup0(d.c, d);
    }

    private <T> T lookup0(Class<T> clazz, Descriptor descriptor) {
        Scope scope = scope(clazz);
        if (scope == null) {
            return newInstance(clazz);
        }
        switch (scope) {
            case SINGLETON:
                return lookupSingleton(clazz, descriptor);
            case STATELESS:
                return lookupStateless(clazz, descriptor);
            case REQUEST:
                return lookupRequestScoped(clazz, descriptor);
            default:
                throw new ImpossibleError();
        }
    }

    private final Map<Class, Object> singletons = new IdentityHashMap<>();
    private final ReadWriteLock rwLock     = new ReentrantReadWriteLock();
    private <T> T lookupSingleton(Class<T> clazz, Descriptor descriptor) {
        rwLock.readLock().lock();
        Object object = singletons.get(clazz);
        rwLock.readLock().unlock();

        if (object == null) {
            rwLock.writeLock().lock();
            try {
                if ((object = singletons.get(clazz)) == null) {
                    object = instantiate(descriptor);
                    singletons.put(clazz, object);
                    try {
                        construct(object);
                    } catch (RuntimeException | Error e) {
                        singletons.remove(clazz);
                        throw e;
                    }
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        return clazz.cast(object);
    }

    private final ThreadLocal<Map<Object, Object>> dejaVu = ThreadLocal.withInitial(HashMap::new);
    private <T> T lookupStateless(Class<T> clazz, Descriptor descriptor) {
        Map<Object, Object> dejaVu = this.dejaVu.get();
        Object object = dejaVu.get(descriptor);
        if (object == null) {
            object = instantiate(descriptor);
            dejaVu.put(descriptor, object);
            try {
                construct(object);
            } finally {
                dejaVu.remove(descriptor);
            }
        }
        return clazz.cast(object);
    }

    @Override
    public void startRequest() {}

    @Override
    public void finishRequest() {
        requestScoped.get().clear();
    }

    private final ThreadLocal<Map<Object, Object>> requestScoped = ThreadLocal.withInitial(HashMap::new);
    private <T> T lookupRequestScoped(Class<T> clazz, Descriptor descriptor) {
        Map<Object, Object> requestScoped = this.requestScoped.get();
        Object object = requestScoped.get(descriptor);
        if (object == null) {
            object = instantiate(descriptor);
            requestScoped.put(descriptor, object);
            construct(object);
        }
        return clazz.cast(object);
    }

    private void construct(Object object) {
        if (object instanceof $CdiObject) {
            (($CdiObject) object).$construct();
        }
    }

    private static final ConcurrentMap<Class<?>, Constructor<?>> constructors = new ConcurrentHashMap<>();
    private Object instantiate(Descriptor descriptor) {
        Class<?> proxy = proxy(descriptor.c);
        Constructor<?> ctr = constructors.computeIfAbsent(proxy, p -> silent(() -> proxy.getConstructors()[0]));
        return ctr.getParameterCount() == 1
                ? newInstance(ctr, this)
                : newInstance(ctr, this, descriptor.p);
    }

    private static final Map<Class, Class<?>> defaults = new IdentityHashMap<>();
    private <T> Class<? extends T> proxy(Class<T> clazz) {
        if (clazz.isAnnotationPresent(Scoped.class)) {
            return InheritorGenerator.getSubclass(clazz);
        } else if (clazz.isInterface()) {
            return defaults.getOrDefault(clazz, clazz).asSubclass(clazz);
        } else {
            return clazz;
        }
    }

    private Scope scope(Class<?> clazz) {
        Scoped scoped = clazz.getAnnotation(Scoped.class);
        return (scoped == null) ? null : scoped.value();
    }

    private static <T> void bind(Class<T> sup, Class<? extends T> sub) {
        defaults.put(sup, sub);
    }

    static {
        bind(Context.class, ContextImpl.class);
        bind(List.class, ArrayList.class);
        bind(Map.class, HashMap.class);
        bind(SortedMap.class, TreeMap.class);
        bind(ConcurrentMap.class, ConcurrentHashMap.class);
    }

    {
        singletons.put(Context.class, this);
    }
}
