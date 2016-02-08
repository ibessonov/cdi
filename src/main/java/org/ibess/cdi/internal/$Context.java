package org.ibess.cdi.internal;

import org.ibess.cdi.Context;
import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.exceptions.ImpossibleError;

import static org.ibess.cdi.enums.CdiErrorType.GENERIC_PARAMETERS_COUNT_MISMATCH;
import static org.ibess.cdi.internal.$Descriptor.$0;

/**
 * Extended {@link Context} functionality for looking up generic classes.
 * For internal purposes only. Must not be used outside of this library's code.
 * @author ibessonov
 */
public interface $Context extends Context {

    default Object $lookup($Descriptor d) {
        Scoped scoped = d.c.getAnnotation(Scoped.class);
        if (scoped == null) {
            return $unscoped(d.c);
        } else switch (scoped.value()) {
            case SINGLETON:
                return $singleton(d);
            case STATELESS:
                return $stateless(d);
            case REQUEST:
                return $request(d);
            default: throw new ImpossibleError();
        }
    }

    Object $unscoped(Class c);
    Object $singleton($Descriptor d);
    Object $stateless($Descriptor d);
    Object $request($Descriptor d);

    /**
     * {@inheritDoc}
     */
    @Override
    default <T> T lookup(Class<T> clazz) {
        Scoped scoped = clazz.getAnnotation(Scoped.class);
        if (scoped != null && clazz.getTypeParameters().length != 0) {
            throw new CdiException(GENERIC_PARAMETERS_COUNT_MISMATCH,
                    clazz.getCanonicalName(), clazz.getTypeParameters().length, 0);
        }

        if (scoped == null) {
            return clazz.cast($unscoped(clazz));
        } else switch (scoped.value()) {
            case SINGLETON:
                return clazz.cast($singleton($0(clazz)));
            case STATELESS:
                return clazz.cast($stateless($0(clazz)));
            case REQUEST:
                return clazz.cast($request($0(clazz)));
            default: throw new ImpossibleError();
        }
    }
}
