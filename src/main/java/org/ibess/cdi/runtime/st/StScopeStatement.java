package org.ibess.cdi.runtime.st;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * @author ibessonov
 */
public class StScopeStatement implements StStatement {
    public final List<StStatement> statements;

    public <L extends List<StStatement> & RandomAccess> StScopeStatement(L statements) {
        this.statements = Collections.unmodifiableList(statements);
    }

    public StScopeStatement(StStatement... statements) {
        this.statements = Arrays.asList(statements);
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitScopeStatement(this);
    }
}
