package ibessonov.cdi;

import ibessonov.cdi.annotations.Scoped;
import ibessonov.cdi.enums.CdiErrorType;
import ibessonov.cdi.enums.Scope;
import ibessonov.cdi.exceptions.CdiException;
import ibessonov.cdi.exceptions.ImpossibleError;
import ibessonov.cdi.internal.$CdiObject;
import ibessonov.cdi.internal.$Context;
import ibessonov.cdi.reflection.InheritorGenerator;
import ibessonov.cdi.reflection.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ibessonov.cdi.enums.CdiErrorType.GENERIC_PARAMETERS_COUNT_MISMATCH;
import static ibessonov.cdi.enums.CdiErrorType.PRIMITIVE_TYPE_LOOKUP;
import static ibessonov.cdi.reflection.ReflectionUtil.newInstance;
import static ibessonov.cdi.util.Cdi.silent;

/**
 * @author ibessonov
 */
final class ContextImpl implements $Context {

    @Override
    public <T> T lookup(Class<T> clazz, Class<?> ... params) {
        if (clazz.getTypeParameters().length != params.length) {
            throw new CdiException(GENERIC_PARAMETERS_COUNT_MISMATCH,
                    clazz.getCanonicalName(), clazz.getTypeParameters().length, params.length);
        }
        return lookup0(clazz, new Descriptor(clazz, params));
    }

    private <T> T lookup0(Class<T> clazz, Descriptor descriptor) {
        if (clazz.isPrimitive()) throw new CdiException(PRIMITIVE_TYPE_LOOKUP, clazz.getCanonicalName());

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

    private final ThreadLocal<Map<Object, Object>> dejaVu = ThreadLocal.withInitial(WeakHashMap::new);
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

    private Object instantiate(Descriptor descriptor) {
        Constructor<?> ctr = silent(() -> {
            Class<?> proxy = proxy(descriptor.clazz);
            //problem
            return proxy.getConstructor($Context.class, Class[].class);
        });
        return ReflectionUtil.newInstance(ctr, this, descriptor.params);
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

    private static final class Descriptor {
        public final Class<?> clazz;
        public final Class<?> params[];
        private int  hash = 0;

        public Descriptor(Class<?> clazz, Class<?> ... params) {
            this.clazz = clazz;
            this.params = params;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Descriptor)) return false;

            Descriptor that = (Descriptor) obj;
            return this.clazz == that.clazz && Arrays.equals(this.params, that.params);
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                hash = h = clazz.hashCode() ^ Arrays.hashCode(params);
            }
            return h;
        }
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
