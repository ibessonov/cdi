package org.ibess.cdi;

import org.ibess.cdi.annotations.Scoped;

import static org.ibess.cdi.enums.Scope.SINGLETON;

/**
 * @author ibessonov
 */
@Scoped(SINGLETON)
public interface Context {

    <T> T lookup(Class<T> clazz);

    void startRequest();

    void finishRequest();

    static Context create() {
        return new ContextImpl();
    }
}

