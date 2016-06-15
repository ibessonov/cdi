package org.ibess.cdi;

import java.lang.annotation.Annotation;

/**
 * @author ibessonov
 */
@FunctionalInterface
public interface ValueTransformer<T extends Annotation> {

    Object transform(Object object, Class clazz, T annotation);
}
