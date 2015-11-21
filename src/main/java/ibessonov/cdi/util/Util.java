package ibessonov.cdi.util;

import java.security.PrivilegedAction;
import java.util.concurrent.Callable;

import static java.security.AccessController.doPrivileged;

/**
 * @author ibessonov
 */
public class Util {

    public static <T> T silent(Callable<T> c) {
        try {
            return c.call();
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    public static void silent(RunnableEx r) {
        try {
            r.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwQuite0(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
