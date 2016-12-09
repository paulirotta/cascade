/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

import android.support.annotation.NonNull;

/**
 * Create the actual URL at the last minute, just before the request is processed.
 * <p>
 * This may be useful for load balancing between servers or late-prioritizing parameters
 * based on current conditions. For example use this in association with a Collection to
 * prioritize if/which-next at that moment based on current user interface State.
 * <p>
 * Your implementation must be thread safe since multiple WORKER threads may attempt to start
 * network connections simultaneously. The simplest way to do this is mark the method synchronized
 * <p>
 * Return <code>null</code> if no URL should be loaded at this time. Depending on your use case
 * this may signal the end of a collection of URLs to be downloaded.
 *
 * @param <T>
 */
public interface IGettable<T> {
    /**
     * Get the current from of a variable or the next from in a list.
     *
     * @return the current from, or the next from in the series
     * @throws IllegalStateException if the current from is not available. See also {@link ISafeGettable#unsafeGet()}
     */
    @NonNull
    T get();

    /**
     * Must be the {@link Object#toString()} of the from returned by {@link #get()}
     *
     * @return string representation of the current value or {@link IAltFuture.AltFutureState}
     */
    @NonNull
    String toString();
}
