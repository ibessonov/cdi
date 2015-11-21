package ibessonov.cdi.javac;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.lang.reflect.Method;

import static ibessonov.cdi.reflection.ReflectionUtil.getMethod;
import static ibessonov.cdi.reflection.ReflectionUtil.invoke;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Collections.singleton;

/**
 * @author ibessonov
 */
public class JavaC {

    private static final ClassLoader SYSTEM_CLASS_LOADER = getSystemClassLoader();
    private static final Method DEFINE_CLASS_METHOD = getMethod(ClassLoader.class, "defineClass",
                                                                String.class, byte[].class, int.class, int.class);

    public static Class<?> compile(String name, String content) {
        Object o = void.class;
        return compile(SYSTEM_CLASS_LOADER, name, content);
    }

    public static Class<?> compile(ClassLoader classLoader, String name, String content) {
        SourceCode sourceCode = new SourceCode(name, content);
        CompiledCode compiledCode = new CompiledCode(name);

        JavaCompiler javaC = ToolProvider.getSystemJavaCompiler();
        CdiFileManager cdiFileManager = new CdiFileManager(javaC.getStandardFileManager(null, null, null), compiledCode);
        javaC.getTask(null, cdiFileManager, null, null, null, singleton(sourceCode)).call();

        return defineClass(classLoader, name, compiledCode.toByteCode());
    }

    private static Class<?> defineClass(ClassLoader cl, String name, byte[] bytes) {
        return (Class<?>) invoke(DEFINE_CLASS_METHOD, cl, name, bytes, 0, bytes.length);
    }

    private JavaC() {
    }
}
