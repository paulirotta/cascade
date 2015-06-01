/*
The MIT License (MIT)

Copyright (c) 2015 Futurice Oy and individual contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.futurice.cascade.reactive;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IActionOneR;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.i.reactive.IAtomicValue;

import java.util.concurrent.atomic.AtomicReference;

import static com.futurice.cascade.Async.*;

/**
 * Thread-safe reactive display of a variable getValue. Add one or more {@link com.futurice.cascade.i.action.IActionOne}
 * actions to update the display when the variable fires. Usually these can be added as Lambda expressions
 * referencing the UI element you would like to track the variable's getValue in an eventually-consistent
 * manner.
 * <p>
 * Note the all <code>get()</code>-style actions will return the latest getValue. Therefore asynchronous
 * calls may not result in all values
 * </p>
 * <p>
 * Bindings are thread safe. All reactiveTargets will refire concurrently if the {@link com.futurice.cascade.i.IThreadType}
 * allows, but individual reactiveTargets will never be called concurrently or out-of-sequence. Multiple
 * changes to the bound getValue within a short time relative to the current speed of the
 * {@link com.futurice.cascade.i.IThreadType} may coalesce into a single headFunctionalChainLink refire of only
 * the most recent getValue. Bound functions must be idempotent. Repeat firing of the same getValue
 * is filter under most but not all circumstances. This possibility is related to the use of
 * {@link java.lang.ref.WeakReference} of the previously fired getValue of each headFunctionalChainLink to minimize
 * memory load.
 * </p>
 */
@NotCallOrigin
public class ReactiveValue<T> extends Subscription<T, T> implements IAtomicValue<T> {
    private final AtomicReference<T> valueAR = new AtomicReference<>();

    /**
     * Create a new AtomicValue
     *
     * @param threadType
     * @param name
     * @param initialValue
     */
    public ReactiveValue(
            @NonNull final IThreadType threadType,
            @NonNull final String name,
            @NonNull final T initialValue) {
        this(threadType, name);

        set(initialValue);
    }

    /**
     * Create a new AtomicValue
     *
     * @param threadType
     * @param name
     * @param initialValue
     * @param onError
     */
    public ReactiveValue(
            @NonNull final IThreadType threadType,
            @NonNull final String name,
            @NonNull final T initialValue,
            @NonNull final IOnErrorAction onError) {
        this(threadType, name, onError);

        set(initialValue);
    }

    /**
     * Create a new AtomicValue
     *
     * @param threadType
     * @param name
     */
    public ReactiveValue(@NonNull IThreadType threadType, @NonNull final String name) {
        this(threadType, name, e -> ee(ReactiveValue.class.getSimpleName(), "Problem firing subscription, name=" + name, e));
    }

    /**
     * Create a new AtomicValue
     *
     * @param threadType
     * @param name
     * @param onError
     */
    public ReactiveValue(@NonNull IThreadType threadType, @NonNull String name, @NonNull IOnErrorAction onError) {
        this(threadType, name, onError, out -> out);
    }

    /**
     * Create a new AtomicValue and provide a function which will observe each value right after it is {@link #set(Object)}
     * and before it fires downchain to any linked actions.
     * <p>
     * This validator function can optionally also throw {@link RuntimeException}s
     * or initiate other actions. The validator function controls the values passed down-chain to
     * any reactive subscribers.
     * <p>
     * If you also wish to mutate the value and make the output of the validator function the value you
     * will {@link #get()} from this ReactiveValue, map you should call {@link #set(Object)} in
     * the validator function.
     *
     * @param threadType
     * @param name
     * @param onError
     * @param validator
     */
    public ReactiveValue(
            @NonNull final IThreadType threadType,
            @NonNull final String name,
            @NonNull final IOnErrorAction onError,
            @NonNull final IActionOneR<T, T> validator) {
        super(threadType, name, null, validator, onError);
    }

    /**
     * Run all reactive functional chains bound to this {@link ReactiveValue}.
     * <p>
     * Normally you do not need to call this, it is called for you. Instead, call
     * {@link #set(Object)} to assert a new value.
     * <p>
     * You can also link this to receive multiple reactive updates as a
     * down-chain {@link com.futurice.cascade.i.reactive.IReactiveSource#subscribe(com.futurice.cascade.i.action.IActionOne)}
     * to receive and store reactive values.
     * <p>
     * You can also link into a functional chain to receive individually constructed and fired updates using
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
    public void fire() {
        fire(valueAR.get());
    }

    @Override // IAtomicValue
    @NonNull
    public T get() {
        return valueAR.get();
    }

    @Override // IAtomicValue
    public boolean set(@NonNull final T value) {
        final T previousValue = valueAR.getAndSet(value);
        final boolean valueChanged = !(value == previousValue || value.equals(previousValue) || (previousValue != null && previousValue.equals(value)));

        if (valueChanged) {
            vv(this, origin, "Successful set(" + value + "), about to fire()");
            fire(value);
        } else {
            // The value has not changed
            vv(this, origin, "set() value=" + value + " was already the value, so no change");
        }
        return valueChanged;
    }

    @Override // IAtomicValue
    public boolean compareAndSet(@NonNull final T expected, @NonNull final T update) {
        final boolean success = this.valueAR.compareAndSet(expected, update);

        if (success) {
            vv(this, origin, "Successful compareAndSet(" + expected + ", " + update + ")");
            fire(update);
        } else {
            dd(this, origin, "Failed compareAndSet(" + expected + ", " + update + "). The current value is " + get());
        }

        return success;
    }
}
