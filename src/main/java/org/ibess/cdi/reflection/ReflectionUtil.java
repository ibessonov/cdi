package org.ibess.cdi.reflection;

import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.exceptions.ImpossibleError;

import static org.ibess.cdi.enums.CdiErrorType.ILLEGAL_ACCESS;

/**
 * @author ibessonov
 */
public final class ReflectionUtil {

    public static <T> T newInstance(Class<? extends T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException ie) {
            throw new ImpossibleError(ie);
        } catch (IllegalAccessException iae) {
            throw new CdiException(iae, ILLEGAL_ACCESS);
        }
    }
}
