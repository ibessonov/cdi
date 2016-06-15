package org.ibess.cdi;

import org.ibess.cdi.runtime.st.StStatement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author ibessonov
 */
@FunctionalInterface
public interface MethodTransformer<T extends Annotation> {

    StStatement transform(StStatement statement, Method method, T annotation);
}
