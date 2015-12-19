package ibessonov.cdi.reflection;

import ibessonov.cdi.exceptions.CdiException;
import ibessonov.cdi.exceptions.ImpossibleError;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static ibessonov.cdi.enums.CdiErrorType.ILLEGAL_ACCESS;
import static ibessonov.cdi.util.Cdi.*;

/**
 * @author ibessonov
 */
public class ReflectionUtil {

    public static Field getPublicField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (Throwable t) {
            throwUnchecked(t);
            throw new ImpossibleError();
        }
    }

    public static Field getField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            privileged(() -> field.setAccessible(true));
            return field;
        } catch (Throwable t) {
            throwUnchecked(t);
            throw new ImpossibleError();
        }
    }

    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameters) {
        try {
            Method method = clazz.getDeclaredMethod(name, parameters);
            privileged(() -> method.setAccessible(true));
            return method;
        } catch (Throwable t) {
            throwUnchecked(t);
            throw new ImpossibleError();
        }
    }

    public static <T> T newInstance(Class<? extends T> clazz) {
        return silent(clazz::newInstance);
    }

    public static <T> T newInstance(Constructor<T> ctr, Object... args) {
        try {
            return ctr.newInstance(args);
        } catch (InstantiationException ie) {
            throw new ImpossibleError(ie);
        } catch (IllegalAccessException iae) {
            throw new CdiException(ILLEGAL_ACCESS);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getTargetException();
            throwIfRuntime(cause);
            throw new ImpossibleError(cause);
        }
    }

    public static Object invoke(Method method, Object object, Object... parameters) {
        try {
            return method.invoke(object, parameters);
        } catch (IllegalAccessException iae) {
            throw new CdiException(ILLEGAL_ACCESS);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getTargetException();
            throwIfRuntime(cause);
            throw new ImpossibleError(cause);
        }
    }

    private static void throwIfRuntime(Throwable t) {
        if (t instanceof RuntimeException) throw (RuntimeException) t;
        if (t instanceof Error) throw (Error) t;
    }

    private ReflectionUtil() {
    }
}
