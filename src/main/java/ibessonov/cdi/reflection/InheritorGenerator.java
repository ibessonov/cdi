package ibessonov.cdi.reflection;

import ibessonov.cdi.annotations.Constructor;
import ibessonov.cdi.annotations.Provided;
import ibessonov.cdi.annotations.Inject;
import ibessonov.cdi.annotations.Scoped;
import ibessonov.cdi.enums.CdiErrorType;
import ibessonov.cdi.enums.Scope;
import ibessonov.cdi.exceptions.CdiException;
import ibessonov.cdi.exceptions.ImpossibleError;
import ibessonov.cdi.javac.JavaC;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ibessonov.cdi.enums.CdiErrorType.*;
import static ibessonov.cdi.enums.Scope.STATELESS;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;

/**
 * @author ibessonov
 */
public final class InheritorGenerator {

    private static final String PROXY_SUFFIX = "$CdiProxy";

    private static final ConcurrentMap<Class, Class<?>> cache = new ConcurrentHashMap<>();

    public static <T> Class<? extends T> getSubclass(Class<T> clazz) {
        return cache.computeIfAbsent(clazz, c -> {
            if (isFinal(clazz.getModifiers())) {
                throw new CdiException(FINAL_SCOPED_CLASS, clazz.getCanonicalName());
            }
            Scoped scoped = clazz.getAnnotation(Scoped.class);
            if (scoped == null) { // remove this checking in future. This class has to be invisible for users
                throw new ImpossibleError();
            }
            if (scoped.value() != STATELESS && clazz.getTypeParameters().length > 0) {
                throw new CdiException(CdiErrorType.PARAMETERIZED_NON_STATELESS, clazz.getCanonicalName());
            }

            ClassInfo<T> classInfo = new ClassInfo<>(clazz);

            classInfo.content.append("package ").append(classInfo.packageName).append(";\n");
            classInfo.content.append("@SuppressWarnings(\"unchecked\") public final class ").append(classInfo.className);
            appendGenericParameters(classInfo, true);
            classInfo.content.append(" extends ").append(clazz.getCanonicalName());
            appendGenericParameters(classInfo, false);
            classInfo.content.append(" implements ibessonov.cdi.internal.$CdiObject {\n");
            appendGeneratedFieldsAndMethods(classInfo);
            classInfo.content.append("  @Override public final void $construct() {\n");
            Field[] fields = Stream.of(clazz.getDeclaredFields()).filter(InheritorGenerator::injectable).toArray(Field[]::new);
            for (Field field : fields) {
                appendFieldConstruction(classInfo, field);
            }
            appendConstructorInvocation(classInfo);
            classInfo.content.append("  }\n");

            Method[] methods = Stream.of(clazz.getDeclaredMethods()).filter(m -> isAbstract(m.getModifiers())).toArray(Method[]::new);
            for (Method method : methods) {
                implementMethod(classInfo, method);
            }

            classInfo.content.append("}\n");

            return JavaC.compile(classInfo.superClassName + PROXY_SUFFIX, classInfo.toString());
        }).asSubclass(clazz);
    }

    private static void appendGenericParameters(ClassInfo classInfo, boolean bound) {
        if (classInfo.params.length == 0) return;
        classInfo.content.append("<").append(bound ? genericParam(classInfo.params[0]) : classInfo.params[0].getName());
        for (int i = 1; i < classInfo.params.length; i++) {
            classInfo.content.append(" , ").append(bound ? genericParam(classInfo.params[i]) : classInfo.params[i].getName());
        }
        classInfo.content.append(">");
    }

    private static String genericParam(TypeVariable type) {
        Type[] bounds = type.getBounds();
        if (bounds.length == 0) {
            return type.getName();
        } else {
            return type.getName() + " extends " + String.join(" & ", Stream.of(bounds).map(Type::getTypeName).collect(Collectors.toList()));
        }
    }

    private static void appendGeneratedFieldsAndMethods(ClassInfo classInfo) {

        classInfo.content.append("  private final ibessonov.cdi.internal.$Context $context;\n");
        classInfo.content.append("  private final Class<?>[] $generics;\n");

        classInfo.content.append("  public ").append(classInfo.className);
        classInfo.content.append("(ibessonov.cdi.internal.$Context $context, Class<?>[] $generics) {\n");
        classInfo.content.append("    this.$context  = $context;\n");
        classInfo.content.append("    this.$generics = $generics;\n");
        classInfo.content.append("  }\n");

        classInfo.content.append("  @Override public final ibessonov.cdi.internal.$Context $context() {\n");
        classInfo.content.append("    return this.$context;\n");
        classInfo.content.append("  }\n");

        classInfo.content.append("  @Override public final Class<?>[] $generics() {\n");
        classInfo.content.append("    return this.$generics;\n");
        classInfo.content.append("  }\n");
    }

    private static void appendFieldConstruction(ClassInfo classInfo, Field field) {
        classInfo.content.append("    this.").append(field.getName()).append(" = ");
        appendLookup(classInfo, field.getGenericType());
        classInfo.content.append(";\n");
    }

    private static void appendConstructorInvocation(ClassInfo classInfo) {
        Method[] methods = Stream.of(classInfo.clazz.getDeclaredMethods()).filter(
                m -> m.isAnnotationPresent(Constructor.class)
        ).toArray(Method[]::new);
        if (methods.length < 1) return;
        if (methods.length > 1) throw new CdiException(TOO_MANY_CONSTRUCTORS, classInfo.clazz.getCanonicalName());
        Method constructor = methods[0];

        validateConstructor(constructor);
        int parameterCount = constructor.getParameterCount();

        Type[] parameterTypes = constructor.getGenericParameterTypes();
        classInfo.content.append("    this.").append(constructor.getName()).append("(");
        for (int i = 0; i < parameterCount; i++) {
            if (i + 1 != parameterCount) classInfo.content.append(", ");
            appendLookup(classInfo, parameterTypes[i]);
        }
        classInfo.content.append(");\n");
    }


    private static <T> void implementMethod(ClassInfo<T> classInfo, Method method) {
        if (!implementable(method)) {
            throw new CdiException(CdiErrorType.UNIMPLEMENTABLE_ABSTRACT_METHOD, classInfo.clazz.getCanonicalName(), method.getName());
        }
        String returnTypeName = method.getGenericReturnType().getTypeName();
        classInfo.content.append("  @Override public ").append(returnTypeName);
        classInfo.content.append(" ").append(method.getName()).append("() {\n    return ");
        appendLookup(classInfo, method.getGenericReturnType());
        classInfo.content.append(";\n  }\n");
    }

    private static void appendLookup(ClassInfo classInfo, Type type) {
        if (type == Class.class) {
            throw new CdiException(CLASS_INJECTION, classInfo.clazz.getCanonicalName(), type.getTypeName());
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() == Class.class) {
                Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];

                if (!(actualTypeArgument instanceof TypeVariable)) {
                    throw new CdiException(CLASS_INJECTION, classInfo.clazz.getCanonicalName(), type.getTypeName());
                }

                TypeVariable actualTypeVariable = (TypeVariable) actualTypeArgument;
                String actualTypeVariableName = actualTypeVariable.getName();

                classInfo.content.append("(Class<").append(actualTypeVariableName).append(">) $generics[");
                classInfo.content.append(classInfo.paramsIndex.get(actualTypeVariableName)).append("];\n");

                return;
            }
        }

        validateType(classInfo, type);

        if (type instanceof Class) {
            classInfo.content.append("(").append(((Class) type).getCanonicalName()).append(") ");
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            classInfo.content.append("(").append(((Class) parameterizedType.getRawType()).getCanonicalName()).append(") ");
        } else if (type instanceof TypeVariable) {
            classInfo.content.append("(").append(type.getTypeName()).append(") ");
        }
        classInfo.content.append("$context.lookup(");

        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Class rawType = (Class) pType.getRawType();
            String canonicalName = rawType.getCanonicalName();
            classInfo.content.append(canonicalName).append(".class");

            if (rawType.isAnnotationPresent(Scoped.class)) {
                Type[] actualTypeArguments = pType.getActualTypeArguments();
                for (Type actualTypeArgument : actualTypeArguments) {
                    if (actualTypeArgument instanceof ParameterizedType) {
                        Class parameter = (Class) ((ParameterizedType) actualTypeArgument).getRawType();
                        classInfo.content.append(", ").append(parameter.getCanonicalName()).append(".class");
                    } else if (actualTypeArgument instanceof TypeVariable) {
                        TypeVariable actualTypeVariable = (TypeVariable) actualTypeArgument;
                        String actualTypeVariableName = actualTypeVariable.getName();

                        classInfo.content.append(", $generics[");
                        classInfo.content.append(classInfo.paramsIndex.get(actualTypeVariableName)).append("]");
                    } else {
                        Class parameter = (Class) actualTypeArgument;
                        classInfo.content.append(", ").append(parameter.getCanonicalName()).append(".class");
                    }
                }
            } else {
                // maybe without lookup?
            }
        } else if (type instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) type;
            String typeVariableName = typeVariable.getName();

            //TODO what if this type is also parameterized?
            classInfo.content.append("$generics[");
            classInfo.content.append(classInfo.paramsIndex.get(typeVariableName)).append("]");
        } else {
            Class cType = (Class) type;
            classInfo.content.append(cType.getCanonicalName()).append(".class");
            // maybe without lookup?
        }
        classInfo.content.append(")");
    }

    private static boolean injectable(Field field) {
        return field.isAnnotationPresent(Inject.class);
    }

    private static boolean implementable(Method method) {
        return method.getTypeParameters().length == 0 && method.getParameterCount() == 0
                && !method.getReturnType().isPrimitive() && method.isAnnotationPresent(Provided.class); // "lookupable" return type
    }

    private static void validateConstructor(Method constructor) {
        Class<?>[] exceptions = constructor.getExceptionTypes();
        for (Class<?> exceptionClass : exceptions) {
            if (!RuntimeException.class.isAssignableFrom(exceptionClass)
                    && !Error.class.isAssignableFrom(exceptionClass)) {
                throw new RuntimeException();
            }
        }
        if (constructor.getTypeParameters().length > 0) {
            throw new RuntimeException();
        }
    }

    private static void validateType(ClassInfo<?> classInfo, Type type) {
        if (type instanceof Class) {
            Class clazz = (Class) type; // raw type should be validated in future
            if (clazz.isAnnotationPresent(Scoped.class)) {
                TypeVariable[] params = clazz.getTypeParameters();
                if (params.length > 0) {
                    throw new CdiException(GENERIC_PARAMETERS_COUNT_MISMATCH, clazz.getCanonicalName(), 0, params.length);
                }
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Class clazz = (Class) pType.getRawType(); // raw type should be validated in future, but not parameterized type. That would cause infinite recursion in some cases
            if (clazz.isAnnotationPresent(Scoped.class)) {
                for (Type param : pType.getActualTypeArguments()) {
                    validateType(classInfo, param);
                }
            }
        } else if (type instanceof TypeVariable) {
            // remember it for future checks
            TypeVariable typeVariable = (TypeVariable) type;
            int index = classInfo.paramsIndex.get(typeVariable.getName());
            // we need to have context for this type variable and its index in outer declaration!
        } else if (type instanceof WildcardType) {
            throw new RuntimeException("Wildcard type parameter is not supported");
        } else if (type instanceof GenericArrayType) {
            throw new RuntimeException("Array type parameter is not supported");
        }
    }

    private static class ClassInfo<T> {

        public final Class<T>             clazz;
        public final TypeVariable[]       params;
        public final String               packageName;
        public final String               superClassName;
        public final String               className;

        public final StringBuilder        content;

        public final Map<String, Integer> paramsIndex;

        public ClassInfo(Class<T> clazz) {
            this.clazz          = clazz;
            this.params         = clazz.getTypeParameters();

            this.packageName    = clazz.getPackage().getName();
            this.superClassName = clazz.getName();
            this.className      = superClassName.substring(packageName.length() + 1) + PROXY_SUFFIX;

            this.content        = new StringBuilder();

            this.paramsIndex    = new HashMap<>();
            for (int i = 0; i < params.length; i++) {
                paramsIndex.put(params[i].getName(), i);
            }
        }

        @Override
        public String toString() {
            return content.toString();
        }
    }

    private InheritorGenerator() {
    }
}
