package ibessonov.cdi.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author ibessonov
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface Generic {
}
