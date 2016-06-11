package org.ibess.cdi;

import java.lang.annotation.Annotation;

/**
 * @author ibessonov
 */
public interface Transformer<T extends Annotation> {

    Object transform(Object object, T annotation);
}
