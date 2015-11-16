/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.i;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Stop an ongoing activity early if it has not already completed.
 * Long running tasks such as {@link IAltFuture} must
 * periodically check {@link #isCancelled()} and terminate early.
 * <p>
 * This is not an interrupt, it is a cooperative contract. Non-cooperative techniques are discouraged
 * as they cause complications in concurrency and leave internal state and external side effects undetermined.
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
    @CallOrigin
    boolean cancel(@NonNull String reason);

    /**
     * A plain text reason and an exception that originally caused the cancellation. This exception
     * may be from upstream and so it the "indirect reason that happened somewhere else for why
     * we enter the cancelled state". We do not enter an error state in response to this call.
     *
     */
    @CallOrigin
    boolean cancel(@NonNull StateError stateError);

    /**
     * Check if {@link #cancel(String)} or a similar occurrence such as a {@link java.lang.Exception}
     * have brought the operation to a premature end.
     *
     * @return
     */
    boolean isCancelled();

    /**
     * An internal-use interface made public to facilitate mixing in alternate implementations.
     *
     * The default implementations provided by the Reactive Cascade library do not expose internal state
     * and normal application developers will not see or need this interface.
     */
    @NotCallOrigin
    interface State extends IAsyncOrigin {
    }

    /**
     * This is a marker interface. If you return state information, the atomic inner state of your
     * implementation should implement this interface.
     */
    @NotCallOrigin
    interface StateCancelled extends State {
        /**
         * The reason this task was cancelled. This is for debug purposes.
         *
         * @return
         */
        @NonNull
        String getReason();

        /**
         * If the cancellation is because of an error state change elsewhere, provide the details
         * of that original cause also.
         *
         * @return
         */
        @Nullable
        StateError getStateError();
    }

    /**
     * This is a marker interface. If you return state information, the atomic inner state of your
     * implementation should implement this interface.
     */
    @NotCallOrigin
    interface StateError extends State {
        /**
         * Get the exception, if any, which triggered the transition to this internal state
         *
         * @return the exception
         */
        @NonNull
        Exception getException();
    }
}
