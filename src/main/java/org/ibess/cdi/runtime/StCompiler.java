package org.ibess.cdi.runtime;

import org.ibess.cdi.runtime.st.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author ibessonov
 */
public class StCompiler implements StVisitor {

    private static final int CLASS_VERSION = V1_8;

    private ClassWriter cw;
    private String internalClassName;
    private MethodVisitor mv;
    private Class<?>[] parameters;
    private int[] offsets;
    private Class<?> returnType;
    private final List<Label> hooks = new ArrayList<>();

    public static byte[] compile(StClass clazz) {
        return new StCompiler().compile0(clazz);
    }

    private byte[] compile0(StClass clazz) {
        cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
        clazz.accept(this);
        try {
            return cw.toByteArray();
        } finally {
            cw = null;
        }
    }

    @Override
    public void visitClass(StClass clazz) {
        Class<?> superClass = clazz.superClass;
        boolean isInterface = superClass.isInterface();
        if (isInterface) {
            superClass = Object.class;
        }
        String superClassInternalName = getInternalName(superClass);
        internalClassName = internal(clazz.name);
        Class<?>[] interfaces = clazz.interfaces;
        String[] interfacesInternalNames = new String[interfaces.length + (isInterface ? 1 : 0)];
        for (int i = 0, size = interfaces.length; i < size; i++) {
            interfacesInternalNames[i] = getInternalName(interfaces[i]);
        }
        if (isInterface) {
            interfacesInternalNames[interfaces.length] = getInternalName(clazz.superClass);
        }

        cw.visit(CLASS_VERSION, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, internalClassName, null,
                 superClassInternalName, interfacesInternalNames);
        for (StField field : clazz.fields) {
            field.accept(this);
        }
        for (StMethod method : clazz.methods) {
            method.accept(this);
        }
    }

    @Override
    public void visitField(StField field) {
        int modifiers = field.isStatic ? ACC_PUBLIC | ACC_STATIC : ACC_PRIVATE | ACC_FINAL;
        cw.visitField(modifiers, field.name, getDescriptor(field.type), null, null).visitEnd();
    }

    @Override
    public void visitMethod(StMethod method) {
        Class<?>[] params = method.parameters;
        Type[] types = new Type[params.length];
        for (int i = 0, size = params.length; i < size; i++) {
            types[i] = getType(params[i]);
        }
        Type returnType = getType(method.returnType);
        int modifiers = method.isStatic ? ACC_STATIC | ACC_PUBLIC : ACC_PUBLIC;
        mv = cw.visitMethod(modifiers, method.name, getMethodDescriptor(returnType, types), null, null);
        mv.visitCode();

        parameters = method.parameters;
        offsets = new int[parameters.length];
        for (int i = 1; i < parameters.length; i++) {
            offsets[i] = offsets[i - 1] + 1
                    + (parameters[i - 1] == long.class || parameters[i - 1] == double.class ? 1 : 0);
        }
        this.returnType = method.returnType;
        method.statement.accept(this);
        this.returnType = null;
        offsets = null;
        parameters = null;

        if (returnType == VOID_TYPE) {
            mv.visitInsn(RETURN);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = null;
    }

    @Override
    public void visitScopeStatement(StScopeStatement scopeStatement) {
        for (StStatement statement : scopeStatement.statements) {
            statement.accept(this);
        }
    }

    @Override
    public void visitReturnStatement(StReturnStatement returnStatement) {
        returnStatement.expression.accept(this);
        if (hooks.isEmpty()) {
            mv.visitInsn(returnOpcode(returnType));
        } else {
            mv.visitJumpInsn(GOTO, hooks.get(0));
        }
    }

    @Override
    public void visitFieldAssignmentStatement(StFieldAssignmentStatement fieldAssignmentStatement) {
        FieldInfo field = fieldAssignmentStatement.field;
        if (!field.isStatic) {
            fieldAssignmentStatement.left.accept(this);
        }
        fieldAssignmentStatement.right.accept(this);
        String internalClassName = field.declaringClassName == null
                                 ? this.internalClassName
                                 : internal(field.declaringClassName);
        mv.visitFieldInsn(field.isStatic ? PUTSTATIC : PUTFIELD, internalClassName,
                          field.name, descriptor(field.className)
        );
    }

    @Override
    public void visitParamAssignmentStatement(StParamAssignmentStatement paramAssignmentStatement) {
        paramAssignmentStatement.expression.accept(this);
        int offset = offsets[paramAssignmentStatement.index];
        mv.visitVarInsn(storeOpcode(parameters[paramAssignmentStatement.index]), offset + 1);
    }

    @Override
    public void visitIfStatement(StIfStatement ifStatement) {
        ifStatement.condition.accept(this);
        Label elseLabel = new Label();
        mv.visitJumpInsn(ifStatement.negate ? IFEQ : IFNE, elseLabel);
        ifStatement.then.accept(this);

        if (ifStatement.els == null) {
            mv.visitLabel(elseLabel);
        } else {
            Label endLabel = new Label();
            mv.visitJumpInsn(GOTO, endLabel);
            mv.visitLabel(elseLabel);
            ifStatement.els.accept(this);
            mv.visitLabel(endLabel);
        }
    }

    @Override
    public void visitIfNullStatement(StIfNullStatement ifNullStatement) {
        ifNullStatement.expression.accept(this);
        Label elseLabel = new Label();
        mv.visitJumpInsn(ifNullStatement.negate ? IFNULL : IFNONNULL, elseLabel);
        ifNullStatement.then.accept(this);

        if (ifNullStatement.els == null) {
            mv.visitLabel(elseLabel);
        } else {
            Label endLabel = new Label();
            mv.visitJumpInsn(GOTO, endLabel);
            mv.visitLabel(elseLabel);
            ifNullStatement.els.accept(this);
            mv.visitLabel(endLabel);
        }
    }

    @Override
    public void visitNullExpression(StNullExpression nullExpression) {
        mv.visitInsn(ACONST_NULL);
    }

    @Override
    public void visitGetParameterExpression(StGetParameterExpression getParameterExpression) {
        int offset = offsets[getParameterExpression.index];
        mv.visitVarInsn(loadOpcode(parameters[getParameterExpression.index]), offset + 1);
    }

    @Override
    public void visitGetFieldExpression(StGetFieldExpression getFieldExpression) {
        FieldInfo field = getFieldExpression.field;
        if (!field.isStatic) {
            getFieldExpression.left.accept(this);
        }
        String internalClassName = field.declaringClassName == null
                                 ? this.internalClassName
                                 : internal(field.declaringClassName);
        mv.visitFieldInsn(field.isStatic ? GETSTATIC : GETFIELD, internalClassName,
                          field.name, descriptor(field.className)
        );
    }

    @Override
    public void visitThisExpression(StThisExpression thisExpression) {
        mv.visitVarInsn(ALOAD, 0);
    }

    @Override
    public void visitMethodCallStatement(StMethodCallStatement methodCallStatement) {
        methodCallStatement.methodCallExpression.accept(this);
        if (methodCallStatement.methodCallExpression.returnType != void.class) {
            mv.visitInsn(POP);
        }
    }

    @Override
    public void visitClassExpression(StClassExpression classExpression) {
        mv.visitLdcInsn(getType(getDescriptor(classExpression.type)));
    }

    @Override
    public void visitMethodCallExpression(StMethodCallExpression methodCallExpression) {
        if (methodCallExpression.invokeType != InvokeType.STATIC) {
            methodCallExpression.object.accept(this);
        }
        for (StExpression parameter : methodCallExpression.parameters) {
            parameter.accept(this);
        }

        int opcode = -1;
        switch (methodCallExpression.invokeType) {
            case INTERFACE: opcode = INVOKEINTERFACE; break;
            case SPECIAL:   opcode = INVOKESPECIAL;   break;
            case VIRTUAL:   opcode = INVOKEVIRTUAL;   break;
            case STATIC:    opcode = INVOKESTATIC;    break;
        }

        Class<?>[] params = methodCallExpression.paramTypes;
        Type[] paramTypes = new Type[params.length];
        for (int i = 0, size = params.length; i < size; i++) {
            paramTypes[i] = getType(params[i]);
        }
        Type returnType = getType(methodCallExpression.returnType);
        mv.visitMethodInsn(opcode, internal(methodCallExpression.declaringClassName), methodCallExpression.name,
                           getMethodDescriptor(returnType, paramTypes), opcode == INVOKEINTERFACE);
    }

    @Override
    public void visitArrayElementExpression(StArrayElementExpression arrayElementExpression) {
        arrayElementExpression.array.accept(this);
        arrayElementExpression.index.accept(this);
        mv.visitInsn(AALOAD); // Object only :(
    }

    @Override
    public void visitIntConstantExpression(StIntConstantExpression intConstantExpression) {
        visitIntConstantExpression(intConstantExpression.index, intConstantExpression.type);
    }

    private void visitIntConstantExpression(int index, Class<?> type) {
        if (index >= 0 && index < 6) {
            mv.visitInsn(ICONST_0 + index);
        } else {
            if (type == int.class) {
                mv.visitLdcInsn(index);
            } else if (type == byte.class) {
                mv.visitIntInsn(BIPUSH, index);
            } else {
                mv.visitIntInsn(SIPUSH, index);
            }
        }
    }

    @Override
    public void visitConstant(StConstant constant) {
        mv.visitLdcInsn(constant.value);
    }

    @Override
    public void visitCastExpression(StCastExpression castExpression) {
        castExpression.expression.accept(this);
        mv.visitTypeInsn(CHECKCAST, getInternalName(castExpression.type));
    }

    @Override
    public void visitArrayExpression(StArrayExpression arrayExpression) {
        StExpression[] elements = arrayExpression.elements;
        visitIntConstantExpression(elements.length, int.class);
        Class<?> type = arrayExpression.type;
        if (type.isPrimitive()) {
            mv.visitIntInsn(NEWARRAY, newArrayTypeOpcode(type));
        } else {
            mv.visitTypeInsn(ANEWARRAY, getInternalName(type));
        }

        int index = 0;
        for (StExpression element : elements) {
            mv.visitInsn(DUP);
            visitIntConstantExpression(index++, int.class);
            element.accept(this);
            mv.visitInsn(astoreOpcode(type));
        }
    }

    @Override
    public void visitDupExpression(StDupExpression dupExpression) {
        mv.visitInsn(DUP);
    }

    @Override
    public void visitNewExpression(StNewExpression newExpression) {
        mv.visitTypeInsn(NEW, internal(newExpression.className));
    }

    @Override
    public void visitNoopStatement(StNoopStatement noopStatement) {
    }

    @Override
    public void visitSwapExpression(StSwapExpression swapExpression) {
        mv.visitInsn(SWAP);
    }

    @Override
    public void visitReturnHookStatement(StReturnHookStatement returnHookStatement) {
        hooks.add(0, new Label());
        returnHookStatement.statement.accept(this);
        mv.visitLabel(hooks.remove(0));
        returnHookStatement.hook.accept(this);
    }

    private static int storeOpcode(Class<?> type) {
        if (!type.isPrimitive()) return ASTORE;
        switch (type.getName()) {
            case "long":   return LSTORE;
            case "float":  return FSTORE;
            case "double": return DSTORE;
            default:       return ISTORE;
        }
    }

    private static int loadOpcode(Class<?> type) {
        if (!type.isPrimitive()) return ALOAD;
        switch (type.getName()) {
            case "long":   return LLOAD;
            case "float":  return FLOAD;
            case "double": return DLOAD;
            default:       return ILOAD;
        }
    }

    private static int returnOpcode(Class<?> type) {
        if (type == null || !type.isPrimitive()) return ARETURN;
        switch (type.getName()) {
            case "long":   return LRETURN;
            case "float":  return FRETURN;
            case "double": return DRETURN;
            default:       return IRETURN;
        }
    }

    private int astoreOpcode(Class<?> type) {
        if (!type.isPrimitive()) return AASTORE;
        switch (type.getName()) {
            case "boolean":
            case "byte"   : return BASTORE;
            case "char"   : return CASTORE;
            case "short"  : return SASTORE;
            case "int"    : return IASTORE;
            case "long"   : return LASTORE;
            case "float"  : return FASTORE;
            default       : return DASTORE;
        }
    }

    private int newArrayTypeOpcode(Class<?> type) {
        switch (type.getName()) {
            case "boolean": return T_BOOLEAN;
            case "byte"   : return T_BYTE;
            case "char"   : return T_CHAR;
            case "short"  : return T_SHORT;
            case "int"    : return T_INT;
            case "long"   : return T_LONG;
            case "float"  : return T_FLOAT;
            default       : return T_DOUBLE;
        }
    }

    private static String internal(String name) {
        return name.replace('.', '/');
    }

    private static String descriptor(String name) {
        String internal = internal(name);
        if (internal.startsWith("[")) return internal;

        switch (internal) {
            case "boolean": return "Z";
            case "byte"   : return "B";
            case "char"   : return "C";
            case "short"  : return "S";
            case "int"    : return "I";
            case "long"   : return "J";
            case "float"  : return "F";
            case "double" : return "D";
        }
        return "L" + internal + ";";
    }
}
