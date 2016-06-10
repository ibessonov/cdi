package org.ibess.cdi.runtime.st;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * @author ibessonov
 */
public class StMethodCallExpression implements StExpression {

    public final StExpression object;
    public final String name;
    public final String declaringClassName;
    public final Class<?> returnType;
    public final List<Class<?>> paramTypes;
    public final List<StExpression> parameters;
    public final InvokeType invokeType;

    public <L extends List<StExpression> & RandomAccess> StMethodCallExpression(StExpression object, Method method, L parameters, InvokeType invokeType) {
        this.object = object;
        this.name = method.getName();
        this.declaringClassName = method.getDeclaringClass().getName();
        this.returnType = method.getReturnType();
        this.paramTypes = Arrays.asList(method.getParameterTypes());
        this.parameters = Collections.unmodifiableList(parameters);
        this.invokeType = invokeType;
    }

    public <L extends List<StExpression> & RandomAccess,
            C extends List<Class<?>> & RandomAccess>
            StMethodCallExpression(StExpression object, String name, String declaringClassName, Class<?> returnType,
                                   C paramTypes, L parameters, InvokeType invokeType) {
        this.object = object;
        this.name = name;
        this.declaringClassName = declaringClassName;
        this.returnType = returnType;
        this.paramTypes = paramTypes;
        this.parameters = Collections.unmodifiableList(parameters);
        this.invokeType = invokeType;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitMethodCallExpression(this);
    }
}
