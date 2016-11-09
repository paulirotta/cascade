/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.reactive;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IActionOneR;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.util.RCLog;

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
     */
    public ReactiveLong(@NonNull String name) {
        super(name);
    }

    /**
     * Create a new atomic long
     *
     * @param name
     * @param threadType
     * @param inputMapping  a mapping for incoming values, for example <code>l -> Math.max(0, l)</code>
     * @param onErrorAction
     */
    public ReactiveLong(@NonNull String name,
                        @NonNull IThreadType threadType,
                        @Nullable IActionOneR<Long, Long> inputMapping,
                        @NonNull IActionOne<Exception> onErrorAction) {
        super(name, threadType, inputMapping, onErrorAction);
    }

    /**
     * Add two longs in a thread-safe manner
     *
     * @param l
     * @return
     */
    @CallSuper
    public long addAndGet(long l) {
        while (true) {
            long currentValue = get();

            if (compareAndSet(currentValue, currentValue + l)) {
                return currentValue;
            }
            RCLog.d(this, "Collision in concurrent add, will try again: " + currentValue);
        }
    }

    /**
     * Multiply two longs in a thread-safe manner
     *
     * @param l the second operand
     * @return the updated from
     */
    @CallSuper
    public long multiplyAndGet(long l) {
        while (true) {
            final long currentValue = get();
            if (compareAndSet(currentValue, currentValue * l)) {
                return currentValue;
            }
            RCLog.d(this, "Collision in concurrent add, will try again: " + currentValue);
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