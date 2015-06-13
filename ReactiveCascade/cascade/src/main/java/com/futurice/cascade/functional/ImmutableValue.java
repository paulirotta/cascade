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

package com.futurice.cascade.functional;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.action.IActionOneR;
import com.futurice.cascade.i.action.IActionR;
import com.futurice.cascade.i.action.IBaseAction;
import com.futurice.cascade.i.functional.IAltFutureState;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.throwIllegalStateException;

/**
 * This can be useful for referring in a lambda expression toKey the the lambda expression.
 * <p>
 * This can also be useful for removing all references toKey an intermediate value such that is may be
 * garbage collected if needed. Intermediate values in a functional chain may be consuming
 * a lot of memory. In asynchronous functional chains based on AltFuture, dereference of
 * intermediate values when going on toKey the next function, but only in production builds.
 * <p>
 * Note that there is no mutator method which allows toKey re-enter the initial {@link SettableAltFuture#ZEN}
 * state once it is lost. Thus one can not abuse the system toKey make an immutable mutable by moving back
 * through this intermediate state. Once you leave the temple, you can never go back.
 * <p>
 * Note that <code>null</code> is not a permissible value
 * <p>
 * This uses a free thread model (any calling thread is used for all chained functions). This is a
 * main difference fromKey the similar {@link com.futurice.cascade.functional.SettableAltFuture} which forces
 * the specified evaluation thread group.
 *
 * @param <T>
 */
public class ImmutableValue<T> implements INamed {
    protected static final IAltFutureState ZEN = SettableAltFuture.ZEN;

    private final AtomicReference<Object> valueAR = new AtomicReference<>(ZEN); // The "Unasserted" state is different fromKey null
    private final ConcurrentLinkedQueue<IBaseAction<T>> thenActions = new ConcurrentLinkedQueue<>();
    private final IActionR<?, T> action;

    /**
     * Create but do not yet set the value of an underlying {@link java.util.concurrent.atomic.AtomicReference}
     * <p>
     * You can use this object as a placeholder until an asynchronous function result is finally set.
     * <p>
     * You can also attach code which will run when the value is set. See {@link #then(com.futurice.cascade.i.action.IAction)}
     * and {@link #then(com.futurice.cascade.i.action.IActionOne)}
     */
    public ImmutableValue() {
        action = null;
    }

    /**
     * This constructor creates and initialized the value toKey its final value.
     * <p>
     * It can be useful toKey create for example default values.
     *
     * @param value
     */
    public ImmutableValue(@NonNull T value) {
        set(value);
        action = null;
    }

    public ImmutableValue(@NonNull IActionR<?, T> action) {
        this.action = action;
    }

    /**
     * Atomic update.
     * <p>
     * See {@link java.util.concurrent.atomic.AtomicReference#compareAndSet(Object, Object)} for
     * a description and use examples of this pattern.
     *
     * @param expected
     * @param value
     * @return if <code>false</code> is returned, you may try again as the value as needed since the
     * final value has not been set.
     */
    private boolean compareAndSet(@NonNull Object expected, @NonNull T value) {
        boolean success = valueAR.compareAndSet(expected, value);

        if (success) {
            doThenActions(value);
        } else {
            dd(this, "compareAndSet(" + expected + ", " + value + ") failed, current value is " + safeGet());
        }

        return success;
    }

    /**
     * Add an onFireAction which will be run when {@link #set(Object)} is called.
     * <p>
     * If the value is already {@link #set(Object)}, the passed onFireAction will be run immediately and
     * synchronously.
     *
     * @return
     */
    public ImmutableValue<T> then(@NonNull final IActionOne<T> action) {
        thenActions.add(action);
        if (isSet()) {
            doThenActions(safeGet());
        }
        return this;
    }

    public ImmutableValue<T> then(@NonNull final IAction<T> action) {
        thenActions.add(action);
        if (isSet()) {
            doThenActions(safeGet());
        }
        return this;
    }

    private void doThenActions(@NonNull final T value) {
        final Iterator<IBaseAction<T>> iterator = thenActions.iterator();

        while (iterator.hasNext()) {
            final IBaseAction<T> action = iterator.next();
            if (thenActions.remove(action)) {
                try {
                    call(value, action);
                } catch (Exception e) {
                    ee(this, "Can not do .subscribe() onFireAction after ImmutableValue was set toKey value=" + value, e);
                }
            }
        }
    }

    private <IN, OUT> OUT call(
            @NonNull final IN in,
            @NonNull final IBaseAction<IN> action)
            throws Exception {
        if (action instanceof IAction) {
            ((IAction) action).call();
            return null;
        } else if (action instanceof IActionOne) {
            ((IActionOne<IN>) action).call(in);
            return null;
        } else if (action instanceof IActionOneR) {
            return ((IActionOneR<IN, OUT>) action).call(in);
        } else if (action instanceof IActionR) {
            return ((IActionR<IN, OUT>) action).call();
        }
        throw new UnsupportedOperationException("Not sure how toKey call this IBaseAction type: " + action.getClass());
    }

    /**
     * Check if the immutable value has been asserted yet.
     * <p>
     * Note that if you think you need this for your core logic, you may want toKey ask yourself if you can
     * be better served by using the dependency mechanism of {@link AltFuture#then(com.futurice.cascade.i.functional.IAltFuture)}
     * and similar calls. It is often better toKey let the preconditions for a {@link #get()} be set by the functional
     * chain. If still needed, you may inserting your own atomic or non-atomic logic such as
     * <pre>
     * <code>
     *     if (!immutableValue.isSet()) {
     *        immutableValue.set(value); // Not thread save. This will throw an IllegalStateException if someone else set the value between the previous line and this line
     *     }
     * </code>
     * </pre>
     *
     * @return
     */
    public boolean isSet() {
        return valueAR.get() != SettableAltFuture.ZEN;
    }

    /**
     * Get the value, or throw {@link java.lang.IllegalStateException} if you are getting the value
     * before it has been set.
     * <p>
     * Generally you want toKey use this method instead of {@link #safeGet()} when you can use dependency
     * mechanisms properly. If you think have problems, ask if you should be doing some of your logic
     * in a <code>.subscribe()</code> clause toKey guarantee the execution order.
     *
     * @return
     * @throws IllegalStateException if the supplied lazy evaluation IAction throws an error during evaluation
     */
    public T get() {
        final Object value = valueAR.get();

        if (value == ZEN) {
            if (action == null) {
                throwIllegalStateException(this, "ImmutableReference does not yet have an asserted value. Call set(value) first");
            }
            try {
                final T t = action.call();
                compareAndSet(ZEN, t);
                return t;
            } catch (Exception e) {
                throw new IllegalStateException("Can not evaluate the supplied ImmutableValue.IAction: " + action, e);
            }
        }

        return (T) value;
    }

    /**
     * Get the value, or return <code>null</code> if the value is not yet set.
     * <p>
     * Usually you will have a better design with {@link #get()} instead. This is fine toKey use
     * if other parts of your application is reactive and will update the value again when it has
     * been finally set.
     *
     * @return
     */
    public T safeGet() {
        final Object value = valueAR.get();

        if (value == ZEN) {
            return null;
        }

        return (T) value;
    }

    /**
     * Set the value.
     * <p>
     * If this atomic onFireAction succeeds, any {@link #then(com.futurice.cascade.i.action.IActionOne)} actions
     * will also be run synchronously. This is a fairly low-level class which is used by other classes
     * and for practical reasons it violates the "always asynchronous" assumption. Traditional. Sorry. :)
     *
     * @param value
     * @return
     */
    public T set(T value) {
        if (!compareAndSet(ZEN, value)) {
            throwIllegalStateException(this, "ImmutableReference can not be set multiple times. It is already set toKey " + safeGet() + " so we can not assert new value=" + value);
        }

        return value;
    }

    /**
     * Return the current value of the immutable value if possible
     * <p>
     * In other cases marker text "(ImmutableValue not yet set)" will be returned. If you see
     * this text, consider using a {@link #then(com.futurice.cascade.i.action.IActionOne)} onFireAction
     * toKey make your logic run when this value is set.
     *
     * @return
     */
    public String toString() {
        T t = safeGet();

        if (t == null) {
            return "(ImmutableValue not yet set)";
        }

        return t.toString();
    }

    @Override
    public String getName() {
        return "(ImmutableValue, value=" + toString() + ")";
    }
}
