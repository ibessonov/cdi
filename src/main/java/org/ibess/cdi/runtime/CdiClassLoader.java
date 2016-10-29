package org.ibess.cdi.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.security.AccessController.doPrivileged;

/**
 * @author ibessonov
 */
final class CdiClassLoader extends ClassLoader {

    private static final ClassLoader SYSTEM_CLASS_LOADER = getSystemClassLoader0();
    private static final MethodHandle DEFINE_CLASS_METHOD_HANDLE = getDefineClassMethodHandle();
    private static final String NAME = null;

    /**
     * Defines Java class by its bytecode. System ClassLoader is used to load the class
     * @param bytes array that contains bytecode for resulting class
     * @param <T> Generic parameter to avoid compiler warnings
     * @return Class object representing defined class
     * @see ClassLoader#getSystemClassLoader()
     * @see ClassLoader#defineClass(String, byte[], int, int)
     */
    public static <T> Class<T> defineClass(byte[] bytes) {
        return defineClass0(SYSTEM_CLASS_LOADER, bytes);
    }

    public static ClassLoader getClassLoader(Class<?> clazz) {
        if (System.getSecurityManager() == null) {
            return clazz.getClassLoader();
        } else {
            return doPrivileged((PrivilegedAction<ClassLoader>) clazz::getClassLoader);
        }
    }

    private static ClassLoader getSystemClassLoader0() {
        if (System.getSecurityManager() == null) {
            return getSystemClassLoader();
        } else {
            return doPrivileged((PrivilegedAction<ClassLoader>) ClassLoader::getSystemClassLoader);
        }
    }

    private static MethodHandle getDefineClassMethodHandle() {
        try {
            Method method = ClassLoader.class.getDeclaredMethod(
                "defineClass", String.class, byte[].class, int.class, int.class
            );
            if (System.getSecurityManager() == null) {
                method.setAccessible(true);
            } else {
                doPrivileged((PrivilegedAction<Void>) () -> {
                    method.setAccessible(true); return null;
                });
            }
            return publicLookup().unreflect(method);
        } catch (Throwable throwable) {
            return throw0(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> defineClass0(ClassLoader classLoader, byte[] bytes) {
        try {
            return (Class) DEFINE_CLASS_METHOD_HANDLE.invokeExact(classLoader, NAME, bytes, 0, bytes.length);
        } catch (Throwable throwable) {
            return throw0(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable, Nothing> Nothing throw0(Throwable throwable) throws T {
        throw (T) (throwable == null ? new IllegalArgumentException("Attempt to throw null") : throwable);
    }
}
