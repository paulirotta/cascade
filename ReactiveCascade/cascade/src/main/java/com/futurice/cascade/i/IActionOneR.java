/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
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
public interface IActionOneR<IN, OUT> extends IBaseAction<IN> {
    @NonNull
    @nonnull
    OUT call(@NonNull @nonnull IN value) throws Exception;
}
