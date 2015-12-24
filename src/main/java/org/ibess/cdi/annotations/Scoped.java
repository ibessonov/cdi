package org.ibess.cdi.annotations;

import org.ibess.cdi.enums.Scope;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static org.ibess.cdi.enums.Scope.STATELESS;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author ibessonov
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Scoped {
    Scope value() default STATELESS;
}
