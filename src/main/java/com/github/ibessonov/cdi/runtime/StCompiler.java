package com.github.ibessonov.cdi.runtime;

import com.github.ibessonov.cdi.runtime.st.*;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.*;

import static com.github.ibessonov.cdi.util.ClassUtil.isPrimitive;
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
    private final Map<String, Class<?>> fieldTypes = new HashMap<>();
    private MethodVisitor mv;
    private Class<?>[] parameters;
    private int[] offsets;
    private Class<?> returnType;

    public static byte[] compile(StClass clazz) {
        return new StCompiler().compile0(clazz);
    }

    private byte[] compile0(StClass clazz) {
        cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
        clazz.accept(this);

        byte[] result = cw.toByteArray();
        cw = null;
        return result;
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
        int modifiers = field.isStatic ? ACC_PUBLIC | ACC_STATIC | ACC_FINAL : ACC_PRIVATE | ACC_FINAL;
        fieldTypes.put(field.name, field.type);
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

        parameters = method.parameters;
        offsets = new int[parameters.length];
        for (int i = 1; i < parameters.length; i++) {
            offsets[i] = offsets[i - 1] + 1
                    + (parameters[i - 1] == long.class || parameters[i - 1] == double.class ? 1 : 0);
        }
        this.returnType = method.returnType;

        mv = cw.visitMethod(modifiers, method.name, getMethodDescriptor(returnType, types), null, null);
        mv.visitCode();

        method.statement.accept(this);

        this.returnType = null;
        offsets = null;
        parameters = null;

        if (returnType == VOID_TYPE) {
            mv.visitInsn(RETURN);
        }
        mv.visitMaxs(-1, -1);
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
        mv.visitInsn(returnOpcode(returnType));
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
        ifStatement.expression.accept(this);
        Label elseLabel = new Label();
        int opcode = ifStatement.compareToNull
                ? (ifStatement.negate ? IFNULL : IFNONNULL) //?
                : (ifStatement.negate ? IFNE : IFEQ);
        mv.visitJumpInsn(opcode, elseLabel);
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
        String fieldClassName = field.declaringClassName == null
                              ? fieldTypes.get(field.name).getName()
                              : field.className;
        mv.visitFieldInsn(field.isStatic ? GETSTATIC : GETFIELD, internalClassName,
                          field.name, descriptor(fieldClassName)
        );
    }

    @Override
    public void visitThisExpression(StThisExpression thisExpression) {
        mv.visitVarInsn(ALOAD, 0);
    }

    @Override
    public void visitMethodCallStatement(StMethodCallStatement methodCallStatement) {
        StExpression expression = methodCallStatement.expression;
        expression.accept(this);
        if (expression instanceof StTypedExpression) {
            if (((StTypedExpression) expression).getType() != void.class) {
                mv.visitInsn(POP);
            }
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

        int opcode = getInvokeMethodOpcode(methodCallExpression.invokeType);

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
    public void visitInvokeDynamicExpression(StInvokeDynamicExpression invokeDynamicExpression) {
        for (StExpression parameter : invokeDynamicExpression.parameters) {
            parameter.accept(this);
        }
        Class<?>[] params = invokeDynamicExpression.paramTypes;
        Type[] paramTypes = new Type[params.length];
        for (int i = 0, size = params.length; i < size; i++) {
            paramTypes[i] = params[i] == null ? getType(descriptor(internalClassName)) : getType(params[i]);
        }
        Type returnType = getType(invokeDynamicExpression.returnType);
        Type[] metafactoryParams = new Type[3 + invokeDynamicExpression.args.length];
        metafactoryParams[0] = getType(MethodHandles.Lookup.class);
        metafactoryParams[1] = getType(String.class);
        metafactoryParams[2] = getType(MethodType.class);
        for (int i = 0, len = invokeDynamicExpression.args.length; i < len; i++) {
            metafactoryParams[i + 3] = getType(invokeDynamicExpression.args[i].getClass());
        }

        mv.visitInvokeDynamicInsn(invokeDynamicExpression.methodName, getMethodDescriptor(returnType, paramTypes),
            new Handle(H_INVOKESTATIC, internal(invokeDynamicExpression.metafactory.getName()),
                invokeDynamicExpression.metafactoryMethod,
                getMethodDescriptor(getType(CallSite.class), metafactoryParams), false
            ), invokeDynamicExpression.args
        );
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
        if (isPrimitive(type)) {
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

    private int getInvokeMethodOpcode(InvokeType invokeType) {
        switch (invokeType) {
            case STATIC:    return INVOKESTATIC;
            case VIRTUAL:   return INVOKEVIRTUAL;
            case SPECIAL:   return INVOKESPECIAL;
            default:        return INVOKEINTERFACE;
        }
    }

    private static int storeOpcode(Class<?> type) {
        return ISTORE + opcodeOffset(type);
    }

    private static int loadOpcode(Class<?> type) {
        return ILOAD + opcodeOffset(type);
    }

    private static int returnOpcode(Class<?> type) {
        return IRETURN + opcodeOffset(type);
    }

    private static int opcodeOffset(Class<?> type) {
        if (type == null || !isPrimitive(type)) return 4;
        switch (type.getName()) {
            case "long":   return 1;
            case "float":  return 2;
            case "double": return 3;
            default:       return 0;
        }
    }

    private int astoreOpcode(Class<?> type) {
        if (!isPrimitive(type)) return AASTORE;
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
        if (name.startsWith("[")) return internal(name);

        switch (name) {
            case "boolean": return "Z";
            case "byte"   : return "B";
            case "char"   : return "C";
            case "short"  : return "S";
            case "int"    : return "I";
            case "long"   : return "J";
            case "float"  : return "F";
            case "double" : return "D";
        }
        return "L" + internal(name) + ";";
    }
}
