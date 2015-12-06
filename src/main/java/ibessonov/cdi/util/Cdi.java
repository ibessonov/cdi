package ibessonov.cdi.util;

import ibessonov.cdi.internal.$Generic;

import java.security.PrivilegedAction;
import java.util.concurrent.Callable;

import static java.security.AccessController.doPrivileged;

/**
 * @author ibessonov
 */
public final class Cdi {

    public static <T> T silent(Callable<T> c) {
        try {
            return c.call();
        } catch (Exception e) {
            throw toRuntimeException(e);
        }
    }

    public static void silent(RunnableEx r) {
        try {
            r.run();
        } catch (Exception e) {
            throw toRuntimeException(e);
        }
    }

    public static void privileged(Runnable r) {
        if (System.getSecurityManager() == null) {
            r.run();
        } else {
            doPrivileged((PrivilegedAction<Void>) () -> {
                r.run(); return null;
            });
        }
    }

    public static void throwQuite(Throwable throwable) {
        throwQuite0(throwable);
    }

    public static Class<?> getTypeParameter(Object object) {
        return (($Generic) object).$typeParameter();
    }

    private static RuntimeException toRuntimeException(Exception e) {
        return (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwQuite0(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
