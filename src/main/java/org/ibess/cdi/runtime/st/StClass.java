package org.ibess.cdi.runtime.st;

import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * @author ibessonov
 */
public class StClass implements StNode {

    public final Class<?> superClass;
    public final List<Class<?>> interfaces;
    public final String name;
    public final List<StField> fields;
    public final List<StMethod> methods;

    public <L extends List<Class<?>> & RandomAccess,
            F extends List<StField> & RandomAccess,
            M extends List<StMethod> & RandomAccess>
            StClass(Class<?> superClass, L interfaces, String name, F fields, M methods) {
        this.superClass = superClass;
        this.interfaces = Collections.unmodifiableList(interfaces);
        this.name = name;
        this.fields = Collections.unmodifiableList(fields);
        this.methods = Collections.unmodifiableList(methods);
    }


    @Override
    public void accept(StVisitor visitor) {
        visitor.visitClass(this);
    }
}
