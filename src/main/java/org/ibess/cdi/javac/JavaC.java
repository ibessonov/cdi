package org.ibess.cdi.javac;

import java.lang.reflect.Method;

import static java.lang.ClassLoader.getSystemClassLoader;
import static org.ibess.cdi.javac.Reflection.getMethod;
import static org.ibess.cdi.javac.Reflection.invoke;

/**
 * @author ibessonov
 */
public class JavaC {

    private static final ClassLoader SYSTEM_CLASS_LOADER = getSystemClassLoader();

    private static final Method DEFINE_CLASS_METHOD = getMethod(ClassLoader.class, "defineClass",
                                                                String.class, byte[].class, int.class, int.class);

    public static Class<?> defineClass(String name, byte[] bytes) {
        return (Class<?>) invoke(DEFINE_CLASS_METHOD, SYSTEM_CLASS_LOADER, name, bytes, 0, bytes.length);
    }

    private JavaC() {
    }
}
