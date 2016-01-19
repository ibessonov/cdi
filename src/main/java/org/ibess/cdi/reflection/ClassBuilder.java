package org.ibess.cdi.reflection;

import org.ibess.cdi.Context;
import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.exceptions.ImpossibleError;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.*;
import java.util.*;

import static org.ibess.cdi.javac.CdiClassLoader.defineClass;
import static org.ibess.cdi.reflection.ClassBuilderConstants.*;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author ibessonov
 */
//TODO separate "tree construction" from bytecode generation
public class ClassBuilder {

    private final String internalName;
    private final String internalNameCdi;
    private final String internalNameCdiI;

    private final Map<String, String> fields = new HashMap<>();
    private final Set<MethodInfo> methods = new HashSet<>();

    private final Map<String, Integer> paramsIndex = new HashMap<>();

    private boolean hasContext = false;
    private boolean hasDescriptors = false;
    private MethodInfo construct = null;

    public ClassBuilder(Class<?> superClass) {
        this.internalName     = getInternalName(superClass);
        this.internalNameCdi  = internalName + CDI_SUFFIX;
        this.internalNameCdiI = internalNameCdi + $ + I_SUFFIX;

        TypeVariable[] params = superClass.getTypeParameters();
        for (int i = 0; i < params.length; i++) {
            this.paramsIndex.put(params[i].getName(), i);
        }
    }

    private void requiresContextField() {
        if (!hasContext) {
            hasContext = true;
            fields.put(CONTEXT_F_NAME, CONTEXT_DESCR);
        }
    }

    private void requiresDescriptorsField() {
        if (!hasDescriptors) {
            hasDescriptors = true;
            fields.put(DESCRIPTOR_F_NAME, DESCRIPTOR_A_DESCR);
        }
    }

    public MethodInfo getConstructMethod() {
        MethodInfo construct = this.construct;
        if (construct == null) {
            this.construct = construct = new MethodInfo(CONSTRUCT_M_NAME, VOID_TYPE);
            this.methods.add(construct);
        }
        return construct;
    }

    public MethodInfo createNewMethod(String name, Class returnType) {
        MethodInfo method = new MethodInfo(name, getType(returnType));
        methods.add(method);
        return method;
    }

    public Expression newThisExpression() {
        return new ThisExpression();
    }

    public Expression newLookupExpression(Type type) {
        return new ComplexInjectionExpression(type);
    }

    public Statement newAssignmentStatement(Field field, Expression value) {
        return new AssignmentStatement(field, value);
    }

    public Statement newMethodCallStatement(Method method, Expression self, List<Expression> params) {
        return new MethodCallStatement(method, self, params);
    }

    public Statement newReturnStatement(Expression value) {
        return new ReturnStatement(value);
    }

    public Class<?> define() {
        defineInstantiator();

        ClassWriter cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES); // flags: compute nothing
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, internalNameCdi, null,
                internalName, new String[]{ CDI_OBJECT_INTERNAL });
        cw.visitInnerClass(internalNameCdiI, internalNameCdi, I_SUFFIX, ACC_PUBLIC | ACC_STATIC | ACC_FINAL);
        // other inner classes

        // fields
        cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, INSTANCE_F_NAME, "L" + internalNameCdiI + ";", null, null).visitEnd();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            cw.visitField(ACC_PRIVATE | ACC_FINAL, entry.getKey(), entry.getValue(), null, null).visitEnd();
        }

        for (MethodInfo method : methods) {
            MethodVisitor mv = method.visitMethod(cw);
            method.appendCode(mv);
        }

        // <init>
        {
            String fstParam = hasContext ? CONTEXT_DESCR : "";
            String sndParam = hasDescriptors ? DESCRIPTOR_A_DESCR : "";

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, INIT_M_NAME, "(" + fstParam + sndParam + ")V", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, internalName, INIT_M_NAME, INIT_M_DESCR, false);

            if (hasContext) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, internalNameCdi, CONTEXT_F_NAME, fstParam);
            }

            if (hasDescriptors) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, hasContext ? 2 : 1);
                mv.visitFieldInsn(PUTFIELD, internalNameCdi, DESCRIPTOR_F_NAME, sndParam);
            }

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // <clinit>
        {
            MethodVisitor mv = cw.visitMethod(ACC_STATIC, CLINIT_M_NAME, INIT_M_DESCR, null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, internalNameCdiI);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, internalNameCdiI, INIT_M_NAME, INIT_M_DESCR, false);
            mv.visitFieldInsn(PUTSTATIC, internalNameCdi, INSTANCE_F_NAME, "L" + internalNameCdiI + ";");
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return defineClass(cw.toByteArray());
    }

    private void defineInstantiator() {
        ClassWriter cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, internalNameCdiI, null, OBJECT_INTERNAL, new String[]{ INSTANTIATOR_INTERNAL });
        cw.visitInnerClass(internalNameCdiI, internalNameCdi, I_SUFFIX, ACC_PUBLIC | ACC_STATIC | ACC_FINAL);

        // <init>
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, INIT_M_NAME, INIT_M_DESCR, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, OBJECT_INTERNAL, INIT_M_NAME, INIT_M_DESCR, false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // $create
        {
            String fstParam = hasContext ? CONTEXT_DESCR : "";
            String sndParam = hasDescriptors ? DESCRIPTOR_A_DESCR : "";

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, CREATE_M_NAME, CREATE_M_DESCR, null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, internalNameCdi);
            mv.visitInsn(DUP);
            if (hasContext) {
                mv.visitVarInsn(ALOAD, 1);
            }
            if (hasDescriptors) {
                mv.visitVarInsn(ALOAD, 2);
            }
            mv.visitMethodInsn(INVOKESPECIAL, internalNameCdi, INIT_M_NAME, "(" + fstParam + sndParam + ")V", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        defineClass(cw.toByteArray());
    }

    static class MethodInfo {

        private final String name;
        private final org.objectweb.asm.Type returnType;
        private final List<Class> params = new ArrayList<>();
        private final List<Statement> statements = new ArrayList<>();

        MethodInfo(String name, org.objectweb.asm.Type returnType) {
            this.name = name;
            this.returnType = returnType;
        }

        public void addStatement(Statement statement) {
            statements.add(statement);
        }

        public MethodVisitor visitMethod(ClassWriter cw) {
            org.objectweb.asm.Type[] types = new org.objectweb.asm.Type[params.size()];
            for (int i = 0, size = params.size(); i < size; i++) {
                types[i] = getType(params.get(i));
            }
            return cw.visitMethod(ACC_PUBLIC, name, getMethodDescriptor(returnType, types), null, null);
        }

        public void appendCode(MethodVisitor mv) {
            mv.visitCode();
            for (Statement statement : statements) {
                statement.appendCode(mv);
            }
            if (returnType == VOID_TYPE) {
                mv.visitInsn(RETURN);
            }
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    private static String internalType(Type type) {
        Class clazz;
        if (type instanceof Class) {
            clazz = (Class) type;
        } else if (type instanceof ParameterizedType) {
            clazz = (Class) ((ParameterizedType) type).getRawType();
        } else {
            clazz = Object.class;
        }
        return getDescriptor(clazz);
    }

    public interface Statement {
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
            Class<?> declaringClass = field.getDeclaringClass();
            mv.visitFieldInsn(PUTFIELD, internalType(declaringClass), field.getName(), internalType(field.getType()));
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

    public interface Expression {

        void appendCode(MethodVisitor mv);
    }

    private static class ThisExpression implements Expression {

        @Override
        public void appendCode(MethodVisitor mv) {
            mv.visitVarInsn(ALOAD, 0);
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
        public void appendCode(MethodVisitor mv) {
            if (type == Context.class) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, internalNameCdi, CONTEXT_F_NAME, CONTEXT_DESCR);
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
                    mv.visitFieldInsn(GETFIELD, internalNameCdi, DESCRIPTOR_F_NAME, DESCRIPTOR_A_DESCR);
                    int index = paramsIndex.get(actualTypeVariableName);
                    visitIntConst(mv, index);
                    mv.visitInsn(AALOAD);
                    mv.visitFieldInsn(GETFIELD, DESCRIPTOR_DESCR, DESCRIPTOR_CLASS_F_NAME, CLASS_DESCR);
                    return;
                }
            }

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, internalNameCdi, CONTEXT_F_NAME, CONTEXT_DESCR);
            appendDescriptor(mv, type);
            mv.visitMethodInsn(INVOKEINTERFACE, CONTEXT_INTERNAL, LOOKUP_M_NAME, LOOKUP_M_DESCR, true);
            String internalType = internalType(this.type);
            mv.visitTypeInsn(CHECKCAST, internalType.substring(1, internalType.length() - 1));
        }

        private void appendDescriptor(MethodVisitor mv, Type currentType) {
            if (currentType instanceof TypeVariable) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, internalNameCdi, DESCRIPTOR_F_NAME, DESCRIPTOR_A_DESCR);
                int index = paramsIndex.get(currentType.getTypeName());
                visitIntConst(mv, index);
                mv.visitInsn(AALOAD);
            } else {
                if (currentType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) currentType;
                    Class clazz = (Class) parameterizedType.getRawType();

                    mv.visitLdcInsn(getType(internalType(clazz)));
                    if (clazz.isAnnotationPresent(Scoped.class)) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if (actualTypeArguments.length == 0) {
                            mv.visitMethodInsn(INVOKESTATIC, DESCRIPTOR_INTERNAL, DESCRIPTOR0_M_NAME, DESCRIPTOR0_M_DESCR, false);
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
                                mv.visitFieldInsn(GETFIELD, internalNameCdi, DESCRIPTOR_F_NAME, DESCRIPTOR_A_DESCR);
                            } else {
                                int index = 0, length = actualTypeArguments.length;
                                visitIntConst(mv, length);
                                mv.visitTypeInsn(ANEWARRAY, DESCRIPTOR_INTERNAL);
                                for (Type actualTypeArgument : actualTypeArguments) {
                                    mv.visitInsn(DUP);
                                    visitIntConst(mv, index);
                                    appendDescriptor(mv, actualTypeArgument);
                                    mv.visitInsn(AASTORE);
                                    index++;
                                }
                            }
                            mv.visitMethodInsn(INVOKESTATIC, DESCRIPTOR_INTERNAL, DESCRIPTOR_M_NAME, DESCRIPTOR_M_DESCR, false);
                        }
                    } else {
                        mv.visitMethodInsn(INVOKESTATIC, DESCRIPTOR_INTERNAL, DESCRIPTOR0_M_NAME, DESCRIPTOR0_M_DESCR, false);
                    }
                } else if (currentType instanceof Class) {
                    Class clazz = (Class) currentType;
                    mv.visitLdcInsn(getType(internalType(clazz)));
                    mv.visitMethodInsn(INVOKESTATIC, DESCRIPTOR_INTERNAL, DESCRIPTOR0_M_NAME, DESCRIPTOR0_M_DESCR, false);
                } else {
                    throw new ImpossibleError();
                }
            }
        }
    }

    private static class MethodCallStatement implements Statement {

        private final Method method;
        private final Expression self;
        private final List<Expression> params;

        public MethodCallStatement(Method method, Expression self, List<Expression> params) {
            this.method = method;
            this.self = self;
            this.params = params;
        }

        @Override
        public void appendCode(MethodVisitor mv) {
            self.appendCode(mv);
            for (Expression param : params) {
                param.appendCode(mv);
            }
            Class<?> clazz = method.getDeclaringClass();
            mv.visitMethodInsn(INVOKESPECIAL, getInternalName(clazz), method.getName(),
                    getMethodDescriptor(method), clazz.isInterface());
        }
    }

    private static void visitIntConst(MethodVisitor mv, int index) {
        if (index < 6) {
            mv.visitInsn(ICONST_0 + index);
        } else {
            mv.visitVarInsn(SIPUSH, index);
        }
    }
}
