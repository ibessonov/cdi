package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StCastExpression implements StExpression {
    public final Class<?> type;
    public final StExpression expression;

    public StCastExpression(Class<?> type, StExpression expression) {
        this.type = type;
        this.expression = expression;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitCastExpression(this);
    }
}
