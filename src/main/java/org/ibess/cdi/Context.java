package org.ibess.cdi;

/**
 * @author ibessonov
 */
public interface Context {

    <T> T lookup(Class<T> clazz);

    void cleanupThreadLocals();

    static Context create() {
        return new ContextImpl();
    }
}

