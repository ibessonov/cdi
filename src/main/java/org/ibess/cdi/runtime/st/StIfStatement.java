package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StIfStatement implements StStatement {

    public final boolean compareToNull;
    public final boolean negate;
    public final StExpression expression;
    public final StStatement then;
    public final StStatement els;

    public StIfStatement(boolean compareToNull, boolean negate, StExpression expression, StStatement then, StStatement els) {
        this.compareToNull = compareToNull;
        this.negate = negate;
        this.expression = expression;
        this.then = then;
        this.els = els;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitIfStatement(this);
    }
}
