package org.ibess.cdi.util;

import org.ibess.cdi.exceptions.ImpossibleError;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;

/**
 * @author ibessonov
 */
public final class BoxingUtil {

    public static final MethodHandle IDENTITY = identity(Object.class); //TODO move

    private static final MethodHandle zValueOf;
    private static final MethodHandle bValueOf;
    private static final MethodHandle cValueOf;
    private static final MethodHandle sValueOf;
    private static final MethodHandle iValueOf;
    private static final MethodHandle jValueOf;
    private static final MethodHandle fValueOf;
    private static final MethodHandle dValueOf;

    private static final MethodHandle zValue;
    private static final MethodHandle bValue;
    private static final MethodHandle cValue;
    private static final MethodHandle sValue;
    private static final MethodHandle iValue;
    private static final MethodHandle jValue;
    private static final MethodHandle fValue;
    private static final MethodHandle dValue;

    static {
        try {
            MethodHandles.Lookup lookup = publicLookup();
            zValueOf = lookup.unreflect(Boolean  .class.getDeclaredMethod("valueOf", boolean.class)).asType(methodType(Object.class, boolean.class));
            bValueOf = lookup.unreflect(Byte     .class.getDeclaredMethod("valueOf", byte.class   )).asType(methodType(Object.class, byte.class   ));
            cValueOf = lookup.unreflect(Character.class.getDeclaredMethod("valueOf", char.class   )).asType(methodType(Object.class, char.class   ));
            sValueOf = lookup.unreflect(Short    .class.getDeclaredMethod("valueOf", short.class  )).asType(methodType(Object.class, short.class  ));
            iValueOf = lookup.unreflect(Integer  .class.getDeclaredMethod("valueOf", int.class    )).asType(methodType(Object.class, int.class    ));
            jValueOf = lookup.unreflect(Long     .class.getDeclaredMethod("valueOf", long.class   )).asType(methodType(Object.class, long.class   ));
            fValueOf = lookup.unreflect(Float    .class.getDeclaredMethod("valueOf", float.class  )).asType(methodType(Object.class, float.class  ));
            dValueOf = lookup.unreflect(Double   .class.getDeclaredMethod("valueOf", double.class )).asType(methodType(Object.class, double.class ));

            zValue   = filterReturnValue(IDENTITY.asType(methodType(Boolean  .class, Object.class)), lookup.unreflect(Boolean  .class.getDeclaredMethod("booleanValue")));
            bValue   = filterReturnValue(IDENTITY.asType(methodType(Byte     .class, Object.class)), lookup.unreflect(Byte     .class.getDeclaredMethod("byteValue"   )));
            cValue   = filterReturnValue(IDENTITY.asType(methodType(Character.class, Object.class)), lookup.unreflect(Character.class.getDeclaredMethod("charValue"   )));
            sValue   = filterReturnValue(IDENTITY.asType(methodType(Short    .class, Object.class)), lookup.unreflect(Short    .class.getDeclaredMethod("shortValue"  )));
            iValue   = filterReturnValue(IDENTITY.asType(methodType(Integer  .class, Object.class)), lookup.unreflect(Integer  .class.getDeclaredMethod("intValue"    )));
            jValue   = filterReturnValue(IDENTITY.asType(methodType(Long     .class, Object.class)), lookup.unreflect(Long     .class.getDeclaredMethod("longValue"   )));
            fValue   = filterReturnValue(IDENTITY.asType(methodType(Float    .class, Object.class)), lookup.unreflect(Float    .class.getDeclaredMethod("floatValue"  )));
            dValue   = filterReturnValue(IDENTITY.asType(methodType(Double   .class, Object.class)), lookup.unreflect(Double   .class.getDeclaredMethod("doubleValue" )));
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

    public static MethodHandle boxHandle(Class<?> primitive) {
        switch (primitive.getName()) {
            case "boolean": return zValueOf;
            case "byte":    return bValueOf;
            case "char":    return cValueOf;
            case "short":   return sValueOf;
            case "int":     return iValueOf;
            case "long":    return jValueOf;
            case "float":   return fValueOf;
            case "double":  return dValueOf;
            default: throw new ImpossibleError("Unknown primitive type: " + primitive);
        }
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

    public static MethodHandle unboxHandle(Class<?> primitive) {
        switch (primitive.getName()) {
            case "boolean": return zValue;
            case "byte":    return bValue;
            case "char":    return cValue;
            case "short":   return sValue;
            case "int":     return iValue;
            case "long":    return jValue;
            case "float":   return fValue;
            case "double":  return dValue;
            default: throw new ImpossibleError("Unknown primitive type: " + primitive);
        }
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
