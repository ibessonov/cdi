package ibessonov.cdi.util;

import ibessonov.cdi.annotations.Provided;
import ibessonov.cdi.annotations.Scoped;

import java.util.function.Supplier;

/**
 * @author ibessonov
 */
@Scoped
public abstract class Factory<T> implements Supplier<T> {

    @Provided
    public abstract T get();
}
