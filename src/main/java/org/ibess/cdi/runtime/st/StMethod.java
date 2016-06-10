package org.ibess.cdi.runtime.st;

import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * @author ibessonov
 */
public class StMethod implements StNode {

    public final boolean isStatic;
    public final String name;
    public final List<Class<?>> parameters;
    public final Class<?> returnType;
    public final StStatement statement;

    public <L extends List<Class<?>> & RandomAccess> StMethod(boolean isStatic, String name, L parameters, Class<?> returnType, StStatement statement) {
        this.name = name;
        this.parameters = Collections.unmodifiableList(parameters);
        this.isStatic = isStatic;
        this.returnType = returnType;
        this.statement = statement;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitMethod(this);
    }
}
