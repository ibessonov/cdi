package org.ibess.cdi.util;

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

    public static boolean isChecked(Class<?> exceptionClass) {
        return RuntimeException.class.isAssignableFrom(exceptionClass)
                       || Error.class.isAssignableFrom(exceptionClass);
    }

    private static RuntimeException toRuntimeException(Exception e) {
        return (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
}
