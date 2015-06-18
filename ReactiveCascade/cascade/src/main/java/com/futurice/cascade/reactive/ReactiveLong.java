package com.futurice.cascade.reactive;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;

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
     * @param name
     * @param initialValue
     */
    public ReactiveLong(
            @NonNull @nonnull final String name,
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
            @NonNull @nonnull final String name,
            final long initialValue,
            @NonNull @nonnull final IThreadType threadType,
            @Nullable @nullable final IActionOneR<Long, Long> inputMapping,
            @NonNull @nonnull final IOnErrorAction onErrorAction) {
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
        long currentValue;

        for (; ; ) {
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
    @CallSuper
    public long multiplyAndGet(final long l) {
        long currentValue;

        for (; ; ) {
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
    @CallSuper
    public long incrementAndGet() {
        return addAndGet(1);
    }

    /**
     * Decrement the long in a thread-safe manner
     *
     * @return the updated value
     */
    @CallSuper
    public long decrementAndGet() {
        return addAndGet(-1);
    }
}