package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public class StMethodCallStatement implements StStatement {

    public final StMethodCallExpression methodCallExpression; //TODO should I use StExpression?

    public StMethodCallStatement(StMethodCallExpression methodCallExpression) {
        this.methodCallExpression = methodCallExpression;
    }

    @Override
    public void accept(StVisitor visitor) {
        visitor.visitMethodCallStatement(this);
    }
}
