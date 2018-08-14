package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StClassExpression implements StExpression {
    public final Class<?> type;

    public StClassExpression(Class<?> type) {
        this.type = type;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitClassExpression(this);
    }
}
