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

import com.futurice.cascade.active.IAltFuture;

/**
 * Stop an ongoing activity early if it has not already completed.
 * Long running tasks such as {@link IAltFuture} must
 * periodically check {@link #isCancelled()} and terminate early.
 *
 * This is a cooperative onFireAction replacing more agressive techniques such as
 * {@link java.util.concurrent.Future#cancel(boolean)} and {@link java.lang.InterruptedException}
 * which lead to erratic results due to real world limitations such as underlying stateful code
 * which may not clean itself up properly.
 */
public interface ICancellable {
    /**
     * A plain text reason must always be provided for debugOrigin purposes. This is carried forward through whay
     * may be non-obvious asynchronous and active chain results.
     *
     * If the operation has already completed normally, this call may be ignored. A warning may be
     * given to help clean up redundant cancellation.
     *
     * @param reason
     * @return
     */
    boolean cancel(@NonNull @nonnull String reason);

    /**
     * A plain text reason and an exception that originally caused the cancellation. This exception
     * may be from upstream and so it the "indirect reason that happened somewhere else for why
     * we enter the cancelled state". We do not enter an error state in response to this call.
     *
     * @param reason
     * @param e
     * @return
     */
    boolean cancel(@NonNull @nonnull String reason, @NonNull @nonnull Exception e);

    /**
     * Check if {@link #cancel(String)} or a similar occurrence such as a {@link java.lang.Exception}
     * have brought the operation to a premature end.
     *
     * @return
     */
    boolean isCancelled();
}
