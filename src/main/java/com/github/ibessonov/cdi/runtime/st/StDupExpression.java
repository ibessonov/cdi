package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StDupExpression implements StExpression {

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitDupExpression(this);
    }
}
