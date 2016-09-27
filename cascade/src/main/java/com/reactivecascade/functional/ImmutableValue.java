/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.functional;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.reactivecascade.i.IAction;
import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IActionOneR;
import com.reactivecascade.i.IActionR;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IBaseAction;
import com.reactivecascade.i.ISafeGettable;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This can be useful for referring in a lambda expression to the the lambda expression.
 * <p>
 * This can also be useful for removing all references to an intermediate from such that is may be
 * garbage collected if needed. Intermediate values in a active chain may be consuming
 * a lot of memory. In asynchronous functional chains based on RunnableAltFuture, dereference of
 * intermediate values when going on to the next function, but only in production builds.
 * <p>
 * Note that there is no mutator method which allows to re-enter the initial {@link com.reactivecascade.i.IGettable#VALUE_NOT_AVAILABLE}
 * state once it is lost. Thus one can not abuse the system to make an immutable mutable by moving back
 * through this intermediate state. Once you leave the temple, you can never go back.
 * <p>
 * Note that <code>null</code> is not a permissible from
 * <p>
 * This uses a free thread model (any calling thread is used for all chained functions). This is a
 * main difference from the similar {@link com.reactivecascade.functional.SettableAltFuture} which forces
 * the specified evaluation thread group.
 *
 * @param <T>
 */
//TODO Do we also need an AddOnlyList type which is a collection that can only grow from the end?
//@Deprecated // Delete this and use SettableAltFuture instead for simplicity
public class ImmutableValue<T> implements ISafeGettable<T> {
    private static final String TAG = ImmutableValue.class.getSimpleName();

    @SuppressWarnings("unchecked")
    private final AtomicReference<T> valueAR = new AtomicReference<>((T) VALUE_NOT_AVAILABLE);

    private final ConcurrentLinkedQueue<IBaseAction<T>> thenActions = new ConcurrentLinkedQueue<>();

    @Nullable
    private final IActionR<T> action;

    /**
     * Create but do not yet set the from of an underlying {@link java.util.concurrent.atomic.AtomicReference}
     * <p>
     * You can use this object as a placeholder until an asynchronous function result is finally set.
     * <p>
     * You can also attach code which will run when the from is set. See {@link #then(IAction)}
     * and {@link #then(IActionOne)}
     */
    public ImmutableValue() {
        action = null;
    }

    /**
     * This constructor creates and initialized the from to its final from.
     * <p>
     * It can be useful to create for example default values.
     *
     * @param value the value to immediately set
     */
    public ImmutableValue(@NonNull T value) {
        set(value);
        action = null;
    }

    public ImmutableValue(@Nullable IActionR<T> action) {
        this.action = action;
    }

    /**
     * Atomic update.
     * <p>
     * See {@link java.util.concurrent.atomic.AtomicReference#compareAndSet(Object, Object)} for
     * a description and use examples of this pattern.
     *
     * @param expected value
     * @param value    to set if expected is the current value
     * @return if <code>false</code> is returned, you may try again as the from as needed since the
     * final from has not been set.
     */
    private boolean compareAndSet(@NonNull T expected,
                                  @NonNull T value) {
        boolean success = valueAR.compareAndSet(expected, value);

        if (success) {
            doThenActions(value);
        } else {
            Log.d(TAG, "compareAndSet(" + expected + ", " + value + ") failed, current from is " + safeGet());
        }

        return success;
    }

    /**
     * Add an action which will be run when {@link #set(Object)} is called.
     * <p>
     * If the from is already {@link #set(Object)}, the passed mOnFireAction will be run immediately and
     * synchronously.
     *
     * @return this
     */
    @NonNull
    public ImmutableValue<T> then(@NonNull IActionOne<T> action) {
        thenActions.add(action);
        if (isSet()) {
            doThenActions(safeGet());
        }
        return this;
    }

    /**
     * Add an action which will be run when {@link #set(Object)} is called.
     * <p>
     * If the from is already {@link #set(Object)}, the passed mOnFireAction will be run immediately and
     * synchronously.
     *
     * @return this
     */
    @NonNull
    public ImmutableValue<T> then(@NonNull IAction<T> action) {
        thenActions.add(action);
        if (isSet()) {
            doThenActions(safeGet());
        }
        return this;
    }

    private void doThenActions(@NonNull T value) {
        for (IBaseAction<T> action : thenActions) {
            if (thenActions.remove(action)) {
                try {
                    call(value, action);
                } catch (Exception e) {
                    Log.e(TAG, "Can not do then() actions after ImmutableValue was set to from=" + value, e);
                }
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    // IN->OUT must be bent to match all cases but the context makes this safe
    private <IN, OUT> OUT call(@NonNull final IN in,
                               @NonNull final IBaseAction<IN> action) throws Exception {
        if (action instanceof IAction) {
            ((IAction) action).call();
            return null;
        } else if (action instanceof IActionOne) {
            ((IActionOne<IN>) action).call(in);
            return null;
        } else if (action instanceof IActionOneR) {
            return ((IActionOneR<IN, OUT>) action).call(in);
        } else if (action instanceof IActionR) {
            return ((IActionR<OUT>) action).call();
        }

        throw new UnsupportedOperationException("Not sure how to call this IBaseAction type: " + action.getClass());
    }

    /**
     * Check if the immutable from has been asserted yet.
     * <p>
     * Note that if you think you need this for your core logic, you may want to ask yourself if you can
     * be better served by using the dependency mechanism of {@link RunnableAltFuture#then(IAltFuture)}
     * and similar calls. It is often better to let the preconditions for a {@link #get()} be set by the functional
     * chain. If still needed, you may inserting your own atomic or non-atomic logic such as
     * <pre>
     * <code>
     *     if (!immutableValue.isSet()) {
     *        immutableValue.set(from); // Not thread save. This will throw an IllegalStateException if someone else set the from between the previous line and this line
     *     }
     * </code>
     * </pre>
     *
     * @return <code>true</code> if the value is already asserted
     */
    public final boolean isSet() {
        return valueAR.get() != VALUE_NOT_AVAILABLE;
    }

    /**
     * Get the from, or throw {@link java.lang.IllegalStateException} if you are getting the from
     * before it has been set.
     * <p>
     * Generally you want to use this method instead of {@link #safeGet()} when you can use dependency
     * mechanisms properly. If you think have problems, ask if you should be doing some of your logic
     * in a <code>.sub()</code> clause to guarantee the execution order.
     *
     * @return
     * @throws IllegalStateException if the supplied lazy evaluation IAction throws an error during evaluation
     */
    @CallSuper
    @NonNull
    @SuppressWarnings("unchecked")
    public T get() {
        T value = valueAR.get();

        if (value == VALUE_NOT_AVAILABLE) {
            try {
                if (action != null) {
                    T t = action.call();
                    if (!compareAndSet((T) VALUE_NOT_AVAILABLE, t)) {
                        Log.d(TAG, "ImmutableValue was set while calling action during get: ignoring the second value from action \"" + t + "\" in favor of \"" + valueAR.get() + "\"");
                    }
                    return t;
                }
                throw new IllegalStateException("Null action and from has not been set()");
            } catch (Exception e) {
                throw new IllegalStateException("Can not evaluate the supplied ImmutableValue.IAction", e);
            }
        }

        return value;
    }

    /**
     * Get the from, or return <code>null</code> if the from is not yet set.
     * <p>
     * Usually you will have a better design with {@link #get()} instead. This is fine to use
     * if other parts of your application is reactive and will update the from again when it has
     * been finally set.
     *
     * @return the from, or <code>null</code> if the from has not yet been determined
     */
    @CallSuper
    @SuppressWarnings("unchecked")
    @NonNull
    public T safeGet() {
        return valueAR.get();
    }

    /**
     * Set the from.
     * <p>
     * If this atomic mOnFireAction succeeds, any {@link #then(IActionOne)} actions
     * will also be run synchronously. This is a fairly low-level class which is used by other classes
     * and for practical reasons it violates the "always asynchronous" assumption. Traditional. Sorry. :)
     *
     * @param value
     * @return
     */
    @CallSuper
    @NonNull
    @SuppressWarnings("unchecked")
    public T set(@NonNull T value) {
        if (!compareAndSet((T) VALUE_NOT_AVAILABLE, value)) {
            throw new IllegalStateException("ImmutableReference can not be set multiple times. It is already set to " + safeGet() + " so we can not assert new from=" + value);
        }
        doThenActions(value);

        return value;
    }

    /**
     * Return the current from of the immutable from if possible
     * <p>
     * In other cases marker text "(ImmutableValue not yet set)" will be returned. If you see
     * this text, consider using a {@link #then(IActionOne)} mOnFireAction
     * to make your logic run when this from is set.
     *
     * @return the string representation, or "Value not available" if not yet determined and can not be determined by an on-get action
     */
    @CallSuper
    @NonNull
    public String toString() {
        return safeGet().toString();
    }
}
