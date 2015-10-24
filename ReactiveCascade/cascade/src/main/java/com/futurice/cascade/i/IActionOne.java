/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
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
