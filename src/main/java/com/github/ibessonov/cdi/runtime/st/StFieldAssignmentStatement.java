package com.github.ibessonov.cdi.runtime.st;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author ibessonov
 */
public class StFieldAssignmentStatement implements StStatement {

    public final StExpression left;
    public final FieldInfo field;
    public final StExpression right;

    public StFieldAssignmentStatement(StExpression left, Field field, StExpression right) {
        this(left, Modifier.isStatic(field.getModifiers()), field.getDeclaringClass().getName(), field.getType(), field.getName(), right);
    }

    public StFieldAssignmentStatement(StExpression left, boolean isStatic, String declaringClassName, Class<?> fieldClass, String fieldName, StExpression right) {
        this.left = left;
        this.field = new FieldInfo(isStatic, declaringClassName, fieldClass.getName(), fieldName);
        this.right = right;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitFieldAssignmentStatement(this);
    }
}
