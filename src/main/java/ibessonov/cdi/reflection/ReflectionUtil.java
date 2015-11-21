package ibessonov.cdi.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static ibessonov.cdi.util.Util.privileged;
import static ibessonov.cdi.util.Util.silent;
import static ibessonov.cdi.util.Util.throwQuite;

/**
 * @author ibessonov
 */
public class ReflectionUtil {

    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameters) {
        try {
            Method method = clazz.getDeclaredMethod(name, parameters);
            privileged(() -> method.setAccessible(true));
            return method;
        } catch (Throwable t) {
            throwQuite(t);
            return null;
        }
    }

    public static <T> T newInstance(Class<? extends T> clazz) {
        return silent(clazz::newInstance);
    }

    public static Object invoke(Method method, Object object, Object... parameters) {
        try {
            return method.invoke(object, parameters);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else {
                throw new RuntimeException(targetException);
            }
        }
    }

    private ReflectionUtil() {
    }
}
