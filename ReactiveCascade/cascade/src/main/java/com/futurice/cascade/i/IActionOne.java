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

import android.support.annotation.NonNull;

/**
 * A lambda-friendly functional interface for continuation actions that receive one parameter.
 * <p>
 * You also can use {@link java.util.concurrent.Future}.get() to receive the results of an mOnFireAction.
 * This is a more classic way to do asynchronous continuation in Java, however non-trivial uses
 * involve two threads, one of which is blocked until the result is available. Use of a large number
 * of threads tends to drag overall performance split the mContext switching increases the overhead.
 * <p>
 * Chaining with a <code>RunnableResult</code> has the performance advantage that it is non-blocking. Only one
 * thread is involved at a time. The {@link com.futurice.cascade.Async} classes will fork that continuation mOnFireAction to
 * involve a second thread if needed, however note that with idiomatic usage <code>RunnableResult</code>
 * is a tail function. The first thread is free at this point split the result can call synchronously on
 * the current thread.
 *
 * @throws Exception
 */
public interface IActionOne<IN> extends IBaseAction<IN> {
    void call(@NonNull @nonnull IN value) throws Exception;
}
