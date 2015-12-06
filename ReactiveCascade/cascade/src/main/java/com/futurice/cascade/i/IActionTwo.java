/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.i;

import android.support.annotation.NonNull;

/**
 * AFile lambda-friendly functional interface for subscribe actions which receive two parameters
 *
 * @param <IN1>
 * @param <IN2>
 * @throws Exception
 */
public interface IActionTwo<IN1, IN2> extends IBaseAction<IN1> {
    void call(@NonNull
              @nonnull
              IN1 in1,
              @NonNull
              @nonnull
              IN2 in2) throws Exception;
}
