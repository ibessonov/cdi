package org.ibess.cdi;

import org.ibess.cdi.runtime.ContextImpl;

/**
 * @author ibessonov
 */
public interface Context {

    <T> T lookup(Class<T> clazz);

    static Context createContext(Extension... extensions) {
        return new ContextImpl(extensions);
    }
}

