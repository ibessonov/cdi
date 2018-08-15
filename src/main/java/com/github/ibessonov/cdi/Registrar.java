package com.github.ibessonov.cdi;

import com.github.ibessonov.cdi.annotations.MethodTransformer;
import com.github.ibessonov.cdi.annotations.ValueTransformer;

import java.lang.annotation.Annotation;

/**
 * @author ibessonov
 */
public interface Registrar {

    <T extends Annotation> void registerValueTransformer(Class<T> clazz, ValueTransformer<T> valueTransformer);

    <T extends Annotation> void registerMethodTransformer(Class<T> clazz, MethodTransformer<T> methodTransformer);

    <T> void registerProvider(Class<T> clazz, Provider<T> provider);
}
