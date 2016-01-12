package org.ibess.cdi.internal;

import org.ibess.cdi.Context;
import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.exceptions.CdiException;

import static org.ibess.cdi.enums.CdiErrorType.GENERIC_PARAMETERS_COUNT_MISMATCH;
import static org.ibess.cdi.internal.$Descriptor.$;

/**
 * Extended {@link Context} functionality for looking up generic classes.
 * For internal purposes only. Must not be used outside of this library's code.
 * @author ibessonov
 */
public interface $Context extends Context {

    Object $lookup($Descriptor d);

    /**
     * {@inheritDoc}
     */
    @Override
    default <T> T lookup(Class<T> clazz) {
        if (clazz.isAnnotationPresent(Scoped.class) && clazz.getTypeParameters().length != 0) {
            throw new CdiException(GENERIC_PARAMETERS_COUNT_MISMATCH,
                    clazz.getCanonicalName(), clazz.getTypeParameters().length, 0);
        }

        return clazz.cast($lookup($(clazz)));
    }
}
