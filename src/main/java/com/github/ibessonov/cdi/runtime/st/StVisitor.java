package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public interface StVisitor {

    void visitClass(StClass clazz);

    void visitField(StField field);

    void visitMethod(StMethod method);

    void visitScopeStatement(StScopeStatement scopeStatement);

    void visitReturnStatement(StReturnStatement returnStatement);

    void visitFieldAssignmentStatement(StFieldAssignmentStatement fieldAssignmentStatement);

    void visitNullExpression(StNullExpression nullExpression);

    void visitGetParameterExpression(StGetParameterExpression getParameterExpression);

    void visitGetFieldExpression(StGetFieldExpression getFieldExpression);

    void visitThisExpression(StThisExpression thisExpression);

    void visitMethodCallStatement(StMethodCallStatement methodCallStatement);

    void visitClassExpression(StClassExpression classExpression);

    void visitMethodCallExpression(StMethodCallExpression methodCallExpression);

    void visitArrayElementExpression(StArrayElementExpression arrayElementExpression);

    void visitIntConstantExpression(StIntConstantExpression intConstantExpression);

    void visitConstant(StConstant constant);

    void visitCastExpression(StCastExpression castExpression);

    void visitArrayExpression(StArrayExpression arrayExpression);

    void visitDupExpression(StDupExpression dupExpression);

    void visitNewExpression(StNewExpression newExpression);

    void visitNoopStatement(StNoopStatement noopStatement);

    void visitParamAssignmentStatement(StParamAssignmentStatement paramAssignmentStatement);

    void visitSwapExpression(StSwapExpression swapExpression);

    void visitIfStatement(StIfStatement ifStatement);

    void visitInvokeDynamicExpression(StInvokeDynamicExpression invokeDynamicExpression);
}
