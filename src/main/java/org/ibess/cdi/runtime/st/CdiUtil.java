package org.ibess.cdi.runtime.st;

import java.lang.reflect.Method;

import static org.ibess.cdi.runtime.st.Dsl.*;

/**
 * @author ibessonov
 */
public final class CdiUtil {

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
                return $invokeStaticMethod(zValueOf, $withParameters(expression));
            case "byte":
                return $invokeStaticMethod(bValueOf, $withParameters(expression));
            case "char":
                return $invokeStaticMethod(cValueOf, $withParameters(expression));
            case "short":
                return $invokeStaticMethod(sValueOf, $withParameters(expression));
            case "int":
                return $invokeStaticMethod(iValueOf, $withParameters(expression));
            case "long":
                return $invokeStaticMethod(jValueOf, $withParameters(expression));
            case "float":
                return $invokeStaticMethod(fValueOf, $withParameters(expression));
            case "double":
                return $invokeStaticMethod(dValueOf, $withParameters(expression));
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
                return $invokeVirtualMethod(zValue, $cast(Boolean  .class, expression), $withoutParameters());
            case "byte":
                return $invokeVirtualMethod(bValue, $cast(Byte     .class, expression), $withoutParameters());
            case "char":
                return $invokeVirtualMethod(cValue, $cast(Character.class, expression), $withoutParameters());
            case "short":
                return $invokeVirtualMethod(sValue, $cast(Short    .class, expression), $withoutParameters());
            case "int":
                return $invokeVirtualMethod(iValue, $cast(Integer  .class, expression), $withoutParameters());
            case "long":
                return $invokeVirtualMethod(jValue, $cast(Long     .class, expression), $withoutParameters());
            case "float":
                return $invokeVirtualMethod(fValue, $cast(Float    .class, expression), $withoutParameters());
            case "double":
                return $invokeVirtualMethod(dValue, $cast(Double   .class, expression), $withoutParameters());
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
