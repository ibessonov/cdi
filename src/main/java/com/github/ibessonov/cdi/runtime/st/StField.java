package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StField implements StNode {

    public final boolean isStatic;
    public final String name;
    public final Class<?> type;

    public StField(boolean isStatic, String name, Class<?> type) {
        this.isStatic = isStatic;
        this.name = name;
        this.type = type;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitField(this);
    }
}
