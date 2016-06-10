package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StGetParameterExpression implements StExpression {

    public final int index;

    public StGetParameterExpression(int index) {
        this.index = index;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitGetParameterExpression(this);
    }
}
