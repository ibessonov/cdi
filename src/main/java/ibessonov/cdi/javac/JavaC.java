package ibessonov.cdi.javac;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.lang.reflect.Method;
import java.util.List;

import static ibessonov.cdi.reflection.ReflectionUtil.getMethod;
import static ibessonov.cdi.reflection.ReflectionUtil.invoke;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

/**
 * Runtime in-memory java compiler that uses {@link javax.tools.JavaCompiler} as a base
 * @author ibessonov
 */
public class JavaC {

    @SuppressWarnings("SpellCheckingInspection")
    private static final List<String> PARAMS = asList("-g:none", "-proc:none", "-Xlint:none");

    private static final ClassLoader SYSTEM_CLASS_LOADER = getSystemClassLoader();

    private static final Method DEFINE_CLASS_METHOD = getMethod(ClassLoader.class, "defineClass",
                                                                String.class, byte[].class, int.class, int.class);

    /**
     * Compiles Java class from source code
     * @param name full name of class to return
     * @param content string representation of source code to be compiled
     * @return Class object that represents compiled class.
     *         System {@link java.lang.ClassLoader} will be used to load compiled class
     */
    public static Class<?> compile(String name, String content) {
        return compile(SYSTEM_CLASS_LOADER, name, content);
    }

    /**
     * Compiles Java class from source code
     * @param classLoader ClassLoader object that will be used to load compiled class
     * @param name full name of class to return
     * @param content string representation of source code to be compiled
     * @return Class object that represents compiled class
     */
    public static Class<?> compile(ClassLoader classLoader, String name, String content) {
        SourceCode sourceCode = new SourceCode(name, content);
        CompiledCode compiledCode = new CompiledCode(name);

        JavaCompiler javaC = ToolProvider.getSystemJavaCompiler();
        CdiFileManager cdiFileManager = new CdiFileManager(javaC.getStandardFileManager(null, null, null), compiledCode);
        javaC.getTask(null, cdiFileManager, null, PARAMS, null, singleton(sourceCode)).call();

        return defineClass(classLoader, name, compiledCode.toByteCode());
    }

    private static Class<?> defineClass(ClassLoader cl, String name, byte[] bytes) {
        return (Class<?>) invoke(DEFINE_CLASS_METHOD, cl, name, bytes, 0, bytes.length);
    }

    private JavaC() {}
}
