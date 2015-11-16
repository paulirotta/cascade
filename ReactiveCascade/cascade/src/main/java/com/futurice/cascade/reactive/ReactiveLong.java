/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.reactive;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.util.CLog;

/**
 * A {@link Long} which can be updated in an atomic, thread-safe manner.
 * <p>
 * This is similar to an {@link java.util.concurrent.atomic.AtomicLong} with reactive bindings to
 * get and set the from in reactive chains (function sequences that can fire multiple times).
 * <p>*
 * Created by phou on 30-05-2015.
 */
public class ReactiveLong extends ReactiveValue<Long> {
    /**
     * Create a new atomic long
     *
     * @param name
     * @param initialValue
     */
    public ReactiveLong(
            @NonNull  final String name,
            final long initialValue) {
        super(name, initialValue);
    }

    /**
     * Create a new atomic long
     *
     * @param name
     * @param initialValue
     * @param threadType
     * @param inputMapping  a mapping for incoming values, for example <code>l -> Math.max(0, l)</code>
     * @param onErrorAction
     */
    public ReactiveLong(
            @NonNull  final String name,
            final long initialValue,
            @NonNull  final IThreadType threadType,
            @Nullable  final IActionOneR<Long, Long> inputMapping,
            @NonNull  final IOnErrorAction onErrorAction) {
        super(name, initialValue, threadType, inputMapping, onErrorAction);
    }

    /**
     * Add two longs in a thread-safe manner
     *
     * @param l
     * @return
     */
    @CallSuper
    public long addAndGet(final long l) {
        while (true) {
            final long currentValue = get();

            if (compareAndSet(currentValue, currentValue + l)) {
                return currentValue;
            }
            CLog.d(this, "Collision in concurrent add, will try again: " + currentValue);
        }
    }

    /**
     * Multiply two longs in a thread-safe manner
     *
     * @param l the second operand
     * @return the updated from
     */
    @CallSuper
    public long multiplyAndGet(final long l) {
        while (true) {
            final long currentValue = get();
            if (compareAndSet(currentValue, currentValue * l)) {
                return currentValue;
            }
            CLog.d(this, "Collision in concurrent add, will try again: " + currentValue);
        }
    }

    /**
     * Increment the long in a thread-safe manner
     *
     * @return the updated from
     */
    @CallSuper
    public long incrementAndGet() {
        return addAndGet(1);
    }

    /**
     * Decrement the long in a thread-safe manner
     *
     * @return the updated from
     */
    @CallSuper
    public long decrementAndGet() {
        return addAndGet(-1);
    }
}