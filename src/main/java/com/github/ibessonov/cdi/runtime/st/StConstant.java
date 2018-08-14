package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StConstant implements StExpression {

    public final Object value;

    public StConstant(Object value) {
        this.value = value;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitConstant(this);
    }
}
