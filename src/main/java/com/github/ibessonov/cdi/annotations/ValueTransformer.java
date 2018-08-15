package com.github.ibessonov.cdi.annotations;

import java.lang.annotation.Annotation;

/**
 * @author ibessonov
 */
@FunctionalInterface
public interface ValueTransformer<T extends Annotation> {

    Object transform(T annotation, Class<?> clazz, Object object);

    default boolean isApplicable(Class<?> clazz) {
        return true;
    }
}
