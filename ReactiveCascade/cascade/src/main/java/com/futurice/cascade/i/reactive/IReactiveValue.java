/*
 * Copyright (c) 2015 Futurice GmbH. All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.futurice.cascade.i.reactive;

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
     *
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
     * {@link com.futurice.cascade.i.reactive.IReactiveSource}, which will also trigger all
     * down-chain {@link com.futurice.cascade.i.reactive.IReactiveTarget}s to receive the update.
     *
     * Any associated chain will only fire if the value set is new, not a repeat of a previous value.
     *
     * @param value the new value asserted
     * @return <code>true</code> if this is a change from the previous value
     */
    boolean set(@NonNull T value);

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
    boolean compareAndSet(@NonNull T expected, @NonNull T update);
}
