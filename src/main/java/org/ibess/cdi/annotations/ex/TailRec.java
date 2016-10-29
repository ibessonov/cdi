package org.ibess.cdi.annotations.ex;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Not safe for general usage. Be sure that you know what you're doing.<br/>
 * Incorrect structure of recursive method may lead to invalid bytecode or unexpected behavior.
 * @author ibessonov
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface TailRec {
}
