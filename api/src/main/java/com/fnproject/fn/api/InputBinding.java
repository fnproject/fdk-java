package com.fnproject.fn.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used on a parameter of the user function.
 *
 * The annotation must have a 'coercion' argument which specifies the
 * {@link InputCoercion} class to use to perform the input binding.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface InputBinding {
    /**
     * Which coercion class to use for processing an InputEvent into
     * the type required by the function parameter.
     *
     * @return the{@link InputCoercion} class used to perform the input binding
     */
    Class<? extends InputCoercion> coercion();
}
