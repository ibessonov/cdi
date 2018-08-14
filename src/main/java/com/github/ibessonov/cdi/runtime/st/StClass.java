package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StClass implements StNode {

    public final Class<?> superClass;
    public final Class<?>[] interfaces;
    public final String name;
    public final StField[] fields;
    public final StMethod[] methods;

    public StClass(Class<?> superClass, Class<?>[] interfaces, String name, StField[] fields, StMethod[] methods) {
        this.superClass = superClass;
        this.interfaces = interfaces.clone();
        this.name = name;
        this.fields = fields.clone();
        this.methods = methods.clone();
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitClass(this);
    }
}
