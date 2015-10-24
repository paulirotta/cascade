/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.reactive;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;

import java.util.concurrent.atomic.AtomicReference;

import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.vv;

/**
 * Thread-safe reactive display of a variable getValue. Add one or more {@link IActionOne}
 * actions to update the display when the variable fires. Usually these can be added as Lambda expressions
 * referencing the UI element you would like to track the variable's getValue in an eventually-consistent
 * manner.
 * <p>
 * Note the all <code>get()</code>-style actions will return the latest getValue. Therefore asynchronous
 * calls may not result in all values
 * </p>
 * <p>
 * Bindings are thread safe. All mReactiveTargets will refire concurrently if the {@link com.futurice.cascade.i.IThreadType}
 * allows, but individual mReactiveTargets will never be called concurrently or out-of-sequence. Multiple
 * changes to the bound getValue within a short time relative to the current speed of the
 * {@link com.futurice.cascade.i.IThreadType} may coalesce into a single headFunctionalChainLink refire of only
 * the most recent getValue. Bound functions must be idempotent. Repeat firing of the same getValue
 * is filter under most but not all circumstances. This possibility is related to the use of
 * {@link java.lang.ref.WeakReference} of the previously fired getValue of each headFunctionalChainLink to minimize
 * memory load.
 * </p>
 */
@NotCallOrigin
public class ReactiveValue<T> extends Subscription<T, T> implements IReactiveValue<T> {
    private final AtomicReference<T> valueAR = new AtomicReference<>();

    /**
     * Create a new AtomicValue
     *
     * @param name
     * @param initialValue
     */
    public ReactiveValue(
            @NonNull @nonnull final String name,
            @NonNull @nonnull final T initialValue) {
        this(name, initialValue, null, null, null);
    }

    /**
     * Create a new AtomicValue
     *
     * @param name
     * @param initialValue
     * @param threadType
     * @param inputMapping
     * @param onError
     */
    public ReactiveValue(
            @NonNull @nonnull final String name,
            @NonNull @nonnull final T initialValue,
            @Nullable @nullable final IThreadType threadType,
            @Nullable @nullable final IActionOneR<T, T> inputMapping,
            @Nullable @nullable final IOnErrorAction onError) {
        super(name, null, threadType, inputMapping != null ? inputMapping : out -> out, onError);

        set(initialValue);
    }

    /**
     * Run all reactive functional chains bound to this {@link ReactiveValue}.
     * <p>
     * Normally you do not need to call this, it is called for you. Instead, call
     * {@link #set(Object)} to assert a new value.
     * <p>
     * You can also link this to receive multiple reactive updates as a
     * down-chain {@link IReactiveSource#subscribe(IActionOne)}
     * to receive and store reactive values.
     * <p>
     * You can also link into a active chain to receive individually constructed and fired updates using
     * <code>
     * <pre>
     *         myAltFuture.subscribe(value -> myAtomicValue.set(value))
     *     </pre>
     * </code>
     *
     * Both of these methods will automatically call <code>fire()</code> for you.
     *
     * You may want to <code>fire()</code> manually on app startup after all your initial reactive chains are constructed.
     * This will heat up the reactive chain to initial state by flushing current values through the system.
     *
     * All methods and receivers within a reactive chain are <em>supposed</em> to be idempotent to
     * multiple firing events. This
     * does not however mean the calls are free or give a good user experience and value as in the
     * case of requesting data multiple times from a server. You have been warned.
     */
    @NotCallOrigin
    @CallSuper
    public void fire() {
        fire(valueAR.get());
    }

    @CallSuper
    @NonNull
    @nonnull
    @Override // IAtomicValue, IGettable
    public T get() {
        return valueAR.get();
    }

    @CallSuper
    @Override // IAtomicValue
    public boolean set(@NonNull @nonnull final T value) {
        final T previousValue = valueAR.getAndSet(value);
        final boolean valueChanged = !(value == previousValue || value.equals(previousValue) || (previousValue != null && previousValue.equals(value)));

        if (valueChanged) {
            vv(this, mOrigin, "Successful set(" + value + "), about to fire()");
            fire(value);
        } else {
            // The value has not changed
            vv(this, mOrigin, "set() value=" + value + " was already the value, so no change");
        }
        return valueChanged;
    }

    @CallSuper
    @Override // IAtomicValue
    public boolean compareAndSet(@NonNull @nonnull final T expected, @NonNull @nonnull final T update) {
        final boolean success = this.valueAR.compareAndSet(expected, update);

        if (success) {
            vv(this, mOrigin, "Successful compareAndSet(" + expected + ", " + update + ")");
            fire(update);
        } else {
            dd(this, mOrigin, "Failed compareAndSet(" + expected + ", " + update + "). The current value is " + get());
        }

        return success;
    }

    @NonNull
    @nonnull
    @Override // Object
    public String toString() {
        return get().toString();
    }
}
