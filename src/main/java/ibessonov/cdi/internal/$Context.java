package ibessonov.cdi.internal;

import ibessonov.cdi.Context;
import ibessonov.cdi.annotations.Scoped;
import ibessonov.cdi.exceptions.CdiException;
import ibessonov.cdi.reflection.Descriptor;

import static ibessonov.cdi.enums.CdiErrorType.GENERIC_PARAMETERS_COUNT_MISMATCH;
import static ibessonov.cdi.reflection.Descriptor.$$;

/**
 * Interface for internal purposes. Must not be used outside of this library's code.
 * Signature might be changed at any time.
 * @author ibessonov
 */
public interface $Context extends Context {

    <T> T lookup(Descriptor<T> d);

    @Override
    default <T> T lookup(Class<T> clazz) {
        if (clazz.isAnnotationPresent(Scoped.class) && clazz.getTypeParameters().length != 0) {
            throw new CdiException(GENERIC_PARAMETERS_COUNT_MISMATCH,
                    clazz.getCanonicalName(), clazz.getTypeParameters().length, 0);
        }

        return lookup($$(clazz));
    }
}
