package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StNullExpression implements StExpression {

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitNullExpression(this);
    }
}
