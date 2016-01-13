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
import java.util.*;

import static java.lang.reflect.Modifier.*;
import static org.ibess.cdi.enums.CdiErrorType.*;
import static org.ibess.cdi.enums.Scope.STATELESS;
import static org.ibess.cdi.util.Cdi.isChecked;

/**
 * @author ibessonov
 */
public final class InheritorGenerator {

    private static final Map<Class, ClassInfo> cache = new HashMap<>();

    public static Class getSubclass(Class clazz) {
        ClassInfo ci = cache.get(clazz);
        if (ci == null || !ci.compiled) {
            synchronized (InheritorGenerator.class) {
                ci = cache.computeIfAbsent(clazz, InheritorGenerator::getClassInfo);
                if (!ci.compiled) {
                    Map<String, String> contents = new HashMap<>();
                    buildSources(ci, contents);
                    ci.compiledClass = JavaC.compile(contents, ci.resultName);
                }
            }
        }
        Class result = ci.compiledClass;
        return (result != null) ? result : (ci.compiledClass = forName(ci.resultName));
    }

    private static ClassInfo getClassInfo(Class<?> clazz) {
        if (isFinal(clazz.getModifiers())) {
            throw new CdiException(FINAL_SCOPED_CLASS, clazz.getCanonicalName());
        }
        if (clazz.getEnclosingClass() != null && !isStatic(clazz.getModifiers())) {
            // throw
        }
        Scoped scoped = clazz.getAnnotation(Scoped.class);
        if (scoped == null) { // remove this checking in future. This class has to be invisible for users
            throw new ImpossibleError();
        }
        if (scoped.value() != STATELESS && clazz.getTypeParameters().length > 0) {
            throw new CdiException(PARAMETERIZED_NON_STATELESS, clazz.getCanonicalName());
        }

        ClassInfo ci = new ClassInfo(clazz);

        for (Field field : ci.declaredFields) {
            if (injectable(field)) {
                validateFieldForInjection(ci, field);
                appendFieldConstruction(ci, field);
            }
        }
        appendConstructorInvocation(ci);

        for (Method method : ci.declaredMethods) {
            if (isAbstract(method.getModifiers())) implementMethod(ci, method);
        }

        return ci;
    }

    private static Class forName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new ImpossibleError(e);
        }
    }

    private static void buildSources(ClassInfo ci, Map<String, String> contents) {
        if (ci.compiled) return;
        if (contents.containsKey(ci.resultName)) return;
        contents.put(ci.resultName, ci.toString());

        for (Class clazz : ci.dependsOn) {
            ClassInfo info = cache.computeIfAbsent(clazz, InheritorGenerator::getClassInfo);
            buildSources(info, contents);
        }
        ci.dependsOn = null;
        ci.compiled = true;
    }

    private static void appendFieldConstruction(ClassInfo ci, Field field) {
        ci.builder.getConstructMethod().addStatement(ci.builder.newAssignmentStatement(
                field.getName(), ci.builder.newLookupExpression(field.getGenericType())
        ));
    }

    private static void appendConstructorInvocation(ClassInfo ci) {
        Method constructor = null;
        for (Method method : ci.declaredMethods) {
            if (method.isAnnotationPresent(Constructor.class)) {
                if (constructor != null) {
                    throw new CdiException(TOO_MANY_CONSTRUCTORS, ci.originalName);
                }
                constructor = method;
            }
        }
        if (constructor == null) return;

        validateConstructor(ci, constructor);
        int parameterCount = constructor.getParameterCount();

        Type[] parameterTypes = constructor.getGenericParameterTypes();
        List<Expression> params = new ArrayList<>(parameterCount);

        for (int i = 0; i < parameterCount; i++) {
            validateLookup(ci, parameterTypes[i]);
            params.add(ci.builder.newLookupExpression(parameterTypes[i]));
        }
        ci.builder.getConstructMethod().addStatement(ci.builder.newMethodCallStatement("super." + constructor.getName(), params));
    }

    private static void implementMethod(ClassInfo ci, Method method) {
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

    private static void validateFieldForInjection(ClassInfo ci, Field field) {
        if (isPrivate(field.getModifiers())) {
            throw new CdiException(PRIVATE_FIELD_INJECTION, ci.originalName, field.getName());
        }
        if (isFinal(field.getModifiers())) {
            throw new CdiException(FINAL_FIELD_INJECTION, ci.originalName, field.getName());
        }
        validateLookup(ci, field.getGenericType());
    }

    private static boolean implementable(Method method) {
        return method.getTypeParameters().length == 0 && method.getParameterCount() == 0
                && !method.getReturnType().isPrimitive() && method.isAnnotationPresent(Provided.class); // " lookupable" return type
    }

    private static void validateConstructor(ClassInfo ci, Method constructor) {
        Class[] exceptions = constructor.getExceptionTypes();
        for (Class exceptionClass : exceptions) {
            if (!isChecked(exceptionClass)) {
                throw new CdiException(CONSTRUCTOR_THROWS_EXCEPTION, ci.originalName, exceptionClass.getCanonicalName());
            }
        }
        if (isPrivate(constructor.getModifiers())) {
            throw new CdiException(CONSTRUCTOR_IS_PRIVATE, ci.originalName);
        }
        if (isAbstract(constructor.getModifiers())) {
            throw new CdiException(CONSTRUCTOR_IS_ABSTRACT, ci.originalName);
        }
        if (constructor.getTypeParameters().length > 0) {
            throw new CdiException(CONSTRUCTOR_IS_GENERIC, ci.originalName);
        }
    }

    private static void validateType(ClassInfo ci, Type type) {
        if (type instanceof Class) {
            Class clazz = (Class) type; // raw type should be validated in future
            if (clazz.isAnnotationPresent(Scoped.class)) {
                TypeVariable[] params = clazz.getTypeParameters();
                if (params.length > 0) {
                    throw new CdiException(GENERIC_PARAMETERS_COUNT_MISMATCH, clazz.getCanonicalName(), 0, params.length);
                }
                ci.dependsOn.add(clazz);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Class clazz = (Class) pType.getRawType(); // raw type should be validated in future, but not parameterized type. That would cause infinite recursion in some cases
            if (clazz.isAnnotationPresent(Scoped.class)) {
                for (Type param : pType.getActualTypeArguments()) {
                    validateType(ci, param);
                }
                ci.dependsOn.add(clazz);
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

    private static class ClassInfo {

        public final Class                clazz;
        public final String               originalName;
        public final TypeVariable[]       params;
        public final Map<String, Integer> paramsIndex;
        public final Field[]              declaredFields;
        public final Method[]             declaredMethods;
        public final String               resultName;

        public final ClassBuilder builder;

        public boolean compiled = false;
        public Class compiledClass;
        public Set<Class> dependsOn = new HashSet<>();

        public ClassInfo(Class clazz) {
            this.clazz          = clazz;
            this.originalName   = clazz.getCanonicalName();
            this.params         = clazz.getTypeParameters();

            this.paramsIndex    = new HashMap<>();
            for (int i = 0, length = params.length; i < length; i++) {
                paramsIndex.put(params[i].getName(), i);
            }

            this.declaredFields  = clazz.getDeclaredFields();
            this.declaredMethods = clazz.getDeclaredMethods();

            this.resultName = clazz.getName() + ClassBuilder.SUFFIX;

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
