package ibessonov.cdi.reflection;

import ibessonov.cdi.annotations.Constructor;
import ibessonov.cdi.annotations.Generic;
import ibessonov.cdi.annotations.Inject;
import ibessonov.cdi.annotations.Scoped;
import ibessonov.cdi.javac.JavaC;

import java.lang.reflect.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static ibessonov.cdi.enums.Scope.SINGLETON;
import static java.lang.reflect.Modifier.isFinal;

/**
 * @author ibessonov
 */
public final class InheritorGenerator {

    public static final String PROXY_SUFFIX = "$CdiProxy";

    private static final ConcurrentMap<Class, Class<?>> cache = new ConcurrentHashMap<>();

    public static <T> Class<? extends T> getSubclass(Class<T> c) {
        return cache.computeIfAbsent(c, clazz -> {
            if (isFinal(c.getModifiers())) {
                throw new RuntimeException();
            }
            Scoped scoped = c.getAnnotation(Scoped.class);
            if (scoped == null) {
                throw new RuntimeException();
            }
            if (scoped.value() == SINGLETON && c.isAnnotationPresent(Generic.class)) {
                throw new RuntimeException();
            }

            String packageName = clazz.getPackage().getName();
            String superClassName = clazz.getName();
            String className = superClassName.substring(packageName.length() + 1) + PROXY_SUFFIX;

            StringBuilder content = new StringBuilder();
            content.append("package ").append(packageName).append(";\n");
            content.append("@SuppressWarnings(\"unchecked\") public final class ").append(className);
            appendGenericParams(clazz, content);
            content.append(" extends ").append(clazz.getCanonicalName());
            appendGenericParams(clazz, content); //FIXME it's completely wrong
            content.append(" implements ibessonov.cdi.internal.$Constructable");
            appendInterfaces(clazz, content);
            content.append(" {\n");
            appendGeneratedFieldsAndMethods(className, clazz, content);
            content.append("  @Override public void $construct(ibessonov.cdi.Context $context) {\n");
            appendGeneratedFieldsConstruction(clazz, content);
            Field[] fields = Stream.of(clazz.getDeclaredFields()).filter(InheritorGenerator::injectable).toArray(Field[]::new);
            for (Field field : fields) {
                appendFieldConstruction(field, content);
            }
            appendConstructorInvocation(clazz, content);
            content.append("  }\n");
            content.append("}\n");

            return JavaC.compile(superClassName + PROXY_SUFFIX, content.toString());
        }).asSubclass(c);
    }

    private static void appendGenericParams(Class clazz, StringBuilder content) {
        if (clazz.isAnnotationPresent(Generic.class)) {
            content.append("<$T0>");
        }
    }

    private static void appendInterfaces(Class clazz, StringBuilder content) {
        if (clazz.isAnnotationPresent(Generic.class)) {
            content.append(", ibessonov.cdi.internal.$Generic<$T0>");
        }
    }

    private static void appendGeneratedFieldsAndMethods(String className, Class clazz, StringBuilder content) {
        if (clazz.isAnnotationPresent(Generic.class)) {
            content.append("  private final Class $class;\n");
            content.append("  public ").append(className).append("(Class $class) {\n    this.$class = $class;\n  }\n");
            content.append("  @Override public Class $typeParameter() {\n    return this.$class;\n  }\n");
        }
    }

    private static void appendGeneratedFieldsConstruction(Class clazz, StringBuilder content) {

    }

    private static void appendFieldConstruction(Field field, StringBuilder content) {
        content.append("    ").append(field.getName()).append(" = ");
        appendLookup(field.getGenericType(), content);
        content.append(");\n");
    }

    private static void appendConstructorInvocation(Class clazz, StringBuilder content) {
        Method[] methods = Stream.of(clazz.getDeclaredMethods()).filter(
                m -> m.isAnnotationPresent(Constructor.class)
        ).toArray(Method[]::new);
        if (methods.length < 1) return;
        if (methods.length > 1) throw new RuntimeException();
        Method constructor = methods[0];

        validateConstructor(constructor);
        int parameterCount = constructor.getParameterCount();

        Type[] parameterTypes = constructor.getGenericParameterTypes();
        content.append("    this.").append(constructor.getName()).append("(");
        for (int i = 0; i < parameterCount; i++) {
            if (i + 1 != parameterCount) content.append(", ");
            appendLookup(parameterTypes[i], content);
        }
        content.append(");\n");
    }

    private static void appendLookup(Type type, StringBuilder content) {
        content.append("$context.lookup(");

        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            String canonicalName = ((Class) pType.getRawType()).getCanonicalName();
            Type[] actualTypeArguments = pType.getActualTypeArguments();
            if (actualTypeArguments.length == 1) {
                Type actualTypeArgument = actualTypeArguments[0];
                if (actualTypeArgument instanceof ParameterizedType) {
                    Class parameter = (Class) ((ParameterizedType) actualTypeArgument).getRawType();
                    //TODO fix
                    System.out.println("Wow!");
                    content.append(canonicalName).append(".class, ").append(parameter.getCanonicalName()).append(".class");
                } else if (actualTypeArgument instanceof TypeVariable) {
                    content.append(canonicalName).append(".class, $class");
                } else {
                    Class parameter = (Class) actualTypeArgument;
                    content.append(canonicalName).append(".class, ").append(parameter.getCanonicalName()).append(".class");
                }
            } else {
                content.append(canonicalName).append(".class");
            }
        } else {
            Class cType = (Class) type;
            content.append(cType.getCanonicalName()).append(".class");
        }
    }

    private static boolean injectable(Field field) {
        return field.isAnnotationPresent(Inject.class);
    }

    private static void validateConstructor(Method constructor) {
        Class<?>[] exceptions = constructor.getExceptionTypes();
        for (Class<?> exceptionClass : exceptions) {
            if (!RuntimeException.class.isAssignableFrom(exceptionClass)
                    && !Error.class.isAssignableFrom(exceptionClass)) {
                throw new RuntimeException();
            }
        }
    }

    private InheritorGenerator() {
    }
}
