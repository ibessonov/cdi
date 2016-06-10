package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StRecursiveVisitor implements StVisitor {

    @Override
    public void visitClass(StClass clazz) {
        for (StField field : clazz.fields) {
            field.accept(this);
        }
        for (StMethod method : clazz.methods) {
            method.accept(this);
        }
    }

    @Override
    public void visitField(StField field) {
    }

    @Override
    public void visitMethod(StMethod method) {
        method.statement.accept(this);
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
    }

    @Override
    public void visitFieldAssignmentStatement(StFieldAssignmentStatement fieldAssignmentStatement) {
        fieldAssignmentStatement.left.accept(this);
        fieldAssignmentStatement.right.accept(this);
    }

    @Override
    public void visitNullExpression(StNullExpression nullExpression) {
    }

    @Override
    public void visitGetParameterExpression(StGetParameterExpression getParameterExpression) {
    }

    @Override
    public void visitGetFieldExpression(StGetFieldExpression getFieldExpression) {
        getFieldExpression.left.accept(this);
    }

    @Override
    public void visitThisExpression(StThisExpression thisExpression) {
    }

    @Override
    public void visitMethodCallStatement(StMethodCallStatement methodCallStatement) {
        methodCallStatement.methodCallExpression.accept(this);
    }

    @Override
    public void visitClassExpression(StClassExpression classExpression) {
    }

    @Override
    public void visitMethodCallExpression(StMethodCallExpression methodCallExpression) {
        methodCallExpression.object.accept(this);
        for (StExpression parameter : methodCallExpression.parameters) {
            parameter.accept(this);
        }
    }

    @Override
    public void visitArrayElementExpression(StArrayElementExpression arrayElementExpression) {
        arrayElementExpression.array.accept(this);
        arrayElementExpression.index.accept(this);
    }

    @Override
    public void visitIntConstantExpression(StIntConstantExpression intConstantExpression) {
    }

    @Override
    public void visitCastExpression(StCastExpression castExpression) {
        castExpression.expression.accept(this);
    }

    @Override
    public void visitArrayExpression(StArrayExpression arrayExpression) {
        for (StExpression element : arrayExpression.elements) {
            element.accept(this);
        }
    }

    @Override
    public void visitDupExpression(StDupExpression dupExpression) {
        dupExpression.expression.accept(this);
    }

    @Override
    public void visitNewExpression(StNewExpression newExpression) {
    }

    @Override
    public void visitNoopStatement(StNoopStatement noopStatement) {
    }
}