package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StGetFieldExpression implements StExpression {

    public final StExpression left;
    public final FieldInfo field;

    public StGetFieldExpression(StExpression left, boolean isStatic, String declaringClassName, Class<?> fieldClass, String fieldName) {
        this.left = left;
        this.field = new FieldInfo(isStatic, declaringClassName, fieldClass.getName(), fieldName);
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitGetFieldExpression(this);
    }
}
