package com.github.ibessonov.cdi;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * @author ibessonov
 */
@FunctionalInterface
public interface MethodTransformer<T extends Annotation> {

    MethodHandle transform(T annotation, Method method, MethodHandle handle);
}
