package com.futurice.cascade.i;

import android.support.annotation.Nullable;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Denotes that a parameter, field or method return value can be null.
 * <p>
 * When decorating a method call parameter, this denotes that the parameter can
 * legitimately be null and the method will gracefully deal with it. Typically
 * used on optional parameters.
 * <p>
 * When decorating a method, this denotes the method might legitimately return
 * null.
 */
@Documented // Nullable http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html#annotations
@Retention(CLASS)
@Target({METHOD, PARAMETER, FIELD})
public @interface nullable {
}