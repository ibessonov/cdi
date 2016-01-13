package org.ibess.cdi.javac;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;
import static java.util.logging.Logger.getLogger;
import static javax.tools.ToolProvider.getSystemJavaCompiler;
import static org.ibess.cdi.javac.Reflection.getMethod;
import static org.ibess.cdi.javac.Reflection.invoke;

/**
 * Runtime in-memory java compiler that uses {@link javax.tools.JavaCompiler} as a base
 * @author ibessonov
 */
public class JavaC {

    private static final Logger _log = getLogger(JavaC.class.getName());

    private static final List<String> PARAMS = asList("-g:none", "-proc:none", "-Xlint:none");

    private static final ClassLoader SYSTEM_CLASS_LOADER = getSystemClassLoader();

    private static final Method DEFINE_CLASS_METHOD = getMethod(ClassLoader.class, "defineClass",
                                                                String.class, byte[].class, int.class, int.class);

    /**
     * Compiles Java classes from source code
     * @param contents class names and code to be compiled
     * @param resultClassName name of class to be returned
     * @return Class with name {@code resultClassName}<br>
     *         {@code null} if it wasn't compiled in this invocation<br>
     *         System {@link java.lang.ClassLoader} will be used to define compiled class
     */
    public static Class<?> compile(Map<String, String> contents, String resultClassName) {
        List<JavaFileObject> sources = new ArrayList<>(contents.size());
        for (Map.Entry<String, String> entry : contents.entrySet()) {
            sources.add(new InputJavaFileObject(entry.getKey(), entry.getValue()));
        }

        JavaCompiler javaC = getSystemJavaCompiler();
        CdiFileManager fileManager = new CdiFileManager(javaC.getStandardFileManager(null, null, null));
        javaC.getTask(null, fileManager, null, PARAMS, null, sources).call();

        Class<?> result = null;
        for (Map.Entry<String, OutputJavaFileObject> entry : fileManager.code.entrySet()) {
            Class<?> clazz = defineClass(entry.getKey(), entry.getValue().toByteCode());
            if (resultClassName != null && entry.getKey().equals(resultClassName)) {
                result = clazz;
            }
        }
        return result;
    }

    public static Class<?> defineClass(String name, byte[] bytes) {
        return (Class<?>) invoke(DEFINE_CLASS_METHOD, SYSTEM_CLASS_LOADER, name, bytes, 0, bytes.length);
    }

    private JavaC() {
    }
}
