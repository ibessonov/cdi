package org.ibess.cdi;

import java.lang.annotation.Annotation;

/**
 * @author ibessonov
 */
public interface Registrar {

    <T extends Annotation> void registerTransformer(Class<T> clazz, Transformer<T> transformer);
}
