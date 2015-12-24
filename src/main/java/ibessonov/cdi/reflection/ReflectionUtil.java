package ibessonov.cdi.reflection;

import ibessonov.cdi.exceptions.CdiException;
import ibessonov.cdi.exceptions.ImpossibleError;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static ibessonov.cdi.enums.CdiErrorType.ILLEGAL_ACCESS;
import static ibessonov.cdi.util.Cdi.silent;

/**
 * @author ibessonov
 */
public class ReflectionUtil {

    public static <T> T newInstance(Class<? extends T> clazz) {
        return silent(clazz::newInstance);
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
