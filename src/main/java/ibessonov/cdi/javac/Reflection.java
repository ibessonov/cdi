package ibessonov.cdi.javac;

import ibessonov.cdi.exceptions.ImpossibleError;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

import static java.security.AccessController.doPrivileged;

/**
 * @author ibessonov
 */
class Reflection {

    static Method getMethod(Class<?> clazz, String name, Class<?>... parameters) {
        try {
            Method method = clazz.getDeclaredMethod(name, parameters);
            if (System.getSecurityManager() == null) {
                method.setAccessible(true);
            } else {
                doPrivileged((PrivilegedAction<Void>) () -> {
                    method.setAccessible(true); return null;
                });
            }
            return method;
        } catch (Throwable t) {
            throwUnchecked(t);
            throw new ImpossibleError(t);
        }
    }

    static Object invoke(Method method, Object object, Object... parameters) {
        try {
            return method.invoke(object, parameters);
        } catch (InvocationTargetException ite) {
            throwUnchecked(ite.getTargetException());
            throw new ImpossibleError(ite);
        } catch (Throwable t) {
            throwUnchecked(t);
            throw new ImpossibleError(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
