package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StParamAssignmentStatement implements StStatement {

    public final int index;
    public final StExpression expression;

    public StParamAssignmentStatement(int index, StExpression expression) {

        this.index = index;
        this.expression = expression;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitParamAssignmentStatement(this);
    }
}
