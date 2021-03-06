package com.github.ibessonov.cdi;

import java.util.function.Supplier;

/**
 * @author ibessonov
 */
@FunctionalInterface
public interface Provider<T> extends Supplier<T> {

    @Override T get();
}
