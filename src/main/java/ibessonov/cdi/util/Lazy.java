package ibessonov.cdi.util;

import ibessonov.cdi.annotations.Provided;
import ibessonov.cdi.annotations.Scoped;

import java.util.function.Supplier;

/**
 * @author ibessonov
 */
@Scoped
public abstract class Lazy<T> implements Supplier<T> {

    private T value;

    @Override
    public T get() {
        T val = value;
        if (val == null) {
            val = value = init();
        }
        return val;
    }

    @Provided
    abstract T init();
}
