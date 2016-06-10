package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StGetFieldExpression implements StExpression {

    public final StExpression left;
    public final String declaringClassName;
    public final Class<?> fieldClass;
    public final String fieldName;

    public StGetFieldExpression(StExpression left, String declaringClassName, Class<?> fieldClass, String fieldName) {
        this.left = left;
        this.declaringClassName = declaringClassName;
        this.fieldClass = fieldClass;
        this.fieldName = fieldName;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitGetFieldExpression(this);
    }
}
