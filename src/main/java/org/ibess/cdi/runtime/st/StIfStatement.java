package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StIfStatement implements StStatement {

    public final boolean negate;
    public final StExpression condition;
    public final StStatement then;
    public final StStatement els;

    public StIfStatement(boolean negate, StExpression condition, StStatement then, StStatement els) {
        this.negate = negate;
        this.condition = condition;
        this.then = then;
        this.els = els;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitIfStatement(this);
    }
}
