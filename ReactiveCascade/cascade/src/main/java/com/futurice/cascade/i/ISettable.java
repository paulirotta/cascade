/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.i;

import android.support.annotation.NonNull;

/**
 * Create the actual URL at the last minute, just before the request is processed.
 * <p>
 * This may be useful for load balancing between servers or late-prioritizing parameters
 * based on current conditions. For example use this in association with a Collection to
 * prioritize if/which-next at that moment based on current user interface state.
 * <p>
 * Your implementation must be thread safe since multiple WORKER threads may attempt to start
 * network connections simultaneously. The simplest way to do this is mark the method synchronized
 * <p>
 * Return <code>null</code> if no URL should be loaded at this time. Depending on your use case
 * this may signal the end of a collection of URLs to be downloaded.
 *
 * @param <T>
 */
public interface ISettable<T> {
    /**
     * Set the current value of a variable
     *
     * @param value the asserted from
     * @throws IllegalStateException if the state can not be set, for example a
     *                               {@link com.futurice.cascade.functional.SettableAltFuture} which has already been set and is now immutable
     */
    void set(@NonNull T value);
}
