package com.github.ibessonov.cdi.internal;

import com.github.ibessonov.cdi.Context;
import com.github.ibessonov.cdi.enums.Scope;
import com.github.ibessonov.cdi.exceptions.CdiException;
import com.github.ibessonov.cdi.exceptions.ImpossibleError;

import static com.github.ibessonov.cdi.enums.CdiErrorType.GENERIC_PARAMETERS_COUNT_MISMATCH;
import static com.github.ibessonov.cdi.internal.$Descriptor.$0;
import static com.github.ibessonov.cdi.util.ScopedAnnotationCache.getScope;

/**
 * Extended {@link Context} functionality for looking up generic classes.
 * For internal purposes only. Must not be used outside of this library's code.
 * @author ibessonov
 */
public interface $Context extends Context {

    default <T> T $lookup($Descriptor<T> d) {
        Scope scope = getScope(d.c);
        if (scope == null) {
            return $unscoped(d.c);
        } else switch (scope) {
            case SINGLETON:
                return $singleton(d);
            case STATELESS:
                return $stateless(d);
            default: throw new ImpossibleError();
        }
    }

    <T> T $unscoped(Class<T> c);
    <T> T $singleton($Descriptor<T> d);
    <T> T $stateless($Descriptor<T> d);

    /**
     * {@inheritDoc}
     */
    @Override
    default <T> T lookup(Class<T> clazz) {
        Scope scope = getScope(clazz);
        if (scope != null && clazz.getTypeParameters().length != 0) {
            throw new CdiException(GENERIC_PARAMETERS_COUNT_MISMATCH,
                    clazz.getCanonicalName(), clazz.getTypeParameters().length, 0);
        }

        if (scope == null) {
            return clazz.cast($unscoped(clazz));
        } else switch (scope) {
            case SINGLETON:
                return clazz.cast($singleton($0(clazz)));
            case STATELESS:
                return clazz.cast($stateless($0(clazz)));
            default: throw new ImpossibleError();
        }
    }
}
