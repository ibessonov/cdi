package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public interface StTypedExpression extends StExpression {

    Class<?> getType();
}
