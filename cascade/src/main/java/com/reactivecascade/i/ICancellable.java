/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    boolean cancel(@NonNull String reason);

    /**
     * A reason, such as an error or manual calling of {@link #cancel(String)}, had trigger the transition
     * to cancelled State upchain. This token is synchonrously passed downchain to inform all other chain
     * step so that they clean up (alter external State such as closing resources or changing UI elements).
     * <p>
     * The Reactive Cascade library exposes thread-safe atomic internal states for extension, default implementation
     * replacement and transparency during debugging. Most application developers will not need to use this interface directly.
     */
    @CallOrigin
    boolean cancel(@NonNull StateError stateError);

    /**
     * Check if {@link #cancel(String)} or a similar occurrence such as a {@link java.lang.Exception}
     * have brought the operation to a premature end.
     *
     * @return <code>true</code> if the action has been cancelled to bring it to an alternate termination State
     */
    boolean isCancelled();

    /**
     * An internal-use interface made public to facilitate mixing in alternate implementations.
     * <p>
     * The Reactive Cascade library exposes thread-safe atomic internal states for extension, default implementation
     * replacement and transparency during debugging. Most application developers will not need to use this interface directly.
     */
    @NotCallOrigin
    interface State extends IAsyncOrigin {
    }

    /**
     * This is a marker interface. If you return State information, the atomic inner State of your
     * implementation should implement this interface.
     * <p>
     * The Reactive Cascade library exposes thread-safe atomic internal states for extension, default implementation
     * replacement and transparency during debugging. Most application developers will not need to use this interface directly.
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
         * If the cancellation is because of an error State change elsewhere, provide the details
         * of that original cause also.
         *
         * @return
         */
        @Nullable
        StateError getStateError();
    }

    /**
     * This is a marker interface. If you return State information, the atomic inner State of your
     * implementation should implement this interface.
     * <p>
     * The Reactive Cascade library exposes thread-safe atomic internal states for extension, default implementation
     * replacement and transparency during debugging. Most application developers will not need to use this interface directly.
     */
    @NotCallOrigin
    interface StateError extends State {
        /**
         * Get the exception, if any, which triggered the transition to this internal State
         *
         * @return the exception
         */
        @NonNull
        Exception getException();
    }
}
