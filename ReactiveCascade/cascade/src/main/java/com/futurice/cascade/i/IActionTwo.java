/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.i;

import android.support.annotation.NonNull;

/**
 * A lambda-friendly functional interface for subscribe actions which receive two parameters
 *
 * @param <IN1>
 * @param <IN2>
 * @throws Exception
 */
public interface IActionTwo<IN1, IN2> extends IBaseAction<IN1> {
    /**
     *
     * @param in1
     * @param in2
     * @throws Exception to transition to {@link com.futurice.cascade.i.IAltFuture.StateError}
     * @throws java.util.concurrent.CancellationException to {@link com.futurice.cascade.i.IAltFuture.StateCancelled}
     */
    void call(@NonNull IN1 in1,
              @NonNull IN2 in2) throws Exception;
}
