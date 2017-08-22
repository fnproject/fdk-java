package com.fnproject.fn.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used on the method of the user function.
 * <p>
 * The annotation must have a 'coercion' argument which specifies the
 * {@link OutputCoercion} class to use to perform the output binding.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OutputBinding {
    /**
     * @return Which coercion class to use for processing the return type of the user function into an OutputEvent.
     */
    Class<? extends OutputCoercion> coercion();
}
