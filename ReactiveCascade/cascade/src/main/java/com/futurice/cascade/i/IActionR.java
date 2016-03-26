/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.i;

import android.support.annotation.NonNull;

import java.util.concurrent.Callable;

/**
 * A lambda-friendly functional interface for continuation actions that receive no parameters
 * split return one result.
 *
 * @param <OUT>
 */
public interface IActionR<OUT> extends Callable<OUT>, IBaseAction<OUT> {
    /**
     * @return
     * @throws Exception to transition to {@link com.futurice.cascade.i.IAltFuture.StateError}
     * @throws java.util.concurrent.CancellationException to {@link com.futurice.cascade.i.IAltFuture.StateCancelled}
     */
    @Override // Callable
    @NonNull
    OUT call() throws Exception;
}
