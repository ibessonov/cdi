package org.ibess.cdi;

import org.ibess.cdi.runtime.ContextImpl;

/**
 * @author ibessonov
 */
public interface Context {

    /**
     * Retrieve object of given class from context. Generic classes are not supported here
     * @param clazz Class of object to lookup
     * @param <T> Generic parameter for type-safe code
     * @return Retrieved object casted to the required type
     */
    <T> T lookup(Class<T> clazz);

    /**
     * Create new context that is independent from other ones
     * @param extensions Arrays of extensions for this context
     * @return New context
     * @see Extension
     */
    static Context createContext(Extension... extensions) {
        return new ContextImpl(extensions);
    }
}

