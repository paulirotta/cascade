/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.i;

import android.support.annotation.NonNull;

import com.futurice.cascade.active.IAltFuture;

/**
 * Stop an ongoing activity early if it has not already completed.
 * Long running tasks such as {@link IAltFuture} must
 * periodically check {@link #isCancelled()} and terminate early.
 * <p>
 * This is a cooperative mOnFireAction replacing more agressive techniques such as
 * {@link java.util.concurrent.Future#cancel(boolean)} and {@link java.lang.InterruptedException}
 * which lead to erratic results due to real world limitations such as underlying stateful code
 * which may not clean itself up properly.
 */
public interface ICancellable {
    /**
     * A plain text reason must always be provided for debugOrigin purposes. This is carried forward through whay
     * may be non-obvious asynchronous and active chain results.
     * <p>
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
