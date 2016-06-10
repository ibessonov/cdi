package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StIntConstantExpression implements StExpression {
    public final int index;

    public StIntConstantExpression(int index) {
        this.index = index;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitIntConstantExpression(this);
    }
}
