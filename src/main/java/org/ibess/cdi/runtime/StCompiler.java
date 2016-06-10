package org.ibess.cdi.runtime;

import org.ibess.cdi.runtime.st.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

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
        String superClassInternalName = getInternalName(clazz.superClass);
        internalClassName = internal(clazz.name);
        List<Class<?>> interfaces = clazz.interfaces;
        String[] interfacesInternalNames = new String[interfaces.size()];
        for (int i = 0, size = interfaces.size(); i < size; i++) {
            interfacesInternalNames[i] = getInternalName(interfaces.get(i));
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
        cw.visitField(modifiers, field.name, getDescriptor(field.type), null, null).visitEnd();
    }

    @Override
    public void visitMethod(StMethod method) {
        List<Class<?>> params = method.parameters;
        Type[] types = new Type[params.size()];
        for (int i = 0, size = params.size(); i < size; i++) {
            types[i] = getType(params.get(i));
        }
        Type returnType = getType(method.returnType);
        int modifiers = method.isStatic ? ACC_STATIC | ACC_PUBLIC : ACC_PUBLIC;
        mv = cw.visitMethod(modifiers, method.name, getMethodDescriptor(returnType, types), null, null);
        mv.visitCode();

        method.statement.accept(this);

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
        mv.visitInsn(ARETURN); // Object only
    }

    @Override
    public void visitFieldAssignmentStatement(StFieldAssignmentStatement fieldAssignmentStatement) {
        if (!fieldAssignmentStatement.isStatic) {
            fieldAssignmentStatement.left.accept(this);
        }
        fieldAssignmentStatement.right.accept(this);
        String internalClassName = fieldAssignmentStatement.declaringClassName == null
                                 ? this.internalClassName
                                 : internal(fieldAssignmentStatement.declaringClassName);
        mv.visitFieldInsn(fieldAssignmentStatement.isStatic ? PUTSTATIC : PUTFIELD, internalClassName,
                          fieldAssignmentStatement.fieldName,
                          descriptor(fieldAssignmentStatement.fieldClassName)
        );
    }

    @Override
    public void visitNullExpression(StNullExpression nullExpression) {
        mv.visitInsn(ACONST_NULL);
    }

    @Override
    public void visitGetParameterExpression(StGetParameterExpression getParameterExpression) {
        mv.visitVarInsn(ALOAD, getParameterExpression.index + 1); // Object only
    }

    @Override
    public void visitGetFieldExpression(StGetFieldExpression getFieldExpression) {
        getFieldExpression.left.accept(this);
        String internalClassName = getFieldExpression.declaringClassName == null
                                 ? this.internalClassName
                                 : internal(getFieldExpression.declaringClassName);
        mv.visitFieldInsn(GETFIELD, internalClassName,
                          getFieldExpression.fieldName,
                          getDescriptor(getFieldExpression.fieldClass)
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

        List<Class<?>> params = methodCallExpression.paramTypes;
        Type[] paramTypes = new Type[params.size()];
        for (int i = 0, size = params.size(); i < size; i++) {
            paramTypes[i] = getType(params.get(i));
        }
        Type returnType = getType(methodCallExpression.returnType);
        mv.visitMethodInsn(opcode, internal(methodCallExpression.declaringClassName), methodCallExpression.name,
                           getMethodDescriptor(returnType, paramTypes), opcode == INVOKEINTERFACE);
    }

    @Override
    public void visitArrayElementExpression(StArrayElementExpression arrayElementExpression) {
        arrayElementExpression.array.accept(this);
        arrayElementExpression.index.accept(this);
        mv.visitInsn(AALOAD);
    }

    @Override
    public void visitIntConstantExpression(StIntConstantExpression intConstantExpression) {
        visitIntConstantExpression(intConstantExpression.index);
    }

    private void visitIntConstantExpression(int index) {
        if (index < 6) {
            mv.visitInsn(ICONST_0 + index);
        } else {
            mv.visitVarInsn(SIPUSH, index);
        }
    }

    @Override
    public void visitCastExpression(StCastExpression castExpression) {
        castExpression.expression.accept(this);
        mv.visitTypeInsn(CHECKCAST, getInternalName(castExpression.type));
    }

    @Override
    public void visitArrayExpression(StArrayExpression arrayExpression) {
        List<StExpression> elements = arrayExpression.elements;
        visitIntConstantExpression(elements.size());
        mv.visitTypeInsn(ANEWARRAY, getInternalName(arrayExpression.type));

        int index = 0;
        for (StExpression element : elements) {
            mv.visitInsn(DUP);
            visitIntConstantExpression(index++);
            element.accept(this);
            mv.visitInsn(AASTORE);
        }
    }

    @Override
    public void visitDupExpression(StDupExpression dupExpression) {
        dupExpression.expression.accept(this);
        mv.visitInsn(DUP);
    }

    @Override
    public void visitNewExpression(StNewExpression newExpression) {
        mv.visitTypeInsn(NEW, internal(newExpression.className));
    }

    @Override
    public void visitNoopStatement(StNoopStatement noopStatement) {
    }

    private static String internal(String name) {
        return name.replace('.', '/');
    }

    private static String descriptor(String name) {
        String internal = internal(name);
        return internal.startsWith("[") ? internal : "L" + internal + ";";
    }
}
