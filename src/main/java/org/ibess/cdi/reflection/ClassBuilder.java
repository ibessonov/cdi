package org.ibess.cdi.reflection;

import org.ibess.cdi.Context;
import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.exceptions.ImpossibleError;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.internal.$Descriptor;
import org.ibess.cdi.javac.JavaC;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author ibessonov
 */
// this one has to be refactored
public class ClassBuilder {

    public static final String SUFFIX = "$Cdi";
    private static final String DESCRIPTOR_CLASS_NAME = $Descriptor.class.getCanonicalName();

    private final String thePackage;
    private final String className;
    private final Class<?> superClass;
    private final TypeVariable[] params;

    private final Map<String, FieldInfo> fields = new HashMap<>();
    private final Set<MethodInfo> methods = new HashSet<>();
    private final MethodInfo construct;

    private final Map<String, Integer> paramsIndex;
    private boolean hasContext = false;
    private boolean hasDescriptors = false;

    private final String internalName;
    private final String internalNameCdi;
    private final String internalNameCdiI;

    public ClassBuilder(Class<?> superClass) {
        this.superClass = superClass;
        this.thePackage = superClass.getPackage().getName();
        this.className = superClass.getName().substring(thePackage.length() + 1) + SUFFIX;
        this.params = superClass.getTypeParameters();
        this.construct = new MethodInfo("$construct", void.class);
        this.methods.add(construct);

        this.paramsIndex    = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            paramsIndex.put(params[i].getName(), i);
        }

        this.internalName     = superClass.getName().replace('.', '/');
        this.internalNameCdi  = internalName + "$Cdi";
        this.internalNameCdiI = internalName + "$Cdi$$I";
    }

    private void requiresContextField() {
        this.hasContext = true;
        if (!fields.containsKey("$c")) {
            FieldInfo $c = new FieldInfo("$c", $Context.class);
            fields.put("$c", $c);
        }
    }

    private void requiresDescriptorsField() {
        this.hasDescriptors = true;
        if (!fields.containsKey("$d")) {
            FieldInfo $d = new FieldInfo("$d", $Descriptor[].class);
            fields.put("$d", $d);
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

    public Statement newAssignmentStatement(Field field, Expression value) {
        return new AssignmentStatement(field, value);
    }

    public Statement newMethodCallStatement(String methodName, List<Expression> params, Type[] types, Class<?> returnType) {
        return new MethodCallStatement(methodName, params, types, returnType);
    }

    public Statement newReturnStatement(Expression value) {
        return new ReturnStatement(value);
    }

    public Class<?> define() {
        defineInstantiator();

        ClassWriter cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES); // flags: compute nothing
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, internalNameCdi, null,
                internalName, new String[]{"org/ibess/cdi/internal/$CdiObject"});
        cw.visitInnerClass(internalNameCdiI, internalNameCdi, "$I", ACC_PUBLIC | ACC_STATIC | ACC_FINAL);
        // other inner classes

        // fields
        cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "$i", "L" + internalName + "$Cdi$$I;", null, null).visitEnd();
        for (FieldInfo field : fields.values()) {
            String type = field.type.isArray()
                    ? field.type.getName().replace('.', '/')
                    : "L" + field.type.getName().replace('.', '/') + ";";
            cw.visitField(ACC_PRIVATE | ACC_FINAL, field.name, type, null, null).visitEnd();
        }

        for (MethodInfo method : methods) {
            MethodVisitor mv = method.visitMethod(cw);
            method.appendCode(mv);
        }

        // <init>
        {
            String fstParam = hasContext ? "Lorg/ibess/cdi/internal/$Context;" : "";
            String sndParam = hasDescriptors ? "[Lorg/ibess/cdi/internal/$Descriptor;" : "";

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(" + fstParam + sndParam + ")V", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", "()V", false);

            if (hasContext) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, internalNameCdi, "$c", fstParam);
            }

            if (hasDescriptors) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, hasContext ? 2 : 1);
                mv.visitFieldInsn(PUTFIELD, internalNameCdi, "$d", sndParam);
            }

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // <clinit>
        {
            MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, internalNameCdiI);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, internalNameCdiI, "<init>", "()V", false);
            mv.visitFieldInsn(PUTSTATIC, internalNameCdi, "$i", "L" + internalName + "$Cdi$$I;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return JavaC.defineClass(superClass.getName() + "$Cdi", cw.toByteArray());
    }

    private void defineInstantiator() {
        ClassWriter cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, internalNameCdiI, null, "java/lang/Object", new String[]{"org/ibess/cdi/internal/$Instantiator"});
        cw.visitInnerClass(internalNameCdiI, internalNameCdi, "$I", ACC_PUBLIC | ACC_STATIC | ACC_FINAL);

        // <init>
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // $create
        {
            String fstParam = hasContext ? "Lorg/ibess/cdi/internal/$Context;" : "";
            String sndParam = hasDescriptors ? "[Lorg/ibess/cdi/internal/$Descriptor;" : "";

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "$create", "(Lorg/ibess/cdi/internal/$Context;[Lorg/ibess/cdi/internal/$Descriptor;)Ljava/lang/Object;", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, internalNameCdi);
            mv.visitInsn(DUP);
            if (hasContext) {
                mv.visitVarInsn(ALOAD, 1);
            }
            if (hasDescriptors) {
                mv.visitVarInsn(ALOAD, 2);
            }
            mv.visitMethodInsn(INVOKESPECIAL, internalNameCdi, "<init>", "(" + fstParam + sndParam + ")V", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        JavaC.defineClass(superClass.getName() + "$Cdi$$I", cw.toByteArray());
    }

    private interface Appendable {

        default void appendItself(StringBuilder sb) {};

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
        private Class type;
        private boolean isStatic = false;

        FieldInfo(String name, Class type) {
            this.name = name;
            this.type = type;
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

        ParameterInfo addParameter(String name, Class type) {
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

        public MethodVisitor visitMethod(ClassWriter cw) {
            StringBuilder desc = new StringBuilder("(");
            for (ParameterInfo param : params) {
                String s = internalType(param.type);
                desc.append(s);
            }
            desc.append(")");
            desc.append(internalType(returnType));
            return cw.visitMethod(ACC_PUBLIC, name, desc.toString(), null, null);
        }

        public void appendCode(MethodVisitor mv) {
            mv.visitCode();
            for (Statement statement : statements) {
                statement.appendCode(mv);
            }
            if (returnType == void.class) {
                mv.visitInsn(RETURN);
            }
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    private static String internalType(Type returnType) {
        Class c;
        if (returnType instanceof Class) {
            c = (Class) returnType;
        } else if (returnType instanceof ParameterizedType) {
            c = (Class) ((ParameterizedType) returnType).getRawType();
        } else {
            c = Object.class;
        }
        if (c.isPrimitive()) {
            if (c == boolean.class) return "Z";
            if (c == byte.class) return "B";
            if (c == char.class) return "C";
            if (c == double.class) return "D";
            if (c == float.class) return "F";
            if (c == int.class) return "I";
            if (c == long.class) return "J";
            if (c == short.class) return "S";
            if (c == void.class) return "V";
        }
        return c.isArray()
                ? c.getName().replace('.', '/')
                : "L" + c.getName().replace('.', '/') + ";";
    }

    static class ParameterInfo implements Appendable {
        private final Class type;

        ParameterInfo(String name, Class type) {
            this.type = type;
        }
    }

    public interface Statement extends Appendable {
        void appendCode(MethodVisitor mv);
    }

    private class AssignmentStatement implements Statement {
        private final Field field;
        private final Expression value;

        AssignmentStatement(Field field, Expression value) {
            this.field = field;
            this.value = value;
        }

        @Override
        public void appendCode(MethodVisitor mv) {
            mv.visitVarInsn(ALOAD, 0);
            value.appendCode(mv);
            mv.visitFieldInsn(PUTFIELD, internalName, field.getName(), internalType(field.getType()));
        }
    }

    private static class ReturnStatement implements Statement {
        private final Expression value;

        ReturnStatement(Expression value) {
            this.value = value;
        }

        @Override
        public void appendCode(MethodVisitor mv) {
            value.appendCode(mv);
            mv.visitInsn(ARETURN);
        }
    }

    public interface Expression extends Appendable {

        void appendCode(MethodVisitor mv);
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
        public void appendCode(MethodVisitor mv) {
            if (type == Context.class) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, internalNameCdi, "$c", "Lorg/ibess/cdi/internal/$Context;");
                return;
            }

            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class clazz = (Class) parameterizedType.getRawType();
                if (clazz == Class.class) {
                    Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
                    TypeVariable actualTypeVariable = (TypeVariable) actualTypeArgument;
                    String actualTypeVariableName = actualTypeVariable.getName();

                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, internalNameCdi, "$d", "[Lorg/ibess/cdi/internal/$Descriptor;");
                    int index = paramsIndex.get(actualTypeVariableName);
                    if (index < 6) {
                        mv.visitInsn(ICONST_0 + index);
                    } else {
                        mv.visitVarInsn(SIPUSH, index);
                    }
                    mv.visitInsn(AALOAD);
                    mv.visitFieldInsn(GETFIELD, "Lorg/ibess/cdi/internal/$Descriptor;", "c", "Ljava/lang/Class;");
                    return;
                }
            }

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, internalNameCdi, "$c", "Lorg/ibess/cdi/internal/$Context;");
            appendDescriptor(mv, type);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/ibess/cdi/internal/$Context", "$lookup", "(Lorg/ibess/cdi/internal/$Descriptor;)Ljava/lang/Object;", true);
            String internalType = internalType(this.type);
            mv.visitTypeInsn(CHECKCAST, internalType.substring(1, internalType.length() - 1));
        }

        private void appendDescriptor(MethodVisitor mv, Type currentType) {
            if (currentType instanceof TypeVariable) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, internalNameCdi, "$d", "[Lorg/ibess/cdi/internal/$Descriptor;");
                int index = paramsIndex.get(currentType.getTypeName());
                if (index < 6) {
                    mv.visitInsn(ICONST_0 + index);
                } else {
                    mv.visitVarInsn(SIPUSH, index);
                }
                mv.visitInsn(AALOAD);
            } else {
                if (currentType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) currentType;
                    Class clazz = (Class) parameterizedType.getRawType();

                    mv.visitLdcInsn(org.objectweb.asm.Type.getType(internalType(clazz)));
                    if (clazz.isAnnotationPresent(Scoped.class)) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if (actualTypeArguments.length == 0) {
                            mv.visitMethodInsn(INVOKESTATIC, "org/ibess/cdi/internal/$Descriptor", "$0", "(Ljava/lang/Class;)Lorg/ibess/cdi/internal/$Descriptor;", false);
                        } else {
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
                                mv.visitVarInsn(ALOAD, 0);
                                mv.visitFieldInsn(GETFIELD, internalNameCdi, "$d", "[Lorg/ibess/cdi/internal/$Descriptor;");
                            } else {
                                int index = 0, length = actualTypeArguments.length;
                                if (length < 6) {
                                    mv.visitInsn(ICONST_0 + length);
                                } else {
                                    mv.visitVarInsn(SIPUSH, length);
                                }
                                mv.visitTypeInsn(ANEWARRAY, "org/ibess/cdi/internal/$Descriptor");
                                for (Type actualTypeArgument : actualTypeArguments) {
                                    mv.visitInsn(DUP);
                                    if (index < 6) {
                                        mv.visitInsn(ICONST_0 + index);
                                    } else {
                                        mv.visitVarInsn(SIPUSH, index);
                                    }
                                    appendDescriptor(mv, actualTypeArgument);
                                    mv.visitInsn(AASTORE);
                                    index++;
                                }
                            }
                            mv.visitMethodInsn(INVOKESTATIC, "org/ibess/cdi/internal/$Descriptor", "$", "(Ljava/lang/Class;[Lorg/ibess/cdi/internal/$Descriptor;)Lorg/ibess/cdi/internal/$Descriptor;", false);
                        }
                    } else {
                        mv.visitMethodInsn(INVOKESTATIC, "org/ibess/cdi/internal/$Descriptor", "$0", "(Ljava/lang/Class;)Lorg/ibess/cdi/internal/$Descriptor;", false);
                    }
                } else if (currentType instanceof Class) {
                    Class clazz = (Class) currentType;
                    mv.visitLdcInsn(org.objectweb.asm.Type.getType(internalType(clazz)));
                    mv.visitMethodInsn(INVOKESTATIC, "org/ibess/cdi/internal/$Descriptor", "$0", "(Ljava/lang/Class;)Lorg/ibess/cdi/internal/$Descriptor;", false);
                } else {
                    throw new ImpossibleError();
                }
            }
        }
    }

    private class MethodCallStatement implements Statement {
        private String name;
        private List<Expression> params;
        private Type[] types;
        private Class<?> returnType;

        MethodCallStatement(String name, List<Expression> params, Type[] types, Class<?> returnType) {
            this.name = name;
            this.params = params;
            this.types = types;
            this.returnType = returnType;
        }

        @Override
        public void appendItself(StringBuilder sb) {
            sb.append("    ").append(name).append("(");
            Appendable.appendCommaSeparated(params, sb);
            sb.append(");\n");
        }

        @Override
        public void appendCode(MethodVisitor mv) {
            for (Expression param : params) {
                param.appendCode(mv);
            }
            StringBuilder desc = new StringBuilder("(");
            for (Type type : types) {
                desc.append(internalType(type));
            }
            desc.append(")").append(internalType(returnType));
            mv.visitMethodInsn(INVOKESPECIAL, internalName, name, desc.toString(), false);
        }
    }
}
