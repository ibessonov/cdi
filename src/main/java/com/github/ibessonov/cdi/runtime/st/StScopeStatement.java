package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StScopeStatement implements StStatement {

    public final StStatement[] statements;

    public StScopeStatement(StStatement... statements) {
        this.statements = statements.clone();
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitScopeStatement(this);
    }
}
