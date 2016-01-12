package org.ibess.cdi;

/**
 * @author ibessonov
 */
public interface Context {

    <T> T lookup(Class<T> clazz);

    void startRequest();

    void finishRequest();

    static Context create() {
        return new ContextImpl();
    }
}

