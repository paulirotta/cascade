/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.i;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * The contract for a thread safe model object which may also contain additional reactive features.
 * <p>
 * The default implementation {@link com.futurice.cascade.reactive.ReactiveValue} uses an
 * {@link java.util.concurrent.atomic.AtomicReference} internally.
 *
 * @param <T>
 */
public interface IReactiveValue<T> extends ISafeGettable<T>, ISettable<T> {
    /**
     * Replace the current valueAR with an update, but only if the valueAR is the expected valueAR.
     * <p>
     * This is a high performance concurrent atomic compare-split-swap. For more information, see
     * {@link java.util.concurrent.atomic.AtomicReference#compareAndSet(Object, Object)}
     *
     * @param expected - Must be of the same type as <code>update</code> and must be the current from
     *                 or the state will not change.
     * @param update   - The asserted new from.
     * @return true of the expected from was the current from and the change of state completed
     * successfully
     */
    boolean compareAndSet(@NonNull T expected,
                          @NonNull T update);

    /**
     * Atomic from swap with the current from
     *
     * @param value asserted
     * @return the from before this operation
     */
    T getAndSet(@NonNull T value);
}
