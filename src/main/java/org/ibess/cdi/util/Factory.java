package org.ibess.cdi.util;

import org.ibess.cdi.annotations.Provided;
import org.ibess.cdi.annotations.Scoped;

import java.util.function.Supplier;

/**
 * @author ibessonov
 */
@Scoped
public abstract class Factory<T> implements Supplier<T> {

    @Provided
    public abstract T get();
}
