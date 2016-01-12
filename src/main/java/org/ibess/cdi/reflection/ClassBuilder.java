package org.ibess.cdi.reflection;

import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.exceptions.ImpossibleError;
import org.ibess.cdi.internal.$CdiObject;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.internal.$Descriptor;
import org.ibess.cdi.internal.$Instantiator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * @author ibessonov
 */
// this one has to be refactored
class ClassBuilder {

    public static final String SUFFIX = "$Cdi";
    private static final String DESCRIPTOR_CLASS_NAME = $Descriptor.class.getCanonicalName();

    private final String thePackage;
    private final String className;
    private final Class<?> superClass;
    private final TypeVariable[] params;

    private final Map<String, FieldInfo> fields = new HashMap<>();
    private final Set<MethodInfo> methods = new HashSet<>();
    private final MethodInfo constructor;
    private final MethodInfo construct;

    private final Map<String, Integer> paramsIndex;

    public ClassBuilder(Class<?> superClass) {
        this.superClass = superClass;
        this.thePackage = superClass.getPackage().getName();
        this.className = superClass.getName().substring(thePackage.length() + 1) + SUFFIX;
        this.params = superClass.getTypeParameters();
        this.constructor = new MethodInfo(this.className, null);
        this.construct = new MethodInfo("$construct", void.class);
        this.methods.add(constructor);
        this.methods.add(construct);

        constructor.addParameter("$c", $Context.class);

        this.paramsIndex    = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            paramsIndex.put(params[i].getName(), i);
        }
    }

    private void requiresContextField() {
        if (!fields.containsKey("$c")) {
            FieldInfo $c = new FieldInfo("$c", $Context.class);
            fields.put("$c", $c);

            ParameterInfo param = constructor.getParameter(0);
            constructor.addStatement(new AssignmentStatement("$c", new ParameterValueExpression(param)));
        }
    }

    private void requiresDescriptorsField() {
        if (!fields.containsKey("$d")) {
            FieldInfo $d = new FieldInfo("$d", $Descriptor[].class);
            fields.put("$d", $d);

            ParameterInfo param = constructor.addParameter("$d", $Descriptor[].class);
            constructor.addStatement(new AssignmentStatement("$d", new ParameterValueExpression(param)));
        }
    }

    public MethodInfo getConstructMethod() {
        return construct;
    }

    public MethodInfo createNewMethod(String name, Type returnType) {
        MethodInfo method = new MethodInfo(name, returnType);
        methods.add(method);
        return method;
    }

    public Expression newLookupExpression(Type type) {
        return new ComplexInjectionExpression(type);
    }

    public Statement newAssignmentStatement(String fieldName, Expression value) {
        return new AssignmentStatement(fieldName, value);
    }

    public Statement newMethodCallStatement(String methodName, List<Expression> params) {
        return new MethodCallStatement(methodName, params);
    }

    public Statement newReturnStatement(Expression value) {
        return new ReturnStatement(value);
    }

    public String build() {
        StringBuilder sb = new StringBuilder(8 * 1024);

        // package + imports
        sb.append("package ").append(thePackage).append(";\n");

        // class declaration
        sb.append("@SuppressWarnings(\"unchecked\") public final class ").append(className);
        appendGenericParameters(sb, true);
        sb.append(" extends ").append(superClass.getCanonicalName());
        appendGenericParameters(sb, false);
        sb.append(" implements ").append($CdiObject.class.getCanonicalName()).append(" {\n");

        // fields
        for (FieldInfo field : fields.values()) {
            field.appendItself(sb);
        }

        // methods
        for (MethodInfo method : methods) {
            method.appendItself(sb);
        }

        // finish
        sb.append("}\n");
        return sb.toString();
    }

    public String buildInstantiator() {
        StringBuilder sb = new StringBuilder(8 * 1024);

        String className = this.className + "0";
        sb.append("package ").append(thePackage).append(";\n");

        sb.append("@SuppressWarnings(\"unchecked\") public final class ").append(className);
        sb.append(" implements ").append($Instantiator.class.getCanonicalName()).append(" {\n");

        sb.append("  public static final ").append(className).append(" INSTANCE = new ").append(className).append("();\n");
        sb.append("  @Override public Object $create(").append($Context.class.getCanonicalName()).append(" $c, ");
        sb.append(DESCRIPTOR_CLASS_NAME).append("[] $d) {\n");
        if (fields.containsKey("$d")) {
            sb.append("    return new ").append(this.className).append("($c, $d);\n");
        } else {
            sb.append("    return new ").append(this.className).append("($c);\n");
        }
        sb.append("  }\n}\n");
        return sb.toString();
    }

    private void appendGenericParameters(StringBuilder sb, boolean bound) {
        if (params.length == 0) return;

        sb.append("<");
        if (bound) {
            appendGenericParam(sb, params[0]);
            for (int i = 1; i < params.length; i++) {
                sb.append(" , ");
                appendGenericParam(sb, params[i]);
            }
        } else {
            sb.append(params[0].getName());
            for (int i = 1; i < params.length; i++) {
                sb.append(" , ").append(params[i].getName());
            }
        }
        sb.append(">");
    }

    private static void appendGenericParam(StringBuilder sb, TypeVariable type) {
        Type[] bounds = type.getBounds();
        sb.append(type.getName());
        if (bounds.length != 0) {
            sb.append(" extends ").append(bounds[0].getTypeName());
            for (int i = 1, length = bounds.length; i < length; i++) {
                sb.append(" & ").append(bounds[i].getTypeName());
            }
        }
    }

    private interface Appendable {

        void appendItself(StringBuilder sb);

        static void appendCommaSeparated(List<? extends Appendable> list, StringBuilder sb) {
            if (!list.isEmpty()) {
                list.get(0).appendItself(sb);
                for (int i = 1, size = list.size(); i < size; i++) {
                    sb.append(", ");
                    list.get(i).appendItself(sb);
                }
            }
        }
    }

    private static class FieldInfo implements Appendable {
        private String name;
        private Type type;
        private boolean isStatic = false;

        FieldInfo(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public void appendItself(StringBuilder sb) {
            sb.append("  private ");
            if (isStatic) {
                sb.append("static ");
            } else {
                sb.append("final ");
            }
            sb.append(type.getTypeName()).append(" ").append(name).append(";\n");
        }
    }

    static class MethodInfo implements Appendable {

        private final String name;
        private final Type returnType;
        private final List<ParameterInfo> params = new ArrayList<>();
        private final List<Statement> statements = new ArrayList<>();

        MethodInfo(String name, Type returnType) {
            this.name = name;
            this.returnType = returnType;
        }

        ParameterInfo addParameter(String name, Type type) {
            ParameterInfo param = new ParameterInfo(name, type);
            params.add(param);
            return param;
        }

        public ParameterInfo getParameter(int index) {
            return params.get(index);
        }

        public void addStatement(Statement statement) {
            statements.add(statement);
        }

        @Override
        public void appendItself(StringBuilder sb) {
            // declaration
            sb.append("  public ");
            if (returnType != null) {
                sb.append(returnType.getTypeName()).append(" ");
            }
            sb.append(name).append("(");
            Appendable.appendCommaSeparated(params, sb);
            sb.append(") {\n");

            // body
            for (Statement statement : statements) {
                statement.appendItself(sb);
            }

            // finish
            sb.append("  }\n");
        }
    }

    static class ParameterInfo implements Appendable {
        private final String name;
        private final Type type;

        ParameterInfo(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public void appendItself(StringBuilder sb) {
            sb.append(type.getTypeName()).append(" ").append(name);
        }
    }

    public interface Statement extends Appendable {}

    private static class AssignmentStatement implements Statement {
        private final String fieldName;
        private final Expression value;

        AssignmentStatement(String fieldName, Expression value) {
            this.fieldName = fieldName;
            this.value = value;
        }

        @Override
        public void appendItself(StringBuilder sb) {
            sb.append("    this.").append(fieldName).append(" = ");
            value.appendItself(sb);
            sb.append(";\n");
        }
    }

    private static class ReturnStatement implements Statement {
        private final Expression value;

        ReturnStatement(Expression value) {
            this.value = value;
        }

        @Override
        public void appendItself(StringBuilder sb) {
            sb.append("    return ");
            value.appendItself(sb);
            sb.append(";\n");
        }
    }

    public interface Expression extends Appendable {

        void appendItself(StringBuilder sb);
    }

    private static class ParameterValueExpression implements Expression {

        private final ParameterInfo param;

        ParameterValueExpression(ParameterInfo param) {
            this.param = param;
        }

        @Override
        public void appendItself(StringBuilder sb) {
            sb.append(param.name);
        }
    }

    private class ComplexInjectionExpression implements Expression {

        private final Type type;

        ComplexInjectionExpression(Type type) {
            if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() == Class.class) {
                requiresDescriptorsField();
            } else {
                requiresContextField();
                checkIfDescriptorsFieldRequired(type);
            }
            this.type = type;
        }

        private void checkIfDescriptorsFieldRequired(Type currentType) {
            if (currentType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) currentType;
                Class clazz = (Class) parameterizedType.getRawType();
                if (clazz.isAnnotationPresent(Scoped.class)) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    for (Type actualTypeArgument : actualTypeArguments) {
                        checkIfDescriptorsFieldRequired(actualTypeArgument);
                    }
                }
            } else if (currentType instanceof TypeVariable) {
                requiresDescriptorsField();
            }
        }

        @Override
        public void appendItself(StringBuilder sb) {
            if (type instanceof Class) {
                sb.append("(").append(((Class) type).getCanonicalName()).append(") ");
            } else if (type instanceof ParameterizedType) {

                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class clazz = (Class) parameterizedType.getRawType();
                if (clazz == Class.class) {
                    Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
                    TypeVariable actualTypeVariable = (TypeVariable) actualTypeArgument;
                    String actualTypeVariableName = actualTypeVariable.getName();

                    sb.append("(Class<").append(actualTypeVariableName).append(">) $d[");
                    sb.append(paramsIndex.get(actualTypeVariableName)).append("].c");
                    return;
                }
                sb.append("(").append(clazz.getCanonicalName()).append(") ");
            } else if (type instanceof TypeVariable) {
                sb.append("(").append(type.getTypeName()).append(") ");
            }
            sb.append("$c.$lookup(");
            appendDescriptor(sb, type);
            sb.append(")");
        }

        private void appendDescriptor(StringBuilder sb, Type currentType) {
            if (currentType instanceof TypeVariable) {
                sb.append("$d[").append(paramsIndex.get(currentType.getTypeName())).append("]");
            } else {
                sb.append(DESCRIPTOR_CLASS_NAME).append(".$");
                if (currentType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) currentType;
                    Class clazz = (Class) parameterizedType.getRawType();

                    String canonicalName = clazz.getCanonicalName();
                    if (clazz.isAnnotationPresent(Scoped.class)) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if (actualTypeArguments.length == 0) {
                            sb.append("0(").append(canonicalName).append(".class");
                        } else {
                            sb.append("(").append(canonicalName).append(".class");
                            boolean optimize = true;
                            for (int i = 0, length = actualTypeArguments.length; i < length; i++) {
                                Type actualTypeArgument = actualTypeArguments[i];
                                boolean matches = (actualTypeArgument instanceof TypeVariable)
                                        && (i == paramsIndex.get(actualTypeArgument.getTypeName()));
                                if (!matches) {
                                    optimize = false;
                                    break;
                                }
                            }

                            if (optimize) {
                                sb.append(", $d");
                            } else {
                                for (Type actualTypeArgument : actualTypeArguments) {
                                    sb.append(", ");
                                    appendDescriptor(sb, actualTypeArgument);
                                }
                            }
                        }
                    } else {
                        sb.append("0(").append(canonicalName).append(".class");
                    }
                } else if (currentType instanceof Class) {
                    sb.append("0(");
                    Class clazz = (Class) currentType;
                    sb.append(clazz.getCanonicalName()).append(".class");
                } else {
                    throw new ImpossibleError();
                }
                sb.append(")");
            }
        }
    }

    private static class MethodCallStatement implements Statement {
        private String name;
        private List<Expression> params;

        MethodCallStatement(String name, List<Expression> params) {
            this.name = name;
            this.params = params;
        }

        @Override
        public void appendItself(StringBuilder sb) {
            sb.append("    ").append(name).append("(");
            Appendable.appendCommaSeparated(params, sb);
            sb.append(");\n");
        }
    }
}
