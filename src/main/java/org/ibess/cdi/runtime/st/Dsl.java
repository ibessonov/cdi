package org.ibess.cdi.runtime.st;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author ibessonov
 */
public class Dsl {

    public static final StThisExpression $this = new StThisExpression();
    public static final StSwapExpression $swap = new StSwapExpression();
    public static final StNullExpression $null = new StNullExpression();
    public static final StDupExpression $dup = new StDupExpression();

    public static StClass $class(String name, Class<?> superClass, Class<?>[] interfaces,
                                 StField[] fields, StMethod[] methods) {
        return new StClass(superClass, interfaces, name, fields, methods);
    }

    public static String $named(String name) {
        return name;
    }

    public static Class<?> $extends(Class<?> clazz) {
        return clazz;
    }

    public static Class<?>[] $implements(Class<?>... interfaces) {
        return interfaces;
    }

    public static StField[] $withFields(StField... fields) {
        return fields;
    }

    public static StField[] $withoutFields() {
        return $withFields();
    }

    public static StMethod[] $withMethods(StMethod... methods) {
        return methods;
    }

    public static StField $field(String name, Class<?> clazz) {
        return new StField(false, name, clazz);
    }

    public static StField $staticField(String name, Class<?> clazz) {
        return new StField(true, name, clazz);
    }

    public static StMethod $method(String name, Class<?>[] parameters, Class<?> returnType, StStatement[] body) {
        return $method0(false, name, parameters, returnType, body);
    }

    public static StMethod $staticMethod(String name, Class<?>[] parameters, Class<?> returnType, StStatement[] body) {
        return $method0(true, name, parameters, returnType, body);
    }

    private static StMethod $method0(boolean isStatic, String name, Class<?>[] parameters, Class<?> returnType, StStatement[] body) {
        return new StMethod(isStatic, name, parameters, returnType, new StScopeStatement(body));
    }

    public static Class<?>[] $withParameterTypes(Class<?>... types) {
        return types;
    }

    public static Class<?>[] $withoutParameterTypes() {
        return $withParameterTypes();
    }

    public static Class<?> $returns(Class<?> returnType) {
        return returnType;
    }

    public static Class<?> $returnsNothing() {
        return $returns(void.class);
    }

    public static StStatement[] $withBody(StStatement... statements) {
        return statements;
    }

    public static StStatement $scope(StStatement... statements) {
        return new StScopeStatement(statements);
    }

    public static StStatement $return(StExpression expression) {
        return new StReturnStatement(expression);
    }

    public static StStatement $invoke(StMethodCallExpression expression) {
        return new StMethodCallStatement(expression);
    }

    public static StStatement $assign(Field field, StExpression left, StExpression right) {
        return new StFieldAssignmentStatement(left, field, right);
    }

    public static StStatement $assignMyField(String name, Class<?> type, StExpression value) {
        return new StFieldAssignmentStatement($this, false, null, type, name, value);
    }

    public static StStatement $assign(String name, String declaringClassName, Class<?> fieldClass, StExpression value) {
        return new StFieldAssignmentStatement(null, false, declaringClassName, fieldClass, name, value);
    }

    public static StStatement $assignStatic(String name, String declaringClassName, Class<?> fieldClass, StExpression value) {
        return new StFieldAssignmentStatement(null, true, declaringClassName, fieldClass, name, value);
    }

    public static StStatement $noop() {
        return new StNoopStatement();
    }


    public static StStatement $assignMethodParam(int index, StExpression expression) {
        return new StParamAssignmentStatement(index, expression);
    }

    public static StStatement $returnHook(StStatement statement, StStatement hook) {
        return new StReturnHookStatement(statement, hook);
    }

    public static StMethodCallExpression $invokeSpecialMethod(String declaringClassName, String name,
            Class<?>[] parameterTypes, Class<?> returnType, StExpression left, StExpression[] parameters) {
        return $invokeMethod(InvokeType.SPECIAL, declaringClassName, name, parameterTypes, returnType, left, parameters);
    }

    public static StMethodCallExpression $invokeSpecialMethod(Method method, StExpression left, StExpression[] parameters) {
        return $invokeMethod(InvokeType.SPECIAL, method, left, parameters);
    }

    public static StMethodCallExpression $invokeVirtualMethod(String declaringClassName, String name,
            Class<?>[] parameterTypes, Class<?> returnType, StExpression left, StExpression[] parameters) {
        return $invokeMethod(InvokeType.VIRTUAL, declaringClassName, name, parameterTypes, returnType, left, parameters);
    }

    public static StMethodCallExpression $invokeVirtualMethod(Method method, StExpression left, StExpression[] parameters) {
        return $invokeMethod(InvokeType.VIRTUAL, method, left, parameters);
    }

    public static StMethodCallExpression $invokeInterfaceMethod(Method method, StExpression left, StExpression[] parameters) {
        return $invokeMethod(InvokeType.INTERFACE, method, left, parameters);
    }

    public static StMethodCallExpression $invokeStaticMethod(String declaringClassName, String name,
              Class<?>[] parameterTypes, Class<?> returnType, StExpression[] parameters) {
        return $invokeMethod(InvokeType.STATIC, declaringClassName, name, parameterTypes, returnType, null, parameters);
    }

    public static StMethodCallExpression $invokeStaticMethod(Method method, StExpression[] parameters) {
        return $invokeMethod(InvokeType.STATIC, method, null, parameters);
    }

    private static StMethodCallExpression $invokeMethod(InvokeType invokeType, String declaringClassName, String name,
            Class<?>[] parameterTypes, Class<?> returnType, StExpression left, StExpression[] parameters) {
        return new StMethodCallExpression(left, name, declaringClassName, returnType, parameterTypes, parameters, invokeType);
    }

    private static StMethodCallExpression $invokeMethod(InvokeType invokeType, Method method, StExpression left, StExpression[] parameters) {
        return new StMethodCallExpression(left, method, parameters, invokeType);
    }

    public static String $ofClass(Class<?> clazz) {
        return clazz.getName();
    }

    public static String $ofClass(String name) {
        return name;
    }

    public static StExpression $on(StExpression expression) {
        return expression;
    }

    public static StExpression $of(StExpression expression) {
        return expression;
    }

    public static StExpression $value(StExpression expression) {
        return expression;
    }

    public static StExpression[] $withParameters(StExpression... parameters) {
        return parameters;
    }

    public static StExpression[] $withoutParameters() {
        return $withParameters();
    }

    public static StExpression $this() {
        return $this;
    }

    public static StExpression $methodParam(int index) {
        return new StGetParameterExpression(index);
    }

    public static StExpression $new(Class<?> clazz) {
        return new StNewExpression(clazz.getName());
    }

    public static StExpression $new(String className) {
        return new StNewExpression(className);
    }

    public static StExpression $dup() {
        return $dup;
    }

    public static StExpression $cast(Class<?> clazz, StExpression expression) {
        return new StCastExpression(clazz, expression);
    }

    public static StExpression $swap() {
        return $swap;
    }

    public static StExpression $null() {
        return $null;
    }

    public static Class<?> $toClass(Class<?> clazz) {
        return clazz;
    }

    public static StExpression $class(Class<?> clazz) {
        return new StClassExpression(clazz);
    }

    public static StExpression $myField(String name, Class<?> type) {
        return $getField(null, name, type, $this);
    }

    public static StExpression $myStaticField(String name, Class<?> type) {
        return new StGetFieldExpression($this, true, null, type, name);
    }

    public static StExpression $getField(String declaringClassName, String name, Class<?> type, StExpression expression) {
        return new StGetFieldExpression(expression, false, declaringClassName, type, name);
    }

    public static Class<?> $withType(Class<?> clazz) {
        return clazz;
    }

    public static StExpression $int(int index) {
        return new StIntConstantExpression(index);
    }

    public static StExpression $arrayElement(StExpression array, StExpression index) {
        return new StArrayElementExpression(array, index);
    }

    public static StExpression $withIndex(StExpression expression) {
        return expression;
    }

    public static StExpression $array(Class<?> type, StExpression[] elements) {
        return new StArrayExpression(type, elements);
    }

    public static StField[] $fields(List<StField> fields) {
        return fields.toArray(new StField[0]);
    }

    public static StMethod[] $methods(List<StMethod> methods) {
        return methods.toArray(new StMethod[0]);
    }

    public static StStatement[] $statements(List<StStatement> statements) {
        return statements.toArray(new StStatement[0]);
    }

    public static StExpression[] $expressions(List<StExpression> expressions) {
        return expressions.toArray(new StExpression[0]);
    }
}
