package org.ibess.cdi.runtime.st;

import java.lang.reflect.Field;

/**
 * @author ibessonov
 */
public class StFieldAssignmentStatement implements StStatement {

    public final StExpression left;
    public final String declaringClassName;
    public final String fieldClassName;
    public final String fieldName;
    public final StExpression right;
    public final boolean isStatic;

    public StFieldAssignmentStatement(StExpression left, String declaringClassName, Class<?> fieldClass, String fieldName, StExpression right) {
        this.left = left;
        this.declaringClassName = declaringClassName;
        this.fieldClassName = fieldClass.getName();
        this.fieldName = fieldName;
        this.right = right;
        this.isStatic = false;
    }

    public StFieldAssignmentStatement(StExpression left, Field field, StExpression right) {
        this.left = left;
        this.declaringClassName = field.getDeclaringClass().getName();
        this.fieldClassName = field.getType().getName();
        this.fieldName = field.getName();
        this.right = right;
        this.isStatic = false;
    }

    public StFieldAssignmentStatement(boolean isStatic, StExpression left, String declaringClassName, Class<?> fieldClass, String fieldName, StExpression right) {
        this.isStatic = isStatic;
        this.left = left;
        this.declaringClassName = declaringClassName;
        this.fieldClassName = fieldClass.getName();
        this.fieldName = fieldName;
        this.right = right;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitFieldAssignmentStatement(this);
    }
}
