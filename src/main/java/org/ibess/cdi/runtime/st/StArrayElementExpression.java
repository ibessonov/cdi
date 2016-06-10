package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StArrayElementExpression implements StExpression {

    public final StExpression array;
    public final StExpression index;

    public StArrayElementExpression(StExpression array, StExpression index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitArrayElementExpression(this);
    }
}
