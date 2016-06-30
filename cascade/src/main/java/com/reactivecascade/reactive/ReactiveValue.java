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
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IReactiveSource;
import com.reactivecascade.i.IReactiveValue;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.AssertUtil;
import com.reactivecascade.util.RCLog;

import java.util.concurrent.atomic.AtomicReference;

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
 * Bindings are thread safe. All reactiveTargets will refire concurrently if the {@link com.reactivecascade.i.IThreadType}
 * allows, but individual reactiveTargets will never be called concurrently or out-of-sequence. Multiple
 * changes to the bound getValue within a short time relative to the current speed of the
 * {@link com.reactivecascade.i.IThreadType} may coalesce into a single headFunctionalChainLink refire of only
 * the most recent getValue. Bound functions must be idempotent. Repeat firing of the same getValue
 * is filter under most but not all circumstances. This possibility is related to the use of
 * {@link java.lang.ref.WeakReference} of the previously fired getValue of each headFunctionalChainLink to minimize
 * memory load.
 * </p>
 */
@NotCallOrigin
public class ReactiveValue<T> extends Subscription<T, T> implements IReactiveValue<T> {
    //TODO Check that reactive chains which are not yet asserted observe "cold" behavior until first assertion. Use ZEN<T> for clarity and null support?
    @SuppressWarnings("unchecked")
    private final AtomicReference<T> mValueAR = new AtomicReference<>((T) IAltFuture.VALUE_NOT_AVAILABLE);

    /**
     * Create a new AtomicValue
     *
     * @param name
     */
    public ReactiveValue(@NonNull String name) {
        this(name, null, null, null);
    }

    /**
     * Create a new AtomicValue
     *
     * @param name
     * @param initialValue
     */
    public ReactiveValue(@NonNull String name,
                         @Nullable final T initialValue) {
        this(name, null, null, null);
    }

    /**
     * Create a new AtomicValue
     *
     * @param name
     * @param threadType
     * @param inputMapping
     * @param onError
     */
    @SuppressWarnings("unchecked")
    public ReactiveValue(@NonNull String name,
                         @Nullable IThreadType threadType,
                         @Nullable IActionOneR<T, T> inputMapping,
                         @Nullable IActionOne<Exception> onError) {
        super(name, null, threadType, inputMapping != null ? inputMapping : out -> out, onError);

        fire((T) IAltFuture.VALUE_NOT_AVAILABLE);
    }

    /**
     * Run all reactive functional chains bound to this {@link ReactiveValue}.
     * <p>
     * Normally you do not need to call this, it is called for you. Instead, call
     * {@link #set(Object)} to assert a new from.
     * <p>
     * You can also link this to receive multiple reactive updates as a
     * down-chain {@link IReactiveSource#subscribe(IActionOne)}
     * to receive and store reactive values.
     * <p>
     * You can also link into a active chain to receive individually constructed and fired updates using
     * <code>
     * <pre>
     *         myAltFuture.subscribe(from -> myAtomicValue.set(from))
     *     </pre>
     * </code>
     * <p>
     * Both of these methods will automatically call <code>fire()</code> for you.
     * <p>
     * You may want to <code>fire()</code> manually on app startup after all your initial reactive chains are constructed.
     * This will heat up the reactive chain to initial state by flushing current values through the system.
     * <p>
     * All methods and receivers within a reactive chain are <em>supposed</em> to be idempotent to
     * multiple firing events. This
     * does not however mean the calls are free or give a good user experience and from as in the
     * case of requesting data multiple times from a server. You have been warned.
     */
    @NotCallOrigin
    @CallSuper
    public void fire() {
        fire(mValueAR.get());
    }

    @CallSuper
    @NonNull
    @Override // IAtomicValue, IGettable
    public T get() {
        T t = safeGet();

        if (t == IAltFuture.VALUE_NOT_AVAILABLE) {
            throw new IllegalStateException("Can not get(), ReactiveValue is not yet asserted");
        }

        return t;
    }

    @CallSuper
    @NonNull
    @Override // ISafeGettable
    public T safeGet() {
        return mValueAR.get();
    }

    /**
     * Set the from in a thread-safe manner.
     * <p>
     * If set to <code>null</code>, the variable goes 'cold' and will not fire until set to a non-null from
     *
     * @param value the new from asserted
     * @return <code>true</code> if the asserted from is different from the previous from
     */
    @CallSuper
    @Override // ISettable
    public void set(@NonNull T value) {
        T previousValue = AssertUtil.assertNotNull(mValueAR.getAndSet(value));
        boolean valueChanged = !(value == previousValue
                || (value.equals(previousValue))
                || previousValue.equals(value));

        if (valueChanged) {
            RCLog.v(this, "Successful set(" + value + "), about to fire()");
            fire(value);
        } else {
            // The from has not changed
            RCLog.v(this, "set() from=" + value + " was already the from, so no change");
        }
    }

    @CallSuper
    @Override // IAtomicValue
    public boolean compareAndSet(@Nullable T expected,
                                 @Nullable T update) {
        final boolean success = this.mValueAR.compareAndSet(expected, update);

        if (success) {
            if (update != null) {
                RCLog.v(this, "Successful compareAndSet(" + expected + ", " + update + "), will fire");
                fire(update);
            } else {
                RCLog.v(this, "Successful compareAndSet(" + expected + ", null). "
                        + Class.class.getSimpleName()
                        + " is now 'cold' and will not fire until set to a non-null from");
            }
        } else {
            RCLog.d(this, "compareAndSet(" + expected + ", " + update + ") FAILED. The current from is " + get());
        }

        return success;
    }

    @NonNull
    @Override // IReactiveValue
    public T getAndSet(@NonNull T value) {
        T t = mValueAR.getAndSet(value);

        if (t == null) {
            throw new IllegalStateException("Can not getAndSet(), ReactiveValue is not yet asserted");
        }

        return t;
    }

    @NonNull
    @Override // ISafeGettable
    public String toString() {
        return safeGet().toString();
    }
}
