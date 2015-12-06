package ibessonov.cdi;

import ibessonov.cdi.annotations.Scoped;
import ibessonov.cdi.enums.Scope;
import ibessonov.cdi.exceptions.ImpossibleException;
import ibessonov.cdi.internal.$Constructable;
import ibessonov.cdi.reflection.InheritorGenerator;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ibessonov.cdi.enums.Scope.SINGLETON;
import static ibessonov.cdi.reflection.ReflectionUtil.newInstance;
import static ibessonov.cdi.util.Cdi.silent;

/**
 * @author ibessonov
 */
@Scoped(SINGLETON)
public final class Context {

    public <T, V> T lookup(Class<T> clazz, Class<V> param) {
        return lookup0(clazz, new GenericClassDescriptor(clazz, param));
    }

    public <T> T lookup(Class<T> clazz) {
        return lookup0(clazz, clazz);
    }

    private <T> T lookup0(Class<T> clazz, Object descriptor) {
        if (clazz.isPrimitive()) throw new RuntimeException();

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
                throw new ImpossibleException();
        }
    }

    private final Map<Class, Object> singletons = new IdentityHashMap<>();
    private final ReadWriteLock      rwLock     = new ReentrantReadWriteLock();
    private <T> T lookupSingleton(Class<T> clazz, Object descriptor) {
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

    private final ThreadLocal<Map<Object, Object>> dejaVu = ThreadLocal.withInitial(WeakHashMap::new);
    private <T> T lookupStateless(Class<T> clazz, Object descriptor) {
        Map<Object, Object> dejaVu = this.dejaVu.get();
        Object object = dejaVu.get(descriptor);
        if (object == null) {
            object = instantiate(descriptor);
            dejaVu.put(descriptor, object);
            construct(object);
        }
        return clazz.cast(object);
    }

    public void startRequest() {}

    public void finishRequest() {
        requestScoped.get().clear();
    }

    private final ThreadLocal<Map<Object, Object>> requestScoped = ThreadLocal.withInitial(HashMap::new);
    private <T> T lookupRequestScoped(Class<T> clazz, Object descriptor) {
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
        if (object instanceof $Constructable) {
            (($Constructable) object).$construct(this);
        }
    }

    private <T> T instantiate(Class<T> clazz) {
        return newInstance(proxy(clazz));
    }

    private Object instantiate(Object descriptor) {
        if (descriptor instanceof Class) {
            return instantiate((Class<?>) descriptor);
        } else {
            GenericClassDescriptor gcd = (GenericClassDescriptor) descriptor;
            return silent(() -> {
                Class<?> proxy = proxy(gcd.clazz);
                Constructor<?> ctr = proxy.getConstructor(Class.class);
                return ctr.newInstance(gcd.param);
            });
        }
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

    private static final class GenericClassDescriptor {
        public final Class<?> clazz;
        public final Class<?> param;

        public GenericClassDescriptor(Class<?> clazz, Class<?> param) {
            this.clazz = clazz;
            this.param = param;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof GenericClassDescriptor)) return false;

            GenericClassDescriptor that = (GenericClassDescriptor) obj;
            return this.clazz == that.clazz && this.param == that.param;
        }

        @Override
        public int hashCode() {
            return clazz.hashCode() ^ param.hashCode();
        }
    }

    private static <T> void bind(Class<T> sup, Class<? extends T> sub) {
        defaults.put(sup, sub);
    }

    static {
        bind(List.class, ArrayList.class);
        bind(Map.class, HashMap.class);
        bind(SortedMap.class, TreeMap.class);
        bind(ConcurrentMap.class, ConcurrentHashMap.class);
    }

    {
        singletons.put(Context.class, this);
    }
}
