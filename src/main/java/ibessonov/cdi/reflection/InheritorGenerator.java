package ibessonov.cdi.reflection;

import ibessonov.cdi.annotations.Generic;
import ibessonov.cdi.annotations.Scoped;
import ibessonov.cdi.javac.JavaC;

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.reflect.Modifier.isFinal;

/**
 * @author ibessonov
 */
public final class InheritorGenerator {

    public static final String PROXY_SUFFIX = "$CdiProxy";

    private static final ConcurrentMap<Class, Class> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> getSubclass(Class<T> c) {
        if (!c.isAnnotationPresent(Scoped.class)) {
            throw new RuntimeException();
        }
        if (isFinal(c.getModifiers())) {
            throw new RuntimeException();
        }

        return (Class<? extends T>) cache.computeIfAbsent(c, clazz -> {
            String packageName = clazz.getPackage().getName();
            String superClassName = clazz.getName();
            String className = superClassName.substring(packageName.length() + 1) + PROXY_SUFFIX;

            if (clazz.isAnnotationPresent(Generic.class)) {
                String content = MessageFormat.format(
                        "package {0};\n" +
                        "@SuppressWarnings(\"unchecked\")\n" +
                        "public class {1}<$T0> extends {2}<$T0> implements ibessonov.cdi.internal.$Generic<$T0> '{'\n" +
                        "    private final Class $p0;\n" +
                        "    public {1}(Class $p0) '{' this.$p0 = $p0; '}'\n" +
                        "    @Override public Class $typeParameter() '{' return this.$p0; '}'\n" +
                        "'}'",
                        packageName, className, clazz.getCanonicalName());
                return JavaC.compile(superClassName + PROXY_SUFFIX, content);
            } else {
                String content = MessageFormat.format(
                        "package {0};\n" +
                        "public class {1} extends {2} implements ibessonov.cdi.internal.$Constructable '{'\n" +
                        "    @Override public void $construct(ibessonov.cdi.Context $context) '{'\n" +
                        "    '}'\n" +
                        "'}'",
                        packageName, className, clazz.getCanonicalName());
                return JavaC.compile(superClassName + PROXY_SUFFIX, content);
            }
        });
    }

    private InheritorGenerator() {
    }
}
