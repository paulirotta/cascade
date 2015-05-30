package com.futurice.cascade.reactive;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.action.IOnErrorAction;

import static com.futurice.cascade.Async.*;

/**
 * A {@link Long} which can be updated in an atomic, thread-safe manner.
 * <p>
 * This is similar to an {@link java.util.concurrent.atomic.AtomicLong} with reactive bindings to
 * get and set the value in reactive chains (function sequences that can fire multiple times).
 * <p>*
 * Created by phou on 30-05-2015.
 */
public class ReactiveLong extends ReactiveValue<Long> {
    /**
     * Create a new atomic long
     *
     * @param threadType
     * @param name
     * @param initialValue
     */
    public ReactiveLong(
            @NonNull final IThreadType threadType,
            @NonNull final String name,
            final long initialValue) {
        super(threadType, name, initialValue);
    }

    /**
     * Create a new atomic long
     *
     * @param threadType
     * @param name
     * @param initialValue
     * @param onError
     */
    public ReactiveLong(
            @NonNull final IThreadType threadType,
            @NonNull final String name,
            final long initialValue,
            @NonNull final IOnErrorAction onError) {
        super(threadType, name, initialValue, onError);
    }

    /**
     * Create a new atomic long
     *
     * @param threadType
     * @param name
     */
    public ReactiveLong(
            @NonNull final IThreadType threadType,
            @NonNull final String name) {
        super(threadType, name);
    }

    /**
     * Create a new atomic long
     *
     * @param threadType
     * @param name
     * @param onError
     */
    public ReactiveLong(
            @NonNull final IThreadType threadType,
            @NonNull final String name,
            @NonNull final IOnErrorAction onError) {
        super(threadType, name, onError);
    }

    /**
     * Add two longs in a thread-safe manner
     *
     * @param l
     * @return
     */
    public long addAndGet(final long l) {
        long currentValue;

        for (;;) {
            currentValue = get();
            if (compareAndSet(currentValue, currentValue + l)) {
                return currentValue;
            }
            ii(this, origin, "Collision in concurrent add, will try again: " + currentValue);
        }
    }

    /**
     * Multiply two longs in a thread-safe manner
     *
     * @param l the second operand
     * @return the updated value
     */
    public long multiplyAndGet(final long l) {
        long currentValue;

        for (;;) {
            currentValue = get();
            if (compareAndSet(currentValue, currentValue * l)) {
                return currentValue;
            }
            ii(this, origin, "Collision in concurrent add, will try again: " + currentValue);
        }
    }

    /**
     * Increment the long in a thread-safe manner
     *
     * @return the updated value
     */
    public long incrementAndGet() {
        return addAndGet(1);
    }

    /**
     * Decrement the long in a thread-safe manner
     *
     * @return the updated value
     */
    public long decrementAndGet() {
        return addAndGet(-1);
    }
}