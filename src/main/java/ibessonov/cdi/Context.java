package ibessonov.cdi;

import ibessonov.cdi.annotations.Inject;
import ibessonov.cdi.annotations.Scoped;
import ibessonov.cdi.internal.$Constructable;
import ibessonov.cdi.internal.$Generic;
import ibessonov.cdi.internal.$WithContext;
import ibessonov.cdi.reflection.InheritorGenerator;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static ibessonov.cdi.reflection.InheritorGenerator.PROXY_SUFFIX;
import static ibessonov.cdi.reflection.ReflectionUtil.invoke;
import static ibessonov.cdi.reflection.ReflectionUtil.newInstance;
import static ibessonov.cdi.util.Util.privileged;
import static ibessonov.cdi.util.Util.silent;
import static java.lang.reflect.AccessibleObject.setAccessible;

/**
 * @author ibessonov
 */
public class Context {

    public static Class<?> getTypeParameter(Object object) {
        return (($Generic) object).$typeParameter();
    }

    public static Context getContext(Object object) {
        return (($WithContext) object).$context();
    }

    private final Map<Class, Object> singletons = new IdentityHashMap<>();
    private final Map<Class, Consumer> injectors = new IdentityHashMap<>();
    private final ThreadLocal<Map> dejaVu = ThreadLocal.withInitial(HashMap::new);

    public <T, V> T lookup(Class<T> clazz, Class<V> parameter) {
        return lookup(clazz, () -> instantiateGeneric(clazz, parameter));
    }

    public <T> T lookup(Class<T> clazz) {
        return lookup(clazz, () -> instantiate(clazz));
    }

    private <T> T lookup(Class<T> clazz, Supplier<T> instantiator) {
        T object = (T) singletons.get(clazz);
        if (object == null) {
            object = instantiator.get();
            singletons.put(clazz, object);
            requestInjection(object);

            invokeConstructor(object);
        }
        return object;
    }

    public void requestInjection(Object object) {
        Class<?> clazz = object.getClass();
//        if (isProxy(clazz)) {
//            construct(object);
//        } else {
            Consumer injector = getInjector(unproxy(clazz));
            injector.accept(object);
            invokeConstructor(object);
//        }
    }

    private void invokeConstructor(Object object) {
        Class clazz = unproxy(object.getClass());
        Method[] methods = Stream.of(clazz.getDeclaredMethods()).filter(
                m -> m.isAnnotationPresent(ibessonov.cdi.annotations.Constructor.class)
        ).toArray(Method[]::new);
        if (methods.length < 1) return;
        if (methods.length > 1) throw new RuntimeException();
        Method method = methods[0];
        if (!method.isAccessible()) privileged(() -> method.setAccessible(true));
        int parameterCount = method.getParameterCount();
        Object[] parameters = new Object[parameterCount];
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterCount; i++) {
            Class c = parameterTypes[i];
            parameters[i] = lookup(c);
        }
        invoke(method, object, parameters);
    }

    private void construct(Object object) {
        if (object instanceof $Constructable) {
            (($Constructable) object).$construct(this);
        }
    }

    private <T> T instantiate(Class<T> clazz) {
        return newInstance(proxy(clazz));
    }

    private <T, V> T instantiateGeneric(Class<T> clazz, Class<V> parameter) {
        return silent(() -> {
            Class<? extends T> proxy = proxy(clazz);
            Constructor<? extends T> ctr = proxy.getConstructor(Class.class);
            if (!ctr.isAccessible()) privileged(() -> ctr.setAccessible(true));
            return ctr.newInstance(parameter);
        });
    }

    private Consumer getInjector(Class clazz) {
        Consumer injector = injectors.get(clazz);
        if (injector == null) {
            Field[] fields = Stream.of(clazz.getDeclaredFields()).filter(Context::injectable).toArray(Field[]::new);
            privileged(() -> setAccessible(fields, true));
            Consumer[] consumers = Stream.of(fields).map(field -> {
                Class<?> type = field.getType();
                Type genericType = field.getGenericType();
                if (genericType != type) {
                    ParameterizedType pType = (ParameterizedType) genericType;
                    Type[] actualTypeArguments = pType.getActualTypeArguments();
                    if (actualTypeArguments.length == 1) {
                        Type actualTypeArgument = actualTypeArguments[0];
                        Class parameter;
                        if (actualTypeArgument instanceof ParameterizedType) {
                            parameter = (Class) ((ParameterizedType) actualTypeArgument).getRawType();
                            System.out.println("Wow!");
                        } else {
                            parameter = (Class) actualTypeArgument;
                        }
                        return (Consumer) (o -> silent(() -> field.set(o, lookup(type, parameter))));
                    }
                }
                return (Consumer) (o -> silent(() -> field.set(o, lookup(type))));
            }).toArray(Consumer[]::new);
            injector = o -> { for (Consumer c : consumers) c.accept(o); };
            injectors.put(clazz, injector);
        }
        return injector;
    }

    private static boolean injectable(Field field) {
        return field.isAnnotationPresent(Inject.class);
    }

//    private static boolean generic(Class clazz) {
//        if (!$Generic.class.isAssignableFrom(clazz)) return false;
//        if (Arrays.asList(clazz.getGenericInterfaces()).contains($Generic.class)) return false;
//        return clazz.getTypeParameters().length == 1;
//    }

    private <T> Class<? extends T> proxy(Class<T> clazz) {
        if (clazz.isAnnotationPresent(Scoped.class)) {
            return InheritorGenerator.getSubclass(clazz);
        } else {
            return clazz;
        }
    }

    private static Class unproxy(Class clazz) {
        return isProxy(clazz) ? clazz.getSuperclass() : clazz;
    }

    private static boolean isProxy(Class clazz) {
        return clazz.getName().endsWith(PROXY_SUFFIX);
    }
}
