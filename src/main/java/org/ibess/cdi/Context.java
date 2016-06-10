package org.ibess.cdi;

/**
 * @author ibessonov
 */
public interface Context {

    <T> T lookup(Class<T> clazz);

    static Context createContext() {
        return new ContextImpl();
    }
}

