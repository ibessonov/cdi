package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StMethodCallStatement implements StStatement {

    public final StExpression expression;

    public StMethodCallStatement(StExpression expression) {
        this.expression = expression;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitMethodCallStatement(this);
    }
}
