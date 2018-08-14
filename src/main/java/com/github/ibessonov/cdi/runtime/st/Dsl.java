package com.github.ibessonov.cdi.runtime.st;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static com.github.ibessonov.cdi.util.BoxingUtil.box;
import static com.github.ibessonov.cdi.util.ClassUtil.isPrimitive;

/**
 * @author ibessonov
 */
public class Dsl {

    public static final StThisExpression _this = new StThisExpression();
    public static final StSwapExpression _swap = new StSwapExpression();
    public static final StNullExpression _null = new StNullExpression();
    public static final StDupExpression _dup = new StDupExpression();

    /*
     ********************************************************************************
     *                              CLASS DECLARATIONS                              *
     ********************************************************************************
     */

    public static StClass _class(String name, Class<?> superClass, Class<?>[] interfaces,
                                 StField[] fields, StMethod[] methods) {
        return new StClass(superClass, interfaces, name, fields, methods);
    }

    public static StField[] _withFields(StField... fields) {
        return fields;
    }

    private static final StField[] EMPTY_FIELDS = {};
    public static StField[] _withoutFields() {
        return EMPTY_FIELDS;
    }

    public static StMethod[] _withMethods(StMethod... methods) {
        return methods;
    }

    public static StField _field(String name, Class<?> clazz) {
        return new StField(false, name, clazz);
    }

    public static StField _staticField(String name, Class<?> clazz) {
        return new StField(true, name, clazz);
    }

    public static StMethod _method(String name, Class<?>[] parameters, Class<?> returnType, StStatement[] body) {
        return _method0(false, name, parameters, returnType, body);
    }

    public static StMethod _overrideMethod(Method method, StStatement[] body) {
        return _method(_named(method.getName()), _withParameterTypes(method.getParameterTypes()),
            _returns(method.getReturnType()), _withBody(body)
        );
    }

    public static StMethod _staticMethod(String name, Class<?>[] parameters, Class<?> returnType, StStatement[] body) {
        return _method0(true, name, parameters, returnType, body);
    }

    private static StMethod _method0(boolean isStatic, String name, Class<?>[] parameters, Class<?> returnType, StStatement[] body) {
        return new StMethod(isStatic, name, parameters, returnType, new StScopeStatement(body));
    }

    /*
     ********************************************************************************
     *                                   UTILITIES                                  *
     ********************************************************************************
     */

    public static String _named(String name) {
        return name;
    }

    public static Class<?> _extends(Class<?> clazz) {
        return clazz;
    }

    public static Class<?>[] _implements(Class<?>... interfaces) {
        return interfaces;
    }

    public static Class<?>[] _withParameterTypes(Class<?>... types) {
        return types;
    }

    private static final Class<?>[] EMPTY_CLASSES = {};
    public static Class<?>[] _withoutParameterTypes() {
        return EMPTY_CLASSES;
    }

    public static Class<?> _returns(Class<?> returnType) {
        return returnType;
    }

    public static Class<?> _returnsNothing() {
        return _returns(void.class);
    }

    public static StStatement[] _withBody(StStatement... statements) {
        return statements;
    }

    public static StField[] _fields(List<StField> fields) {
        return fields.toArray(new StField[0]);
    }

    public static StMethod[] _methods(List<StMethod> methods) {
        return methods.toArray(new StMethod[0]);
    }

    private static final StStatement[] EMPTY_STATEMENTS = {};
    public static StStatement[] _statements(List<StStatement> statements) {
        return statements.toArray(EMPTY_STATEMENTS);
    }

    private static final StExpression[] EMPTY_EXPRESSIONS = {};
    public static StExpression[] _expressions(List<StExpression> expressions) {
        return expressions.toArray(EMPTY_EXPRESSIONS);
    }

    public static Class<?>[] _types(List<Class<?>> expressions) {
        return expressions.toArray(EMPTY_CLASSES);
    }


    public static String _ofClass(Class<?> clazz) {
        return clazz.getName();
    }

    public static String _ofClass(String name) {
        return name;
    }

    public static StExpression _on(StExpression expression) {
        return expression;
    }

    public static StExpression _of(StExpression expression) {
        return expression;
    }

    public static StExpression _value(StExpression expression) {
        return expression;
    }

    public static StExpression[] _withParameters(StExpression... parameters) {
        return parameters;
    }

    public static StExpression[] _withoutParameters() {
        return EMPTY_EXPRESSIONS;
    }

    public static Class<?> _toClass(Class<?> clazz) {
        return clazz;
    }

    public static Class<?> _withType(Class<?> clazz) {
        return clazz;
    }

    public static StStatement _then(StStatement statement) {
        return statement;
    }

    public static StStatement _else(StStatement statement) {
        return statement;
    }

    /*
     ********************************************************************************
     *                                  METHOD BODY                                 *
     ********************************************************************************
     */

    public static StStatement _scope(StStatement... statements) {
        return new StScopeStatement(statements);
    }

    public static StStatement _return(boolean flag) {
        return _return(_bool(flag));
    }

    public static StStatement _return(StExpression expression) {
        return new StReturnStatement(expression);
    }

    public static StStatement _statement(StExpression expression) {
        return new StMethodCallStatement(expression);
    }

    public static StStatement _assign(Field field, StExpression left, StExpression right) {
        return new StFieldAssignmentStatement(left, field, right);
    }

    public static StStatement _assignMyField(String name, Class<?> type, StExpression value) {
        return new StFieldAssignmentStatement(_this, false, null, type, name, value);
    }

    public static StStatement _assign(String name, String declaringClassName, Class<?> fieldClass, StExpression value) {
        return new StFieldAssignmentStatement(null, false, declaringClassName, fieldClass, name, value);
    }

    public static StStatement _assignStatic(String name, String declaringClassName, Class<?> fieldClass, StExpression value) {
        return new StFieldAssignmentStatement(null, true, declaringClassName, fieldClass, name, value);
    }

    public static StStatement _noop() {
        return new StNoopStatement();
    }


    public static StStatement _assignMethodParam(int index, StExpression expression) {
        return new StParamAssignmentStatement(index, expression);
    }

    public static StStatement _if(StExpression condition, StStatement then, StStatement els) {
        return new StIfStatement(false, false, condition, then, els);
    }

    public static StStatement _if(StExpression condition, StStatement statement) {
        return _if(condition, statement, null);
    }

    public static StStatement _ifNot(StExpression condition, StStatement then, StStatement els) {
        return new StIfStatement(false, true, condition, then, els);
    }

    public static StStatement _ifNot(StExpression condition, StStatement statement) {
        return _ifNot(condition, statement, null);
    }

    public static StStatement _ifNull(StExpression expression, StStatement then, StStatement els) {
        return new StIfStatement(true, false, expression, then, els);
    }

    public static StStatement _ifNotNull(StExpression expression, StStatement then, StStatement els) {
        return new StIfStatement(true, true, expression, then, els);
    }

    /*
     ********************************************************************************
     *                                 EXPRESSIONS                                  *
     ********************************************************************************
     */

    /*
     ********************************************************************************
     *                                 INVOCATIONS                                  *
     ********************************************************************************
     */

    public static StExpression _invokeSpecialMethod(String declaringClassName, String name,
            Class<?>[] parameterTypes, Class<?> returnType, StExpression left, StExpression[] parameters) {
        return _invokeMethod(InvokeType.SPECIAL, declaringClassName, name, parameterTypes, returnType, left, parameters);
    }

    public static StExpression _invokeSpecialMethod(Method method, StExpression left, StExpression[] parameters) {
        return _invokeMethod(InvokeType.SPECIAL, method, left, parameters);
    }

    public static StExpression _invokeVirtualMethod(String declaringClassName, String name,
            Class<?>[] parameterTypes, Class<?> returnType, StExpression left, StExpression[] parameters) {
        return _invokeMethod(InvokeType.VIRTUAL, declaringClassName, name, parameterTypes, returnType, left, parameters);
    }

    public static StExpression _invokeVirtualMethod(Method method, StExpression left, StExpression[] parameters) {
        return _invokeMethod(InvokeType.VIRTUAL, method, left, parameters);
    }

    public static StExpression _invokeInterfaceMethod(Method method, StExpression left, StExpression[] parameters) {
        return _invokeMethod(InvokeType.INTERFACE, method, left, parameters);
    }

    public static StExpression _invokeStaticMethod(String declaringClassName, String name,
            Class<?>[] parameterTypes, Class<?> returnType, StExpression[] parameters) {
        return _invokeMethod(InvokeType.STATIC, declaringClassName, name, parameterTypes, returnType, null, parameters);
    }

    public static StExpression _invokeStaticMethod(Method method, StExpression[] parameters) {
        return _invokeMethod(InvokeType.STATIC, method, null, parameters);
    }

    private static StExpression _invokeMethod(InvokeType invokeType, String declaringClassName, String name,
            Class<?>[] parameterTypes, Class<?> returnType, StExpression left, StExpression[] parameters) {
        return new StMethodCallExpression(left, name, declaringClassName, returnType, parameterTypes, parameters, invokeType);
    }

    private static StExpression _invokeMethod(InvokeType invokeType, Method method, StExpression left, StExpression[] parameters) {
        return new StMethodCallExpression(left, method, parameters, invokeType);
    }

    public static StExpression _cast(Class<?> clazz, StExpression expression) {
        return new StCastExpression(clazz, expression);
    }

    public static StExpression _invokeDynamic(String methodName, Class<?>[] parameterTypes, Class<?> returnType,
                                              String metafactoryMethodName, Class<?> metafactory,
                                              StExpression[] parameters, Object... args) {
        return new StInvokeDynamicExpression(methodName, parameterTypes, returnType,
                metafactoryMethodName, metafactory, parameters, args);
    }

    /*
     ********************************************************************************
     *                             OBJECTS REFERENCES                               *
     ********************************************************************************
     */

    public static StExpression _this() {
        return _this;
    }

    public static StExpression _methodParam(int index) {
        return new StGetParameterExpression(index);
    }

    public static StExpression _dup() {
        return _dup;
    }

    public static StExpression _swap() {
        return _swap;
    }

    public static StExpression _null() {
        return _null;
    }

    public static StExpression _class(Class<?> clazz) {
        if (isPrimitive(clazz)) {
            return _getStaticField(box(clazz).getName(), _named("TYPE"), _withType(Class.class));
        }
        return new StClassExpression(clazz);
    }

    public static StExpression _myField(String name) {
        return new StGetFieldExpression(_this, false, name);
    }

    public static StExpression _myStaticField(String name) {
        return new StGetFieldExpression(_this, true, name);
    }

    public static StExpression _getField(String declaringClassName, String name, Class<?> type, StExpression expression) {
        return new StGetFieldExpression(expression, false, declaringClassName, type, name);
    }

    public static StExpression _getStaticField(String declaringClassName, String name, Class<?> type) {
        return new StGetFieldExpression(null, true, declaringClassName, type, name);
    }

    /*
     ********************************************************************************
     *                                CONSTRUCTORS                                  *
     ********************************************************************************
     */

    public static StExpression _new(Class<?> clazz) {
        return new StNewExpression(clazz.getName());
    }

    public static StExpression _new(String className) {
        return new StNewExpression(className);
    }

    /*
     ********************************************************************************
     *                                  CONSTANTS                                   *
     ********************************************************************************
     */

    public static StExpression _bool(boolean flag) {
        return _int(flag ? 1 : 0);
    }

    public static StExpression _bool(Boolean flag) {
        return _bool(flag.booleanValue());
    }

    public static StExpression _byte(byte value) {
        return _int(value, byte.class);
    }

    public static StExpression _byte(Byte value) {
        return _byte(value.byteValue());
    }

    public static StExpression _char(char character) {
        return _int(0x0000FFFF & (int) character);
    }

    public static StExpression _char(Character character) {
        return _char(character.charValue());
    }

    public static StExpression _short(short value) {
        return _int(value, short.class);
    }

    public static StExpression _short(Short value) {
        return _short(value.shortValue());
    }

    public static StExpression _int(int value) {
        return _int(value, int.class);
    }

    public static StExpression _long(long value) {
        return _long(Long.valueOf(value));
    }

    public static StExpression _long(Long value) {
        return _const0(value);
    }

    public static StExpression _float(float value) {
        return _float(Float.valueOf(value));
    }

    public static StExpression _float(Float value) {
        return _const0(value);
    }

    public static StExpression _double(double value) {
        return _double(Double.valueOf(value));
    }

    public static StExpression _double(Double value) {
        return _const0(value);
    }

    public static StExpression _string(String value) {
        return _const0(value);
    }

    private static StExpression _int(int value, Class<?> clazz) {
        return new StIntConstantExpression(value, clazz);
    }

    private static StExpression _const0(Object value) {
        return new StConstant(value);
    }

    /*
     ********************************************************************************
     *                                   ARRAYS                                     *
     ********************************************************************************
     */

    public static StExpression _arrayElement(StExpression array, StExpression index) {
        return new StArrayElementExpression(array, index);
    }

    public static StExpression _withIndex(StExpression expression) {
        return expression;
    }

    public static StExpression _withIndex(int index) {
        return _int(index);
    }

    public static StExpression _array(Class<?> type, StExpression[] elements) {
        return new StArrayExpression(type, elements);
    }
}
