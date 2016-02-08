package org.ibess.cdi.runtime;

import org.ibess.cdi.exceptions.ImpossibleError;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.security.AccessController.doPrivileged;

/**
 * @author ibessonov
 */
final class CdiClassLoader {

    private static final ClassLoader SYSTEM_CLASS_LOADER = getSystemClassLoader0();

    private static final Method DEFINE_CLASS_METHOD = getMethod(ClassLoader.class, "defineClass",
                                                                String.class, byte[].class, int.class, int.class);

    /**
     * Defines Java class by its bytecode. System ClassLoader is used to load the class
     * @param bytes array that contains bytecode for resulting class
     * @return Class object representing defined class
     * @see ClassLoader#getSystemClassLoader()
     * @see ClassLoader#defineClass(String, byte[], int, int)
     */
    public static Class<?> defineClass(byte[] bytes) {
        return (Class<?>) invoke(DEFINE_CLASS_METHOD, SYSTEM_CLASS_LOADER, null, bytes, 0, bytes.length);
    }

    private static ClassLoader getSystemClassLoader0() {
        if (System.getSecurityManager() == null) {
            return getSystemClassLoader();
        } else {
            return doPrivileged((PrivilegedAction<ClassLoader>) ClassLoader::getSystemClassLoader);
        }
    }

    private static Method getMethod(Class<?> clazz, String name, Class<?>... parameters) {
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

    private static Object invoke(Method method, Object object, Object... parameters) {
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
