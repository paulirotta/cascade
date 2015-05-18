package com.futurice.cascade.systemtest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method for running with the system tester
 * <p>
 * Note that marking classes in a similar manner is not supported. You must list classes manually
 * and pass them in. The DEX class loader makes automating this a bit difficult and perhaps unreliable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Test {
}
