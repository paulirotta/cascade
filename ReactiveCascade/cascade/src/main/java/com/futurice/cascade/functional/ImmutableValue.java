/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.functional;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.i.IBaseAction;
import com.futurice.cascade.i.IGettable;
import com.futurice.cascade.i.ISafeGettable;
import com.futurice.cascade.util.RCLog;

import java.util.Iterator;
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
 * Note that there is no mutator method which allows to re-enter the initial {@link SettableAltFuture#ZEN}
 * state once it is lost. Thus one can not abuse the system to make an immutable mutable by moving back
 * through this intermediate state. Once you leave the temple, you can never go back.
 * <p>
 * Note that <code>null</code> is not a permissible from
 * <p>
 * This uses a free thread model (any calling thread is used for all chained functions). This is a
 * main difference from the similar {@link com.futurice.cascade.functional.SettableAltFuture} which forces
 * the specified evaluation thread group.
 *
 * @param <T>
 */
//TODO Do we also need an AddOnlyList type which is a collection that can only grow from the end?
//@Deprecated // Delete this and use SettableAltFuture instead for simplicity
public class ImmutableValue<T> implements ISafeGettable<T> {
    @SuppressWarnings("unchecked")
    protected final T ZEN = (T) AbstractAltFuture.ZEN;

    private final AtomicReference<T> mValueAR = new AtomicReference<>(ZEN); // The "Unasserted" state is different from null
    private final ConcurrentLinkedQueue<IBaseAction<T>> mThenActions = new ConcurrentLinkedQueue<>();
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
     * @param value
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
     * @param expected
     * @param value
     * @return if <code>false</code> is returned, you may try again as the from as needed since the
     * final from has not been set.
     */
    private boolean compareAndSet(@NonNull T expected,
                                  @NonNull T value) {
        boolean success = mValueAR.compareAndSet(expected, value);

        if (success) {
            doThenActions(value);
        } else {
            RCLog.d(this, "compareAndSet(" + expected + ", " + value + ") failed, current from is " + safeGet());
        }

        return success;
    }

    /**
     * Add an mOnFireAction which will be run when {@link #set(Object)} is called.
     * <p>
     * If the from is already {@link #set(Object)}, the passed mOnFireAction will be run immediately and
     * synchronously.
     *
     * @return
     */
    @NonNull
    public ImmutableValue<T> then(@NonNull IActionOne<T> action) {
        mThenActions.add(action);
        if (isSet()) {
            doThenActions(safeGet());
        }
        return this;
    }

    @NonNull
    public ImmutableValue<T> then(@NonNull IAction<T> action) {
        mThenActions.add(action);
        if (isSet()) {
            doThenActions(safeGet());
        }
        return this;
    }

    private void doThenActions(@NonNull T value) {
        final Iterator<IBaseAction<T>> iterator = mThenActions.iterator();

        while (iterator.hasNext()) {
            final IBaseAction<T> action = iterator.next();

            if (mThenActions.remove(action)) {
                try {
                    call(value, action);
                } catch (Exception e) {
                    RCLog.e(this, "Can not do then() actions after ImmutableValue was set to from=" + value, e);
                }
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    // IN->OUT must be bent to match all cases but the mContext makes this safe
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
     * @return
     */
    public final boolean isSet() {
        return mValueAR.get() != AbstractAltFuture.ZEN;
    }

    /**
     * Get the from, or throw {@link java.lang.IllegalStateException} if you are getting the from
     * before it has been set.
     * <p>
     * Generally you want to use this method instead of {@link #safeGet()} when you can use dependency
     * mechanisms properly. If you think have problems, ask if you should be doing some of your logic
     * in a <code>.subscribe()</code> clause to guarantee the execution order.
     *
     * @return
     * @throws IllegalStateException if the supplied lazy evaluation IAction throws an error during evaluation
     */
    @CallSuper
    @NonNull
    @SuppressWarnings("unchecked")
    // The response must be cast because of internal atomic state is a non-T class
    public T get() {
        final T value = mValueAR.get();

        if (value == ZEN) {
            try {
                if (action != null) {
                    final T t = action.call();
                    compareAndSet(ZEN, t);
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
        final T value = mValueAR.get();

        if (value == ZEN) {
            return (T) IAltFuture.VALUE_NOT_AVAILABLE;
        }

        return value;
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
    public T set(@NonNull T value) {
        if (!compareAndSet(ZEN, value)) {
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
     * @return
     */
    @CallSuper
    @NonNull
    public String toString() {
        final T t = safeGet();

        if (t == null) {
            return IGettable.VALUE_NOT_AVAILABLE.toString();
        }

        return t.toString();
    }
}
