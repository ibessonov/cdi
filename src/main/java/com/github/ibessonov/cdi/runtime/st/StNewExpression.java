package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StNewExpression implements StExpression {

    public final String className;

    public StNewExpression(String className) {
        this.className = className;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitNewExpression(this);
    }
}
