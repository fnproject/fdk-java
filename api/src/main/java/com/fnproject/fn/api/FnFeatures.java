package com.fnproject.fn.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used in user function classes to enable runtime-wide feature.
 *
 * Runtime features are initialized at the point that the function class is loaded but prior to the call chain.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FnFeatures {
   FnFeature[] value();
}
