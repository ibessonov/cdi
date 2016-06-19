package org.ibess.cdi;

import java.lang.annotation.Annotation;

/**
 * @author ibessonov
 */
public interface Registrar {

    <T extends Annotation> void registerValueTransformer(Class<T> clazz, ValueTransformer<T> valueTransformer);

    <T extends Annotation> void registerMethodTransformer(Class<T> clazz, MethodTransformer<T> methodTransformer);

//    <T extends Annotation> void registerClassTransformer(Class<T> clazz, ClassTransformer<T> classTransformer);
}