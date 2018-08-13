package org.ibess.cdi.runtime;

import org.ibess.cdi.MethodTransformer;
import org.ibess.cdi.ValueTransformer;
import org.ibess.cdi.exceptions.ImpossibleError;

import java.lang.annotation.Annotation;
import java.lang.invoke.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;
import static org.ibess.cdi.util.BoxingUtil.*;
import static org.ibess.cdi.util.ClassUtil.isPrimitive;
import static org.ibess.cdi.util.CollectionUtil.drop;

/**
 * @author ibessonov
 */
public class CdiMetafactory {

    private static final Map<String, WeakReference<ContextImpl>> contexts = new ConcurrentHashMap<>();
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

    public static void register(ContextImpl context) {
        contexts.put(context.contextId, new WeakReference<>(context));
    }

    public static CallSite implement(MethodHandles.Lookup caller,
                                     String invokedName,
                                     MethodType invokedType,
                                     String generatedName) {
        Class<?> callerClass = invokedType.parameterArray()[0];
        Method method = findMethod(invokedName, invokedType);
        try {
            MethodHandle methodHandle;
            if (invokedName.equals(generatedName)) {
                methodHandle = caller.unreflectSpecial(method, callerClass);
            } else {
                MethodType shortenedType = invokedType.dropParameterTypes(0, 1);
                methodHandle = caller.findSpecial(callerClass, generatedName, shortenedType, callerClass);
            }
            return new ConstantCallSite(transform(findContext(callerClass), method, methodHandle));
        } catch (NoSuchMethodException e) {
            throw new ImpossibleError(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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

    private static final Pattern contextPattern = Pattern.compile("\\$Cdi(\\d+)$"); //TODO this smells
    private static ContextImpl findContext(Class<?> clazz) {
        Matcher matcher = contextPattern.matcher(clazz.getName());
        if (matcher.find()) {
            String contextId = matcher.group(1);
            WeakReference<ContextImpl> contextReference = contexts.get(contextId);
            if (contextReference != null) {
                ContextImpl context = contextReference.get();
                if (context != null) {
                    return context;
                } else {
                    contexts.remove(contextId);
                }
            }
        }
        throw new IllegalArgumentException("CdiMetafactory cannot find context for class " + clazz);
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
