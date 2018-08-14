package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StIntConstantExpression implements StExpression {
    public final int index;
    public final Class<?> type;

    public StIntConstantExpression(int index, Class<?> type) {
        this.index = index;
        this.type = type;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitIntConstantExpression(this);
    }
}
