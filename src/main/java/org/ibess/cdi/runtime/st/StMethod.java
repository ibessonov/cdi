package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StMethod implements StNode {

    public final boolean isStatic;
    public final String name;
    public final Class<?>[] parameters;
    public final Class<?> returnType;
    public final StStatement statement;

    public StMethod(boolean isStatic, String name, Class<?>[] parameters, Class<?> returnType, StStatement statement) {
        this.name = name;
        this.parameters = parameters.clone();
        this.isStatic = isStatic;
        this.returnType = returnType;
        this.statement = statement;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitMethod(this);
    }
}
