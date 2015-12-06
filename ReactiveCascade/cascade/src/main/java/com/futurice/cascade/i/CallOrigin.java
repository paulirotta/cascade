/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.i;

import com.futurice.cascade.active.IAltFuture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark this class or method as being particularly interesting as the "originAsync"
 * of an asynchronous operation.
 * <p>
 * It can be difficult to track where an asynchronous operation started since the
 * call stack only shows the synchronous call history and includes a lot of garbage.
 * An attempt is made to store the most interesting "originAsync" of the asynchronous call
 * history in debugOrigin builds. The is the top of the call stack at the point where
 * an {@link IAltFuture} is created.
 * <p>
 * You can influence this call stack selection process by adding this annotation to
 * highlight sections of your own code such as primary business logic that are
 * likely to be useful for debugging if something does go wrong. You can similarly
 * use {@link com.futurice.cascade.i.NotCallOrigin} annotation to de-select
 * classes and methods such as libraries and utilities that are not unique to the
 * start of any one asynchronous operation in your application logic.
 * <p>
 * The selection process is algorithmic and not absolute. If for example no other
 * options match, even {@link com.futurice.cascade.i.NotCallOrigin} items, or
 * classes and methods which have not been tagged, can appear as the async originAsync.
 * <p>
 * This feature has no impact on production builds. For performance, the
 * instrumentation for introspective tracking of async origins is made only in
 * debugOrigin builds.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CallOrigin {
}
