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

import static com.futurice.cascade.Async.ii;

/**
 * An {@link Integer} which can be updated in an atomic, thread-safe manner.
 * <p>
 * This is similar to an {@link java.util.concurrent.atomic.AtomicInteger} with reactive bindings to
 * get and set the value in reactive chains (function sequences that can fire multiple times).
 * <p>
 * Created by phou on 30-04-2015.
 */
public class ReactiveInteger extends ReactiveValue<Integer> {
    /**
     * Create a new atomic integer
     *
     * @param name
     * @param initialValue
     */
    public ReactiveInteger(
            @NonNull  final String name,
            final int initialValue) {
        super(name, initialValue);
    }

    /**
     * Create a new atomic integer
     *
     * @param threadType
     * @param name
     * @param initialValue
     * @param onFireAction a mapping for incoming values, for example <code>i -> Math.max(0, i)</code>
     * @param onError
     */
    public ReactiveInteger(
            @NonNull  final IThreadType threadType,
            @NonNull  final String name,
            final int initialValue,
            @Nullable  final IActionOneR<Integer, Integer> onFireAction,
            @NonNull  final IOnErrorAction onError) {
        super(name, initialValue, threadType, onFireAction, onError);
    }

    /**
     * Add two integers in a thread-safe manner
     *
     * @param i
     * @return
     */
    @CallSuper
    public int addAndGet(final int i) {
        int currentValue;

        for (; ; ) {
            currentValue = get();
            if (compareAndSet(currentValue, currentValue + i)) {
                return currentValue;
            }
            ii(this, mOrigin, "Collision concurrent add, will try again: " + currentValue);
        }
    }

    /**
     * Multiply two integers in a thread-safe manner
     *
     * @param i
     * @return
     */
    @CallSuper
    public int multiplyAndGet(final int i) {
        int currentValue;

        for (; ; ) {
            currentValue = get();
            if (compareAndSet(currentValue, currentValue * i)) {
                return currentValue;
            }
            ii(this, mOrigin, "Collision concurrent add, will try again: " + currentValue);
        }
    }

    /**
     * Increment the integer in a thread-safe manner
     *
     * @return
     */
    @CallSuper
    public int incrementAndGet() {
        return addAndGet(1);
    }


    /**
     * Decrement the integer in a thread-safe manner
     *
     * @return
     */
    @CallSuper
    public int decrementAndGet() {
        return addAndGet(-1);
    }
}
