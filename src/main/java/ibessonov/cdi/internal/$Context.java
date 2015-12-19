package ibessonov.cdi.internal;

import ibessonov.cdi.Context;

/**
 * Interface for internal purposes. Must not be used outside of this library's code.
 * @author ibessonov
 */
public interface $Context extends Context {

    Class<?>[] EMPTY = {};

    <T> T lookup(Class<T> clazz, Class<?> ... params);

    @Override
    default <T> T lookup(Class<T> clazz) {
        return lookup(clazz, EMPTY);
    }
}
