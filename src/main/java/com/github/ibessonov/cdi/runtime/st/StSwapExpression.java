package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StSwapExpression implements StExpression {

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitSwapExpression(this);
    }
}
