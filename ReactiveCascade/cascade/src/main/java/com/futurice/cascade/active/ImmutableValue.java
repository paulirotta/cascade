/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.active;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IBaseAction;
import com.futurice.cascade.i.IGettable;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.throwIllegalStateException;

/**
 * This can be useful for referring in a lambda expression to the the lambda expression.
 * <p>
 * This can also be useful for removing all references to an intermediate value such that is may be
 * garbage collected if needed. Intermediate values in a active chain may be consuming
 * a lot of memory. In asynchronous functional chains based on AltFuture, dereference of
 * intermediate values when going on to the next function, but only in production builds.
 * <p>
 * Note that there is no mutator method which allows to re-enter the initial {@link SettableAltFuture#ZEN}
 * state once it is lost. Thus one can not abuse the system to make an immutable mutable by moving back
 * through this intermediate state. Once you leave the temple, you can never go back.
 * <p>
 * Note that <code>null</code> is not a permissible value
 * <p>
 * This uses a free thread model (any calling thread is used for all chained functions). This is a
 * main difference fromKey the similar {@link com.futurice.cascade.active.SettableAltFuture} which forces
 * the specified evaluation thread group.
 *
 * @param <T>
 */
//TODO Do we also need an AddOnlyList type which is a collection that can only grow value the end?
//@Deprecated // Delete this and use SettableAltFuture instead for simplicity
public class ImmutableValue<T extends Object> implements IGettable<T> {
    protected static final IAltFutureState ZEN = SettableAltFuture.ZEN;

    private final AtomicReference<Object> mValueAR = new AtomicReference<>(ZEN); // The "Unasserted" state is different fromKey null
    private final ConcurrentLinkedQueue<IBaseAction<T>> mThenActions = new ConcurrentLinkedQueue<>();
    @Nullable
    private final IActionR<?, T> action;

    /**
     * Create but do not yet set the value of an underlying {@link java.util.concurrent.atomic.AtomicReference}
     * <p>
     * You can use this object as a placeholder until an asynchronous function result is finally set.
     * <p>
     * You can also attach code which will run when the value is set. See {@link #then(IAction)}
     * and {@link #then(IActionOne)}
     */
    public ImmutableValue() {
        action = null;
    }

    /**
     * This constructor creates and initialized the value to its final value.
     * <p>
     * It can be useful to create for example default values.
     *
     * @param value
     */
    public ImmutableValue(@NonNull  T value) {
        set(value);
        action = null;
    }

    public ImmutableValue(@Nullable  IActionR<?, T> action) {
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
    private boolean compareAndSet(@NonNull  final Object expected, @NonNull  final T value) {
        boolean success = mValueAR.compareAndSet(expected, value);

        if (success) {
            doThenActions(value);
        } else {
            dd(this, "compareAndSet(" + expected + ", " + value + ") failed, current value is " + safeGet());
        }

        return success;
    }

    /**
     * Add an mOnFireAction which will be run when {@link #set(Object)} is called.
     * <p>
     * If the value is already {@link #set(Object)}, the passed mOnFireAction will be run immediately and
     * synchronously.
     *
     * @return
     */
    @NonNull
    public ImmutableValue<T> then(@NonNull  final IActionOne<T> action) {
        mThenActions.add(action);
        if (isSet()) {
            doThenActions(safeGet());
        }
        return this;
    }

    @NonNull
    public ImmutableValue<T> then(@NonNull  final IAction<T> action) {
        mThenActions.add(action);
        if (isSet()) {
            doThenActions(safeGet());
        }
        return this;
    }

    private void doThenActions(@NonNull  final T value) {
        final Iterator<IBaseAction<T>> iterator = mThenActions.iterator();

        while (iterator.hasNext()) {
            final IBaseAction<T> action = iterator.next();
            if (mThenActions.remove(action)) {
                try {
                    call(value, action);
                } catch (Exception e) {
                    ee(this, "Can not do .subscribe() mOnFireAction after ImmutableValue was set to value=" + value, e);
                }
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    // IN->OUT must be bent to match all cases but the mContext makes this safe
    private <IN, OUT> OUT call(
            @NonNull  final IN in,
            @NonNull  final IBaseAction<IN> action)
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
        throw new UnsupportedOperationException("Not sure how to call this IBaseAction type: " + action.getClass());
    }

    /**
     * Check if the immutable value has been asserted yet.
     * <p>
     * Note that if you think you need this for your core logic, you may want to ask yourself if you can
     * be better served by using the dependency mechanism of {@link AltFuture#then(IAltFuture)}
     * and similar calls. It is often better to let the preconditions for a {@link #get()} be set by the functional
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
    public final boolean isSet() {
        return mValueAR.get() != SettableAltFuture.ZEN;
    }

    /**
     * Get the value, or throw {@link java.lang.IllegalStateException} if you are getting the value
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
        final Object value = mValueAR.get();

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
     * Usually you will have a better design with {@link #get()} instead. This is fine to use
     * if other parts of your application is reactive and will update the value again when it has
     * been finally set.
     *
     * @return the value, or <code>null</code> if the value has not yet been determined
     */
    @CallSuper
    @SuppressWarnings("unchecked")
    @Nullable
    public T safeGet() {
        final Object value = mValueAR.get();

        if (value == ZEN) {
            return null;
        }

        return (T) value;
    }

    /**
     * Set the value.
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
    public T set(@NonNull  final T value) {
        if (!compareAndSet(ZEN, value)) {
            throwIllegalStateException(this, "ImmutableReference can not be set multiple times. It is already set to " + safeGet() + " so we can not assert new value=" + value);
        }

        return value;
    }

    /**
     * Return the current value of the immutable value if possible
     * <p>
     * In other cases marker text "(ImmutableValue not yet set)" will be returned. If you see
     * this text, consider using a {@link #then(IActionOne)} mOnFireAction
     * to make your logic run when this value is set.
     *
     * @return
     */
    @CallSuper
    @NonNull
    public String toString() {
        T t = safeGet();

        if (t == null) {
            return "(ImmutableValue not yet set)";
        }

        return t.toString();
    }

//    @Override
//    @NonNull//
//    public String getName() {
//        return "(ImmutableValue, value=" + toString() + ")";
//    }
}
