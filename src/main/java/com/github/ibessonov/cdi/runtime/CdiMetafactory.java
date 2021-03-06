package com.github.ibessonov.cdi.runtime;

import com.github.ibessonov.cdi.annotations.MethodTransformer;
import com.github.ibessonov.cdi.annotations.ValueTransformer;
import com.github.ibessonov.cdi.exceptions.ImpossibleError;
import com.github.ibessonov.cdi.internal.$Context;

import java.lang.annotation.Annotation;
import java.lang.invoke.*;
import java.lang.reflect.Method;

import static com.github.ibessonov.cdi.runtime.ContextImpl.findContext;
import static com.github.ibessonov.cdi.util.BoxingUtil.boxHandle;
import static com.github.ibessonov.cdi.util.BoxingUtil.unboxHandle;
import static com.github.ibessonov.cdi.util.ClassUtil.isPrimitive;
import static com.github.ibessonov.cdi.util.CollectionUtil.drop;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;

/**
 * @author ibessonov
 */
public class CdiMetafactory {

    private static final MethodHandle IDENTITY = identity(Object.class);
    private static final MethodHandle transformHandle;

    static {
        try {
            transformHandle = publicLookup().findVirtual(
                ValueTransformer.class, "transform",
                methodType(Object.class, Annotation.class, Class.class, Object.class)
            );
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused")
    public static CallSite implement(MethodHandles.Lookup caller, String invokedName,
                                     MethodType invokedType, String generatedName, String contextId) {
        Class<?> callerClass = invokedType.parameterType(0);
        Method method = findMethod(invokedName, invokedType);
        try {
            MethodHandle methodHandle;
            if (invokedName.equals(generatedName)) {
                methodHandle = caller.unreflectSpecial(method, callerClass);
            } else {
                MethodType shortenedType = invokedType.dropParameterTypes(0, 1);
                methodHandle = caller.findSpecial(callerClass, generatedName, shortenedType, callerClass);
            }
            return new ConstantCallSite(transform(findContext(contextId), method, methodHandle));
        } catch (NoSuchMethodException e) {
            throw new ImpossibleError(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    public static CallSite context(MethodHandles.Lookup caller, String invokedName,
                                   MethodType invokedType, String contextId) {
        ContextImpl context = findContext(contextId);
        return new ConstantCallSite(MethodHandles.constant($Context.class, context));
    }

    private static Method findMethod(String invokedName, MethodType invokedType) {
        Class<?>[] parameterArray = invokedType.parameterArray();
        Class<?> callerClass = parameterArray[0];
        Class<?> declaringClass = callerClass.getSuperclass();
        Class<?>[] parameterTypes = drop(1, parameterArray);
        try {
            return declaringClass.getDeclaredMethod(invokedName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new ImpossibleError(e);
        }
    }

    private static MethodHandle transform(ContextImpl context, Method method, MethodHandle methodHandle) {
        MethodType methodType = methodHandle.type();

        {
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0, parametersCount = parameterTypes.length; i < parametersCount; i++) {
                Class<?> parameterType = parameterTypes[i];
                MethodHandle transform = null;
                for (Annotation annotation : parameterAnnotations[i]) {
                    if (context.valueTransformerRegistered(annotation.annotationType())) {
                        MethodHandle transformBind = transformHandle
                                .bindTo(context.getValueTransformer(annotation.annotationType()))
                                .bindTo(annotation)
                                .bindTo(parameterType);

                        if (transform == null) {
                            transform = transformBind;
                        } else {
                            transform = filterArguments(transform, 0, transformBind);
                        }
                    }
                }
                if (transform != null) {
                    methodHandle = filterArguments(methodHandle, i + 1, cast(transform, parameterType));
                }
            }
        }

        Class<?> returnType = method.getReturnType();
        boolean voidReturnType = returnType == void.class;

        Annotation[] methodAnnotations = method.getAnnotations();
        for (Annotation annotation : methodAnnotations) {
            if (context.methodTransformerRegistered(annotation.annotationType())) {
                MethodTransformer methodTransformer = context.getMethodTransformer(annotation.annotationType());
                if (methodTransformer != null) {
                    //noinspection unchecked
                    methodHandle = methodTransformer.transform(annotation, method, methodHandle);
                    assert methodType.equals(methodHandle.type());
                }
            }
        }

        if (!voidReturnType) {
            MethodHandle transform = null;
            for (Annotation annotation : methodAnnotations) {
                if (context.valueTransformerRegistered(annotation.annotationType())) {
                    MethodHandle transformBind = transformHandle
                            .bindTo(context.getValueTransformer(annotation.annotationType()))
                            .bindTo(annotation)
                            .bindTo(returnType);

                    if (transform == null) {
                        transform = transformBind;
                    } else {
                        transform = filterArguments(transform, 0, transformBind);
                    }
                }
            }
            if (transform != null) {
                methodHandle = filterReturnValue(methodHandle, cast(transform, returnType));
            }
        }

        return methodHandle;
    }

    private static MethodHandle cast(MethodHandle target, Class<?> type) {
        target = filterArguments(target, 0, toObjectFilter(type));
        target = filterReturnValue(target, fromObjectFilter(type));
        return target;
    }

    private static MethodHandle toObjectFilter(Class<?> type) {
        return isPrimitive(type) ? boxHandle(type) : identity(type).asType(methodType(Object.class, type));
    }

    private static MethodHandle fromObjectFilter(Class<?> type) {
        return isPrimitive(type) ? unboxHandle(type) : IDENTITY.asType(methodType(type, Object.class));
    }
}
