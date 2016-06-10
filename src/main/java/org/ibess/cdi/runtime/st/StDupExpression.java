package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StDupExpression implements StExpression {

    public final StExpression expression;

    public StDupExpression(StExpression expression) {
        this.expression = expression;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitDupExpression(this);
    }
}
