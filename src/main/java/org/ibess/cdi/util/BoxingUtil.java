package org.ibess.cdi.util;

import org.ibess.cdi.runtime.st.StExpression;

import java.lang.reflect.Method;

import static org.ibess.cdi.runtime.st.Dsl.*;

/**
 * @author ibessonov
 */
public final class BoxingUtil {

    private static final Method zValueOf;
    private static final Method bValueOf;
    private static final Method cValueOf;
    private static final Method sValueOf;
    private static final Method iValueOf;
    private static final Method jValueOf;
    private static final Method fValueOf;
    private static final Method dValueOf;

    private static final Method zValue;
    private static final Method bValue;
    private static final Method cValue;
    private static final Method sValue;
    private static final Method iValue;
    private static final Method jValue;
    private static final Method fValue;
    private static final Method dValue;

    static {
        try {
            zValueOf = Boolean  .class.getDeclaredMethod("valueOf", boolean.class);
            bValueOf = Byte     .class.getDeclaredMethod("valueOf", byte.class);
            cValueOf = Character.class.getDeclaredMethod("valueOf", char.class);
            sValueOf = Short    .class.getDeclaredMethod("valueOf", short.class);
            iValueOf = Integer  .class.getDeclaredMethod("valueOf", int.class);
            jValueOf = Long     .class.getDeclaredMethod("valueOf", long.class);
            fValueOf = Float    .class.getDeclaredMethod("valueOf", float.class);
            dValueOf = Double   .class.getDeclaredMethod("valueOf", double.class);

            zValue   = Boolean  .class.getDeclaredMethod("booleanValue");
            bValue   = Byte     .class.getDeclaredMethod("byteValue");
            cValue   = Character.class.getDeclaredMethod("charValue");
            sValue   = Short    .class.getDeclaredMethod("shortValue");
            iValue   = Integer  .class.getDeclaredMethod("intValue");
            jValue   = Long     .class.getDeclaredMethod("longValue");
            fValue   = Float    .class.getDeclaredMethod("floatValue");
            dValue   = Double   .class.getDeclaredMethod("doubleValue");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static StExpression box(Class<?> primitive, StExpression expression) {
        switch (primitive.getName()) {
            case "boolean":
                return _invokeStaticMethod(zValueOf, _withParameters(expression));
            case "byte":
                return _invokeStaticMethod(bValueOf, _withParameters(expression));
            case "char":
                return _invokeStaticMethod(cValueOf, _withParameters(expression));
            case "short":
                return _invokeStaticMethod(sValueOf, _withParameters(expression));
            case "int":
                return _invokeStaticMethod(iValueOf, _withParameters(expression));
            case "long":
                return _invokeStaticMethod(jValueOf, _withParameters(expression));
            case "float":
                return _invokeStaticMethod(fValueOf, _withParameters(expression));
            case "double":
                return _invokeStaticMethod(dValueOf, _withParameters(expression));
        }
        return expression;
    }

    public static Class<?> box(Class<?> primitive) {
        switch (primitive.getName()) {
            case "boolean": return Boolean.class;
            case "byte"   : return Byte.class;
            case "char"   : return Character.class;
            case "short"  : return Short.class;
            case "int"    : return Integer.class;
            case "long"   : return Long.class;
            case "float"  : return Float.class;
            case "double" : return Double.class;
            case "void"   : return Void.class;
        }
        return primitive;
    }

    public static StExpression unbox(Class<?> primitive, StExpression expression) {
        switch (primitive.getName()) {
            case "boolean":
                return _invokeVirtualMethod(zValue, _cast(Boolean  .class, expression), _withoutParameters());
            case "byte":
                return _invokeVirtualMethod(bValue, _cast(Byte     .class, expression), _withoutParameters());
            case "char":
                return _invokeVirtualMethod(cValue, _cast(Character.class, expression), _withoutParameters());
            case "short":
                return _invokeVirtualMethod(sValue, _cast(Short    .class, expression), _withoutParameters());
            case "int":
                return _invokeVirtualMethod(iValue, _cast(Integer  .class, expression), _withoutParameters());
            case "long":
                return _invokeVirtualMethod(jValue, _cast(Long     .class, expression), _withoutParameters());
            case "float":
                return _invokeVirtualMethod(fValue, _cast(Float    .class, expression), _withoutParameters());
            case "double":
                return _invokeVirtualMethod(dValue, _cast(Double   .class, expression), _withoutParameters());
        }
        return expression;
    }

    public static Class<?> unbox(Class<?> boxed) {
        switch (boxed.getName()) {
            case "java.lang.Boolean"  : return boolean.class;
            case "java.lang.Byte"     : return byte.class;
            case "java.lang.Character": return char.class;
            case "java.lang.Short"    : return short.class;
            case "java.lang.Integer"  : return int.class;
            case "java.lang.Long"     : return long.class;
            case "java.lang.Float"    : return float.class;
            case "java.lang.Double"   : return double.class;
            case "java.lang.Void"     : return void.class;
        }
        return boxed;
    }
}
