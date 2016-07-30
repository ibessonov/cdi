package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
//TODO same as StIfStatement? Should I merge them?
public class StIfNullStatement implements StStatement {

    public final boolean negate;
    public final StExpression expression;
    public final StStatement then;
    public final StStatement els;

    public StIfNullStatement(boolean negate, StExpression expression, StStatement then, StStatement els) {
        this.negate = negate;
        this.expression = expression;
        this.then = then;
        this.els = els;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitIfNullStatement(this);
    }
}
