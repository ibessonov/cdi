package org.ibess.cdi.runtime.st;

import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * @author ibessonov
 */
public class StArrayExpression implements StExpression {
    public final Class<?> type;
    public final List<StExpression> elements;

    public <L extends List<StExpression> & RandomAccess> StArrayExpression(Class<?> type, L elements) {
        this.type = type;
        this.elements = Collections.unmodifiableList(elements);
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitArrayExpression(this);
    }
}
