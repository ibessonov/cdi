package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StInvokeDynamicExpression implements StTypedExpression {

    public final String methodName;
    public final Class<?>[] paramTypes;
    public final Class<?> returnType;
    public final String metafactoryMethod;
    public final Class<?> metafactory;
    public final StExpression[] parameters;
    public final Object[] args;

    public StInvokeDynamicExpression(String methodName, Class<?>[] paramTypes, Class<?> returnType,
                                     String metafactoryMethod, Class<?> metafactory, StExpression[] parameters, Object[] args) {
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.metafactoryMethod = metafactoryMethod;
        this.metafactory = metafactory;
        this.parameters = parameters;
        this.args = args;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitInvokeDynamicExpression(this);
    }

    @Override
    public Class<?> getType() {
        return returnType;
    }
}
