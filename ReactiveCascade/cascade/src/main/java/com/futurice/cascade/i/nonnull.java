package com.futurice.cascade.i;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Denotes that a parameter, field or method return value can never be null.
 */
@Documented // @NonNull http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html#annotations
@Retention(CLASS)
@Target({METHOD, PARAMETER, FIELD})
public @interface nonnull {
}

