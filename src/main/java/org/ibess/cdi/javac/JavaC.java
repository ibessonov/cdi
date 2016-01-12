package org.ibess.cdi.javac;

import org.ibess.cdi.exceptions.ImpossibleError;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Boolean.getBoolean;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;
import static java.util.logging.Level.INFO;
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
     * Compiles Java class from source code
     * @param name    full name of class to return
     * @param content string representation of source code to be compiled
     * @return Class object that represents compiled class.
     *         System {@link java.lang.ClassLoader} will be used to load compiled class
     * @deprecated
     */
    @Deprecated
    public static Class<?> compile(String name, String content) {
        compile(Collections.singletonMap(name, content));
        try {
            return SYSTEM_CLASS_LOADER.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new ImpossibleError(e);
        }
    }

    /**
     * Compiles Java class from source code
     * @param classLoader ClassLoader object that will be used to load compiled class
     * @param name        full name of class to return
     * @param content     string representation of source code to be compiled
     * @return Class object that represents compiled class
     * @deprecated
     */
    @Deprecated
    public static Class<?> compile(ClassLoader classLoader, String name, String content) {
        if (getBoolean("org.ibess.cdi.javac.log.classes")) {
            _log.log(INFO, "Compiling " + name + " with " + classLoader + ":\n" + content);
        }
        compile(classLoader, Collections.singletonMap(name, content));
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new ImpossibleError(e);
        }
    }

    public static void compile(Map<String, String> contents) {
        compile(SYSTEM_CLASS_LOADER, contents);
    }

    public static void compile(ClassLoader classLoader, Map<String, String> contents) {
        List<JavaFileObject> sources = new ArrayList<>(contents.size());
        for (Map.Entry<String, String> entry : contents.entrySet()) {
            sources.add(new InputJavaFileObject(entry.getKey(), entry.getValue()));
        }

        JavaCompiler javaC = getSystemJavaCompiler();
        CdiFileManager fileManager = new CdiFileManager(javaC.getStandardFileManager(null, null, null));
        javaC.getTask(null, fileManager, null, PARAMS, null, sources).call();

        for (Map.Entry<String, OutputJavaFileObject> entry : fileManager.code.entrySet()) {
            defineClass(classLoader, entry.getKey(), entry.getValue().toByteCode());
        }
    }

    private static Class<?> defineClass(ClassLoader cl, String name, byte[] bytes) {
        return (Class<?>) invoke(DEFINE_CLASS_METHOD, cl, name, bytes, 0, bytes.length);
    }

    private JavaC() {
    }
}
