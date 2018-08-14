package com.github.ibessonov.cdi.util;

/**
 * @author ibessonov
 */
public class ClassUtil {

    public static boolean isPrimitive(Class<?> clazz) {
        String name = clazz.getName();
        if (name.length() > 7) return false;
        switch (name) {
            case "boolean":
            case "byte":
            case "char":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "void": //?
                return true;
            default:
                return false;
        }
    }
}
