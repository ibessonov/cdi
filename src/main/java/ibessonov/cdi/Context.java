package ibessonov.cdi;

import ibessonov.cdi.annotations.Scoped;

import static ibessonov.cdi.enums.Scope.SINGLETON;

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

