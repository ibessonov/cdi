package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StNoopStatement implements StStatement {

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitNoopStatement(this);
    }
}
