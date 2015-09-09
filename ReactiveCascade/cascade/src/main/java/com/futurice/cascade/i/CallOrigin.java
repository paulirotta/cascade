/*
The MIT License (MIT)

Copyright (c) 2015 Futurice Oy and individual contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
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
 *
 * It can be difficult to track where an asynchronous operation started since the
 * call stack only shows the synchronous call history and includes a lot of garbage.
 * An attempt is made to store the most interesting "originAsync" of the asynchronous call
 * history in debugOrigin builds. The is the top of the call stack at the point where
 * an {@link IAltFuture} is created.
 *
 * You can influence this call stack selection process by adding this annotation to
 * highlight sections of your own code such as primary business logic that are
 * likely to be useful for debugging if something does go wrong. You can similarly
 * use {@link com.futurice.cascade.i.NotCallOrigin} annotation to de-select
 * classes and methods such as libraries and utilities that are not unique to the
 * start of any one asynchronous operation in your application logic.
 *
 * The selection process is algorithmic and not absolute. If for example no other
 * options match, even {@link com.futurice.cascade.i.NotCallOrigin} items, or
 * classes and methods which have not been tagged, can appear as the async originAsync.
 *
 * This feature has no impact on production builds. For performance, the
 * instrumentation for introspective tracking of async origins is made only in
 * debugOrigin builds.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CallOrigin {
}
