/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

import android.support.annotation.NonNull;

/**
 * Stop an ongoing activity early if it has not already completed.
 * Cancellation is cooperative. Long running chain steps should
 * periodically check {@link #isCancelled()} and terminate early.
 * <p>
 * This is not an interrupt. Non-cooperative techniques are discouraged in modern async Java development
 * as they cause complications in concurrency design that may leave internal State and external side
 * effects undetermined.
 */
public interface ICancellable {
    /**
     * A plain text reason must always be provided for debugOrigin purposes. This is carried forward through whay
     * may be non-obvious asynchronous and active chain results.
     * <p>
     * If the operation has already completed normally, this call may be ignored. A warning may be
     * given to help clean up redundant cancellation.
     *
     * @param reason for easy debugging
     * @return <code>true</code> if the action was cancelled, otherwise this call had not effect
     */
    @CallOrigin
    boolean cancel(@NonNull CharSequence reason);

    /**
     * Check if {@link #cancel(CharSequence)} or a similar occurrence such as a {@link java.lang.Exception}
     * have brought the operation to a premature end.
     *
     * @return <code>true</code> if the action has been cancelled to bring it to an alternate termination State
     */
    boolean isCancelled();
}
