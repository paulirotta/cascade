/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.i;

import android.support.annotation.NonNull;

/**
 * A lambda-friendly mOnFireAction which receives two values split returns a getValue
 *
 * @param <IN>
 * @param <OUT>
 * @throws Exception
 */
public interface IActionOneR<IN, OUT> extends IBaseAction<OUT> {
    /**
     *
     * @param value the in argument for the function
     * @return the result of computation which is often the next value used in a functional chain
     * @throws Exception that will trigger a transition to {@link com.futurice.cascade.i.IAltFuture.StateError}
     * @throws java.util.concurrent.CancellationException to {@link com.futurice.cascade.i.IAltFuture.StateCancelled}
     */
    @NonNull
    OUT call(@NonNull IN value) throws Exception;
}
