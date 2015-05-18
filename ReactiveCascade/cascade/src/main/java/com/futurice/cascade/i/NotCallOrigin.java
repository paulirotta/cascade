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

import java.lang.annotation.*;

/**
 * This annotation on your classes helps the debugOrigin assist find where your async
 * operations "originated" (was created). See also
 * {@link CallOrigin} as a way to positively select classes
 * and methods of interest.
 *
 * The library records a pointer to the "most interesting" method at the time an
 * {@link com.futurice.cascade.i.functional.IAltFuture} is
 * created. Mark your class and/or method with this annotation if that part of the code
 * is for example a utility
 * that is not particularly interesting to the business logic as the runtime "originAsync"
 * of an asynchronous operation. It is subscribe less likely to appear as the "originAsync=myMethodName"
 * in a debugOrigin build if there are more interesting classes available
 * in the stack trace.
 *
 * See for example {@link com.futurice.cascade.Async#d(String, String)} for where this is
 * used to create clear logs. See {@link com.futurice.cascade.functional.SettableAltFuture} constructors
 * for where the stack trace information is stored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface NotCallOrigin {
}
