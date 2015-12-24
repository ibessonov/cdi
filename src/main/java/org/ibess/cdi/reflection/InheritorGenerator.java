package org.ibess.cdi.reflection;

import org.ibess.cdi.annotations.Constructor;
import org.ibess.cdi.annotations.Inject;
import org.ibess.cdi.annotations.Provided;
import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.exceptions.ImpossibleError;
import org.ibess.cdi.javac.JavaC;
import org.ibess.cdi.reflection.ClassBuilder.Expression;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static org.ibess.cdi.enums.CdiErrorType.*;
import static org.ibess.cdi.enums.Scope.STATELESS;
import static org.ibess.cdi.util.Cdi.isChecked;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;

/**
 * @author ibessonov
 */
public final class InheritorGenerator {

    //TODO global lock required for planned improvements
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
                throw new CdiException(PARAMETERIZED_NON_STATELESS, clazz.getCanonicalName());
            }

            ClassInfo<T> ci = new ClassInfo<>(clazz);

            Field[] fields = Stream.of(clazz.getDeclaredFields()).filter(InheritorGenerator::injectable).toArray(Field[]::new);
            for (Field field : fields) {
                appendFieldConstruction(ci, field);
            }
            appendConstructorInvocation(ci);

            Method[] methods = Stream.of(clazz.getDeclaredMethods()).filter(m -> isAbstract(m.getModifiers())).toArray(Method[]::new);
            for (Method method : methods) {
                implementMethod(ci, method);
            }

            return JavaC.compile(clazz.getName() + ClassBuilder.SUFFIX, ci.toString());
        }).asSubclass(clazz);
    }

    private static void appendFieldConstruction(ClassInfo ci, Field field) {
        validateLookup(ci, field.getGenericType());

        ci.builder.getConstructMethod().addStatement(ci.builder.newAssignmentStatement(
                field.getName(), ci.builder.newLookupExpression(field.getGenericType())
        ));
    }

    private static void appendConstructorInvocation(ClassInfo<?> ci) {
        Method[] methods = Stream.of(ci.clazz.getDeclaredMethods()).filter(
                m -> m.isAnnotationPresent(Constructor.class)
        ).toArray(Method[]::new);
        if (methods.length < 1) return;
        if (methods.length > 1) throw new CdiException(TOO_MANY_CONSTRUCTORS, ci.originalName);
        Method constructor = methods[0];

        validateConstructor(ci, constructor);
        int parameterCount = constructor.getParameterCount();

        Type[] parameterTypes = constructor.getGenericParameterTypes();
        List<Expression> params = new ArrayList<>(parameterCount);

        for (int i = 0; i < parameterCount; i++) {
            validateLookup(ci, parameterTypes[i]);
            params.add(ci.builder.newLookupExpression(parameterTypes[i]));
        }
        ci.builder.getConstructMethod().addStatement(ci.builder.newMethodCallStatement(constructor.getName(), params));
    }


    private static <T> void implementMethod(ClassInfo<T> ci, Method method) {
        if (!implementable(method)) {
            throw new CdiException(UNIMPLEMENTABLE_ABSTRACT_METHOD, ci.originalName, method.getName());
        }

        validateLookup(ci, method.getGenericReturnType());

        ClassBuilder.MethodInfo newMethod = ci.builder.createNewMethod(method.getName(), method.getGenericReturnType());
        newMethod.addStatement(ci.builder.newReturnStatement(ci.builder.newLookupExpression(method.getGenericReturnType())));
    }

    private static void validateLookup(ClassInfo ci, Type type) {
        if (type == Class.class) {
            throw new CdiException(CLASS_INJECTION, ci.originalName, type.getTypeName());
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() == Class.class) {
                Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
                if (!(actualTypeArgument instanceof TypeVariable)) {
                    throw new CdiException(CLASS_INJECTION, ci.originalName, type.getTypeName());
                }
                return;
            }
        }

        validateType(ci, type);
    }

    private static boolean injectable(Field field) {
        return field.isAnnotationPresent(Inject.class);
    }

    private static boolean implementable(Method method) {
        return method.getTypeParameters().length == 0 && method.getParameterCount() == 0
                && !method.getReturnType().isPrimitive() && method.isAnnotationPresent(Provided.class); // " lookupable" return type
    }

    private static void validateConstructor(ClassInfo<?> ci, Method constructor) {
        Class<?>[] exceptions = constructor.getExceptionTypes();
        for (Class<?> exceptionClass : exceptions) {
            if (!isChecked(exceptionClass)) {
                throw new CdiException(CONSTRUCTOR_THROWS_EXCEPTION, ci.originalName);
            }
        }
        if (isAbstract(constructor.getModifiers())) {
            throw new CdiException(CONSTRUCTOR_IS_ABSTRACT, ci.originalName);
        }
        if (constructor.getTypeParameters().length > 0) {
            throw new CdiException(CONSTRUCTOR_IS_GENERIC, ci.originalName);
        }
    }

    private static void validateType(ClassInfo<?> ci, Type type) {
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
                    validateType(ci, param);
                }
            }
        } else if (type instanceof TypeVariable) {
            // remember it for future checks
            TypeVariable typeVariable = (TypeVariable) type;
            int index = ci.paramsIndex.get(typeVariable.getName());
            // we need to have context for this type variable and its index in outer declaration!
        } else if (type instanceof WildcardType) {
            throw new CdiException(WILDCARD_TYPE_PARAMETER, ci.originalName);
        } else if (type instanceof GenericArrayType) {
            throw new CdiException(ARRAY_TYPE_PARAMETER, ci.originalName);
        }
    }

    private static class ClassInfo<T> {

        public final Class<T>             clazz;
        public final String               originalName;
        public final TypeVariable[]       params;
        public final Map<String, Integer> paramsIndex;

        public final ClassBuilder builder;

        public ClassInfo(Class<T> clazz) {
            this.clazz          = clazz;
            this.originalName   = clazz.getCanonicalName();
            this.params         = clazz.getTypeParameters();

            this.paramsIndex    = new HashMap<>();
            for (int i = 0, length = params.length; i < length; i++) {
                paramsIndex.put(params[i].getName(), i);
            }

            builder = new ClassBuilder(clazz);
        }

        @Override
        public String toString() {
            return builder.build();
        }
    }

    private InheritorGenerator() {
    }
}
