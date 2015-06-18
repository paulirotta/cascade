package com.futurice.cascade.util;

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
 * <p>
 * This is strictly to add a notation to JavaDoc and should thus only be used in conjunction with the standard
 * Android annotation <em><code>@Nullable @nullable</code></em>. See <link http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html#annotations>
 *
 * Created by phou on 6/18/2015.
 */
@Documented
@Retention(CLASS)
@Target({METHOD, PARAMETER, FIELD})
public @interface nullable {
}