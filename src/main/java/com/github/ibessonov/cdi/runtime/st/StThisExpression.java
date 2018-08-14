package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StThisExpression implements StExpression {

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitThisExpression(this);
    }
}
