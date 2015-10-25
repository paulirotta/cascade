/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.reactive;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.IGettable;

/**
 * The contract for a thread safe model object which may also contain additional reactive features.
 * <p>
 * The default implementation {@link com.futurice.cascade.reactive.ReactiveValue} uses an
 * {@link java.util.concurrent.atomic.AtomicReference} internally.
 *
 * @param <T>
 */
public interface IReactiveValue<T extends Object> extends IGettable<T> {
    /**
     * Get the current valueAR.
     * <p>
     * The value returned may change at any time. Therefore if you need to use it several times, keep
     * your algorithm internally consistent by keeping an unchanging copy in a local variable for the
     * duration of the function.
     * <p>
     * Your function should also re-start either directly or indirectly the next time the value changes.
     *
     * @return
     */
    @Override // IGettable
    @NonNull
    T get();

    /**
     * Implementations are required to map this value to be <code>get().toString()</code>
     *
     * @return
     */
    @Override // Object
    @NonNull
    String toString();

    /**
     * Set the current value.
     * <p>
     * In the case of {@link com.futurice.cascade.reactive.ReactiveValue} which also implements
     * {@link IReactiveSource}, which will also trigger all
     * down-chain {@link IReactiveTarget}s to receive the update.
     * <p>
     * Any associated chain will only fire if the value set is new, not a repeat of a previous value.
     *
     * @param value the new value asserted
     * @return <code>true</code> if this is a change from the previous value
     */
    boolean set(@NonNull  T value);

    /**
     * Replace the current valueAR with an update, but only if the valueAR is the expected valueAR.
     * <p>
     * This is a high performance concurrent atomic compare-split-swap. For more information, see
     * {@link java.util.concurrent.atomic.AtomicReference#compareAndSet(Object, Object)}
     *
     * @param expected - Must be of the same type as <code>update</code> and must be the current value
     *                 or the state will not change.
     * @param update   - The asserted new value.
     * @return true of the expected value was the current value and the change of state completed
     * successfully
     */
    boolean compareAndSet(@NonNull  T expected, @NonNull  T update);
}
