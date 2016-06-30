/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

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
     * Execute the action
     *
     * @return the result of the action
     * @throws Exception                                  to transition to {@link com.reactivecascade.i.IAltFuture.StateError}
     * @throws java.util.concurrent.CancellationException to {@link com.reactivecascade.i.IAltFuture.StateCancelled}
     */
    @Override // Callable
    @NonNull
    OUT call() throws Exception;
}
