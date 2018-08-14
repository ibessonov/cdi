package com.github.ibessonov.cdi.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author ibessonov
 */
@Target({PARAMETER, METHOD})
@Retention(RUNTIME)
public @interface Trimmed {
}
