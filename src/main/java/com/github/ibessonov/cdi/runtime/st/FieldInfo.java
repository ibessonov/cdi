package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class FieldInfo {

    public final boolean isStatic;
    public final String declaringClassName;
    public final String className;
    public final String name;

    public FieldInfo(boolean isStatic, String declaringClassName, String className, String name) {
        this.isStatic = isStatic;
        this.declaringClassName = declaringClassName;
        this.className = className;
        this.name = name;
    }
}
