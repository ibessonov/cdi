package com.github.ibessonov.cdi.annotations;

import com.github.ibessonov.cdi.enums.Scope;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static com.github.ibessonov.cdi.enums.Scope.STATELESS;
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
