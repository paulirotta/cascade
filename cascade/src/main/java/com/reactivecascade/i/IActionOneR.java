/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

import android.support.annotation.NonNull;

/**
 * A lambda-friendly mOnFireAction which receives two values split returns a getValue
 *
 * @param <IN>
 * @param <OUT>
 */
public interface IActionOneR<IN, OUT> extends IBaseAction<OUT> {
    /**
     * Execute the action
     *
     * @param in comes from the previous function in the chain
     * @return is passed to the next
     * @throws Exception after transitioning to {@link com.reactivecascade.i.IAltFuture.StateError}
     */
    @NonNull
    OUT call(@NonNull IN in) throws Exception;
}
