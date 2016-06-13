package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StReturnHookStatement implements StStatement {

    public final StStatement statement;
    public final StStatement hook;

    public StReturnHookStatement(StStatement statement, StStatement hook) {
        this.statement = statement;
        this.hook = hook;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitReturnHookStatement(this);
    }
}
