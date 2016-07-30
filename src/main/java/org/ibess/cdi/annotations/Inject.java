package org.ibess.cdi.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author ibessonov
 */
@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
public @interface Inject {

}
