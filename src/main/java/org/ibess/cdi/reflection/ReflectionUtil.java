package org.ibess.cdi.reflection;

import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.exceptions.ImpossibleError;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.ibess.cdi.enums.CdiErrorType.ILLEGAL_ACCESS;

/**
 * @author ibessonov
 */
public class ReflectionUtil {

    public static <T> T newInstance(Class<? extends T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException ie) {
            throw new ImpossibleError(ie);
        } catch (IllegalAccessException iae) {
            throw new CdiException(iae, ILLEGAL_ACCESS);
        }
    }

    public static <T> T newInstance(Constructor<T> ctr, Object... args) {
        try {
            return ctr.newInstance(args);
        } catch (InstantiationException ie) {
            throw new ImpossibleError(ie);
        } catch (IllegalAccessException iae) {
            throw new CdiException(iae, ILLEGAL_ACCESS);
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
