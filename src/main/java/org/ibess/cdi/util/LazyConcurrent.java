package org.ibess.cdi.util;

import org.ibess.cdi.annotations.Provided;
import org.ibess.cdi.annotations.Scoped;

import java.util.function.Supplier;

/**
 * @author ibessonov
 */
@Scoped
public abstract class LazyConcurrent<T> implements Supplier<T> {

    private volatile T value;

    @Override
    public T get() {
        T val = value;
        if (val == null) {
            synchronized (this) {
                val = value;
                if (val == null) {
                    value = val = init();
                }
            }
        }
        return val;
    }

    @Provided
    protected abstract T init();
}
