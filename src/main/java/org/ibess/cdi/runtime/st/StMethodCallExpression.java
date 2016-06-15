package org.ibess.cdi.runtime.st;

import java.lang.reflect.Method;

/**
 * @author ibessonov
 */
public class StMethodCallExpression implements StExpression {

    public final StExpression object;
    public final String name;
    public final String declaringClassName;
    public final Class<?> returnType;
    public final Class<?>[] paramTypes;
    public final StExpression[] parameters;
    public final InvokeType invokeType;

    public StMethodCallExpression(StExpression object, Method method, StExpression[] parameters, InvokeType invokeType) {
        this.object = object;
        this.name = method.getName();
        this.declaringClassName = method.getDeclaringClass().getName();
        this.returnType = method.getReturnType();
        this.paramTypes = method.getParameterTypes();
        this.parameters = parameters.clone();
        this.invokeType = invokeType;
    }

    public StMethodCallExpression(StExpression object, String name, String declaringClassName, Class<?> returnType,
                                  Class<?>[] paramTypes, StExpression[] parameters, InvokeType invokeType) {
        this.object = object;
        this.name = name;
        this.declaringClassName = declaringClassName;
        this.returnType = returnType;
        this.paramTypes = paramTypes.clone();
        this.parameters = parameters.clone();
        this.invokeType = invokeType;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitMethodCallExpression(this);
    }
}
