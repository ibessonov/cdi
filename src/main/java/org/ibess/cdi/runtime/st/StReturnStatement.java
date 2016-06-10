package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StReturnStatement implements StStatement {

    public final StExpression expression;

    public StReturnStatement(StExpression expression) {
        this.expression = expression;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitReturnStatement(this);
    }
}
