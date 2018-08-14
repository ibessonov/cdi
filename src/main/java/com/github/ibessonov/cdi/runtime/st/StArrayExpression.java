package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StArrayExpression implements StExpression {

    public final Class<?> type;
    public final StExpression[] elements;

    public StArrayExpression(Class<?> type, StExpression[] elements) {
        this.type = type;
        this.elements = elements.clone();
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitArrayExpression(this);
    }
}
