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

import com.futurice.cascade.Async;
import com.futurice.cascade.AsyncBuilder;
import com.futurice.cascade.i.CallOrigin;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.action.IActionOneR;
import com.futurice.cascade.i.action.IActionR;
import com.futurice.cascade.i.assertion.IAssertion;
import com.futurice.cascade.i.assertion.IAssertionOne;
import com.futurice.cascade.i.assertion.IAssertionTwo;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.i.functional.IAltFuture;
import com.futurice.cascade.i.functional.IAltFutureState;
import com.futurice.cascade.i.reactive.IReactiveTarget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static com.futurice.cascade.Async.*;

/**
 * An {@link com.futurice.cascade.i.functional.IAltFuture} on which you can {@link SettableAltFuture#set(Object)}
 * one time to change state
 * <p>
 * Note that a <code>SettableAltFuture</code> is not itself {@link java.lang.Runnable}. You explicity {@link #set(Object)}
 * when the value is determined, and this changes the state to done. Therefore concepts like {@link com.futurice.cascade.i.functional.IAltFuture#fork()}
 * and {@link com.futurice.cascade.i.functional.IAltFuture#isForked()} do not have their traditional meanings.
 * <p>
 * {@link AltFuture} overrides this class.
 * TODO You may also use a {@link SettableAltFuture} to inject data where the value is determined from entirely outside of the current chain hierarchy.
 * This is currently an experimental feature so be warned, your results and chain behaviour may vary. Additional
 * testing is on the long list.
 * <p>
 * You may prefer to use {@link ImmutableValue} that a similar need in some cases. That is a
 * slightly faster, simpler implementation than {@link SettableAltFuture}.
 * <p>
 * TODO Would it be helpful for debugging to store and pass forward a reference to the object which originally detected the problem? It might help with filtering what onFireAction you want to do onError
 */
@NotCallOrigin
public class SettableAltFuture<IN, OUT> implements IAltFuture<IN, OUT> {
    protected final AtomicReference<Object> stateAR = new AtomicReference<>(ZEN);
    protected final ImmutableValue<String> origin;
    protected final IThreadType threadType;
    protected final CopyOnWriteArrayList<IAltFuture<OUT, ?>> thenAltFutureList = new CopyOnWriteArrayList<>(); // Callable split IThreadType actions to start after this onFireAction completes
    private volatile IOnErrorAction onError;
    private volatile IAltFuture<?, IN> previousAltFuture = null;

    public SettableAltFuture(@NonNull IThreadType threadType) {
        this.threadType = threadType;
        this.origin = Async.originAsync();
    }

    public SettableAltFuture(@NonNull IThreadType threadType, @NonNull OUT value) throws Exception {
        this(threadType);

        set(value);
    }

    private void assertNotForked() {
        if (Async.DEBUG && isForked()) {
            throwIllegalStateException(this, origin, "You attempted to set AltFuture.onError() after fork() or cancel(). That is not meaningful (except as a race condition..:)");
        }
    }

    private IAltFuture<IN, OUT> setOnError(@NonNull IOnErrorAction action) {
        assertNotForked();

        this.onError = action;

        return this;
    }

    @Override // IAltFuture
    @CallOrigin
    public boolean cancel(String reason) {
        assertNotDone();
        if (stateAR.compareAndSet(ZEN, new AltFutureStateCancelled(reason))) {
            dd(this, origin, "Cancelled: reason=" + reason);
            return true;
        } else {
            final Object state = stateAR.get();
            if (state instanceof AltFutureStateCancelled) {
                dd(this, origin, "Ignoring duplicate cancel. The ignored reason=" + reason + ". The previously accepted cancellation reason=" + state);
            } else {
                dd(this, origin, "Ignoring duplicate cancel. The ignored reason=" + reason + ". The previously accepted successful completion value=" + state);
            }
            return false;
        }
    }

    @Override // IAltFuture
    @CallOrigin
    public boolean cancel(String reason, Exception e) {
        assertNotDone();

        final IAltFutureState errorState = new AltFutureStateError(reason, e);
        if (stateAR.compareAndSet(ZEN, errorState)) {
            dd(this, origin, "Cancelled from ZEN state: reason=" + reason);
            return true;
        } else {
            if (stateAR.compareAndSet(FORKED, errorState)) {
                dd(this, origin, "Cancelled from FORKED state: reason=" + reason);
                return true;
            } else {
                final Object state = stateAR.get();
                if (state instanceof IAltFutureStateCancelled) {
                    dd(this, origin, "Ignoring duplicate cancel. The ignored reason=" + reason + " e=" + e + ". The previously accepted cancellation reason=" + state);
                } else {
                    dd(this, origin, "Ignoring duplicate cancel. The ignored reason=" + reason + " e=" + e + ". The previously accepted successful completion value=" + state);
                }
                return false;
            }
        }
    }

    /**
     * This will be true if the final value will never be available because either a call to {@link #cancel(String)}
     * or an {@link java.lang.Exception} has occured during execution of this or one of the previous
     * <code>AltFuture</code>s in a chain.
     *
     * @return
     */
    @Override // IAltFuture
    public boolean isCancelled() {
        return isCancelled(stateAR.get());
    }

    protected final boolean isCancelled(Object objectThatMayBeAState) {
        return objectThatMayBeAState instanceof IAltFutureStateCancelled;
    }

    /**
     * The AltFuture is "done" when it has entered final state, either by successful execution or
     * entering an IStateCancelled (internal definition). Either way, it is an immutable value
     * object from this point forward.
     *
     * @return
     */
    @Override // IAltFuture
    public final boolean isDone() {
        return isDone(stateAR.get());
    }

    protected boolean isDone(final Object state) {
        return state != ZEN && state != FORKED && !(state instanceof AltFutureStateSetButNotYetForked);
    }

    @Override // IAltFuture
    public boolean isConsumed() {
        assertErrorState();

        return isConsumed(stateAR.get());
    }

    protected boolean isConsumed(final Object state) {
        return state instanceof AltFutureStateError && ((AltFutureStateError) state).isConsumed();
    }

    @Override // IAltFuture
    public final boolean isForked() {
        return isForked(stateAR.get());
    }

    protected boolean isForked(final Object state) {
        return state != ZEN && !(state instanceof AltFutureStateSetButNotYetForked);
    }

    /**
     * Complete onError actions
     *
     * @param e
     */
//    protected final void notifyOnError(final Exception e) {
//        try {
//            if (onError == null) {
//                ee(this, origin, "No onError is defined", e);
//            } else {
//                onError.call(e);
//            }
//            doThenActions(); //FIXME Redundant- are we calling this 2x?
//        } catch (Exception e2) {
//            ee(this, origin, "Problem executing " + origin + " onError(" + e + "), error is consumed and will not propagate down-chain", e2);
//        }
//    }

    /**
     * Submit this <code>AltFuture</code> to the {@link java.util.concurrent.ExecutorService} associated
     * with its {@link Async}. It will be queued and eventually executed.
     * <p>
     * There are some memory and response time performance optimizations for <code>{@link com.futurice.cascade.i.IThreadType#isInOrderExecutor()} == false ThreadType</code> s to keep in mind. If this
     * {@link AltFuture} is part of a functional chain but not the beginning of that chain, and out of order execution is permitted, and
     * the backing ThreadType implementation uses a {@link java.util.Deque}, subscribe it will be placed in the front of
     * the queue such that it is next item available for execution. If this behaviour is not desired, the
     * simplest way to disable it without affecting other {@link com.futurice.cascade.i.IThreadType}s of concurrency is to construct the
     * ThreadType with a {@link java.util.Queue} instead of a {@link java.util.Deque}. For example:
     * <p>
     * <code><pre>
     *     // See {@link AsyncBuilder#setWorkerQueue(java.util.concurrent.BlockingQueue)}
     *     ThreadType noWorkerOrderReversalAllowedThreadType = ThreadTypeBuilder(Activity.this.getApplicationContext())
     *             .setWorkerQueue(new Queue())
     *             .build();
     * </pre></code>
     * <p>
     * If you suspect concurrency issues, a more extreme solution which may be useful for testing is
     * to use only one thread per ThreadType:
     * <p>
     * <code><pre>
     *     // See {@link AsyncBuilder#singleThreadedWorkerExecutorService()}
     *     ThreadType noConcurrencyWithinEachThreadTypeAllowedThreadType = AsyncBuilder(Activity.this.getApplicationContext())
     *               .singleThreadedWorkerExecutorService()
     *               .build();
     * </pre></code>
     * <p>
     * <p>
     * Downstream actions are fired when this value {@link #set(Object)}. This will fire immediately
     * when set() if this is the head of a chain (<code>{@link #getPreviousAltFuture()} == null</code>.
     * If it is no
     *
     * @return
     */
    @Override // IAltFuture
    public IAltFuture<IN, OUT> fork() {
        final IAltFuture<?, IN> previousAltFuture = getPreviousAltFuture();
        Object state;

        if (previousAltFuture != null && !previousAltFuture.isForked()) {
            previousAltFuture.fork();
            return this;
        } else {
            if (stateAR.compareAndSet(ZEN, FORKED) ||
                    ((state = stateAR.get()) instanceof AltFutureStateSetButNotYetForked) &&
                            stateAR.compareAndSet(state, ((AltFutureStateSetButNotYetForked) state).value)) {
                // You are here because you were in ZEN or StateSetButNotYetForked (which is now successfully atomically changed to a final isDone() state)
                doFork();
                return this;
            }
        }
        dd(this, origin, "Warning: Ignoring attempt to fork() forked/completed/cancelled/error-state AltFuture. This may be a normal race condition, or maybe you fork() multiple times. state= " + stateAR.get());

        return this;
    }

    protected void doFork() {
        // This is not an IRunnableAltFuture, so nothing to execute(). But AltFuture overrides this and does more
        try {
            doThenActions();
        } catch (Exception e) {
            ee(this, "Can not doFork()", e);
        }
    }

    @Override // IAltFuture
    public final <P> IAltFuture<IN, OUT> setPreviousAltFuture(@NonNull IAltFuture<P, IN> altFuture) {
        assertTrue("previousAltFuture must be null", previousAltFuture == null);
        this.previousAltFuture = altFuture;

        return this;
    }

    /**
     * Implementations of {@link #fork()} must call this when completed. It reduces the window of time
     * in which past intermediate calculation values in a functional chain are held in memory. It is
     * the equivalent of the (illegal) statement:
     * <code>{@link #setPreviousAltFuture(com.futurice.cascade.i.functional.IAltFuture)}</code> to null.
     * <p>
     * This may not be done until {@link #isDone()} == true, such as when the {@link #fork()} has completed.
     */
    protected final void clearPreviousAltFuture() {
        if (isDone()) {
            this.previousAltFuture = null;
        }
    }

    @Override // IAltFuture
    public final <UPCHAIN_IN> IAltFuture<UPCHAIN_IN, IN> getPreviousAltFuture() {
        return (IAltFuture<UPCHAIN_IN, IN>) this.previousAltFuture;
    }

    protected void assertNotDone() {
        assertTrue("assertNotDone failed: SettableFuture already finished or entered canceled/error state", !isDone());
    }

    protected void assertNotConsumed() {
        assertTrue("assertNotConsumed failed: SettableFuture has already been consumed", !isConsumed());
    }

    protected void assertDone() {
        assertTrue("assertDone failed: SettableFuture is not finished or canceled/error state", isDone());
    }

    /**
     * In debugOrigin builds only, check the condition specified. If that is not satisfied, abort the current
     * functional chain by throwing an {@link java.lang.IllegalStateException} with the explanation reason provided.
     *
     * @param reason
     * @param testResult
     */
    protected void assertTrue(String reason, boolean testResult) {
        if (Async.DEBUG && !testResult) {
            throwIllegalStateException(this, origin, addOriginToReason(reason, origin) + " state=" + this.stateAR);
        }
    }

    /**
     * Assert that a logic statement, evaluated after any previous steps in a chain, is true
     * <p>
     * No late-binding-evaluation-time or functional-chain-creation-time parameters are provided
     * to the logical assertion in this variant. The assertion logic itself may still contain
     * references to in-scope final variables (closures) and/or method calls that check that
     * state is as expected.
     * <p>
     * Runtime assertions such as this serve both the notify early of potentially regressive changes
     * in the code and as a form of documentation about assumptions an algorithm makes that might
     * otherwise not be clear to the reader. You may also have reason to do unit tests particularly
     * where test data sets are needed. The advantage of these runtime assertions is they are clear
     * and easy to read directly in the affected code and little extra work is required to write and
     * no extra work is required to execute them. Runtime assertions are execute iff you are running a debugOrigin
     * build but removed automatically in production builds.
     *
     * @param reason
     * @param assertion
     * @return
     * @throws AssertionError
     */
    public IAltFuture<IN, OUT> assertTrue(String reason, IAssertion assertion) throws AssertionError {
        if (DEBUG) {
            then(this
                            .onError(e -> {
                                ee(this, origin, "Consuming up-chain error on split chain before assertion", e);
                                return true;
                            })
                            .then(() -> assertTrue(reason, assertion.call()))
                            .onError(e -> {
                                return cancel(
                                        "Problem performing runtime assertion",
                                        new RuntimeException("Problem performing runtime assertion", e));
                            })
            );
        }

        return this;
    }

    /**
     * Assert that a logic statement, evaluated after any previous steps in a chain, is true
     * <p>
     * For performance in production builds (at which point your architecture problems are already fixed, right? :) this assertTrue
     * will always return <code>false</code>. In this case the re-fork will still
     * throw an {@link java.lang.IllegalStateException}, but does so a bit later. This would happen when the underlying
     * {@link java.util.concurrent.RunnableFuture} finds it has already been execute.
     *
     * @param reason
     * @param IN
     * @param assertion
     * @return - true if everything is as expected
     * @throws AssertionError
     */
    public IAltFuture<IN, OUT> assertTrue(String reason, IN IN, IAssertionOne<IN> assertion) throws AssertionError {
        if (DEBUG) {
            then(this
                            .onError(e -> {
                                ee(this, origin, "Consuming up-chain error on split chain before assertion", e);
                                return true;
                            })
                            .then(() -> assertTrue(reason, assertion.call(IN)))
                            .onError((Exception e) -> {
                                return cancel(
                                        "Problem performing runtime assertion",
                                        new RuntimeException("Problem performing runtime assertion", e));
                            })
            );
        }

        return this;
    }

    /**
     * Assert that a logic statement including the value returned from the previous AltFuture in a
     * chain is true.
     *
     * @param reason
     * @param assertion
     * @return - true if everything is as expected
     * @throws AssertionError
     */
    public IAltFuture<IN, OUT> assertTrue(String reason, IAssertionOne<IN> assertion) throws AssertionError {
        if (DEBUG) {
            then(this
                            .onError(e -> {
                                ee(this, origin, "Consuming up-chain error on split chain before assertion", e);
                                return true;
                            })
                            .then(() -> assertTrue(reason, assertion.call((IN) get())))
                            .onError(e -> {
                                return cancel(
                                        "Problem performing runtime assertion",
                                        new RuntimeException("Problem performing runtime assertion", e));
                            })
            );
        }

        return this;
    }

    /**
     * Assert that a logic statement including the value returned from the previous AltFuture in a
     * chain and a second value passed in at the time the chain is created is true.
     *
     * @param reason
     * @param b
     * @param assertion
     * @param <B>       - a value which is fixed at the time the functional chain is created
     * @return - true if everything is as expected
     */
    public <B> IAltFuture<OUT, OUT> assertTrue(String reason, B b, IAssertionTwo<B, IN> assertion) {
        if (DEBUG) {
            return split(onError(e -> {
                dd(this, origin, "Consuming up-chain error before assertTrue: " + e);
                return true;
            })
                    .then(() -> assertTrue(reason, assertion.call(b, (IN) get())))
                    .onError(e -> {
                        String s = "Problem performing runtime assertion: " + reason + " value=" + b;
                        ee(this, origin, s, e);
                        throw new RuntimeException(s, e);
                    }));
        }

        return (IAltFuture<OUT, OUT>) this;
    }

    /**
     * Return the value of this AltFuture which has already completed execution
     * <p>
     * Note that unlike a standard Future, this will return an {@link java.lang.IllegalStateException}
     * at runtime if you attempt to call it before the AltFuture is finished. Blocking functions are
     * not allowed for performance split stability reasons (no free threads deadlock).
     * <p>
     * Usually getValue() is called indirectly for you by creating a functional chain. When you
     * use {@link AltFuture#then(com.futurice.cascade.i.functional.IAltFuture)} split related convenience methods,
     * the functional chain will execute in explicit chain dependency order efficiently without blocking.
     * This avoids several common multi-threaded design problems like excessive concurrency split applicationContext
     * switching. Non-blocking thread design also eliminates common thread starvation or running out
     * of thread resources due to one or more blocked operations.
     * <p>
     * If you really want the classic {@link java.util.concurrent.Future#get()} block-until-complete
     * behaviour, you may implement an {@link com.futurice.cascade.i.functional.IAltFuture} which also implements
     * {@link java.util.concurrent.RunnableFuture} such as {@link java.util.concurrent.FutureTask}.
     * No default implementation is provided.
     *
     * @return
     */
    @Override // IAltFuture
    public OUT get() {
        final Object state = stateAR.get();

        if (!isDone(state)) {
            throwIllegalStateException(this, origin, "Attempt to get() AltFuture that is not yet finished. state=" + state);
        }
        if (isCancelled(state)) {
            throwIllegalStateException(this, origin, "Attempt to get() AltFuture that is cancelled: state=" + state);
        }

        return (OUT) state;
    }

    @Override // IAltFuture
    public OUT safeGet() {
        final Object state = stateAR.get();

        if (!isDone(state) || isCancelled(state)) {
            return null;
        }

        return (OUT) state;

    }

    @Override // IAltFuture
    public final IThreadType getThreadType() {
        return this.threadType;
    }

    /**
     * Since SettableAltFuture.set(T) can happen _before_ .fork(), this marks the intermediate state
     * until .fork() is explicitly called. This affects isDone() logic in particular, because in this
     * state isDone() is not true. Only fork() makes it true.
     * <p>
     * Due to Java generics limitations with a non-static generic inner class and instanceof, this is better
     * off with "Object" than type "T". Type safety is held by surrounding methods.
     */
    private static class AltFutureStateSetButNotYetForked implements IAltFutureState {
        final Object value;

        AltFutureStateSetButNotYetForked(Object value) {
            this.value = value;
        }

        @Override
        public Exception getException() {
            throw new IllegalStateException("Can not getException() for a non-exception state " + AltFutureStateSetButNotYetForked.class.getSimpleName());
        }

        @Override
        public String toString() {
            return "SET_BUT_NOT_YET_FORKED: value=" + value;
        }

    }

    @Override // IAltFuture
    public void set(OUT value) throws Exception {
        if (stateAR.compareAndSet(ZEN, new AltFutureStateSetButNotYetForked(value))) {
            // Previous state was ZEN, so accept it but do not enter isDone() and complete .subscribe() onFireAction until after .fork() is called
            if (DEBUG) {
                final int n = thenAltFutureList.size();
                if (n != 0) {
                    dd(this, origin, "Set value= " + value);
                } else {
                    dd(this, origin, "Set value= " + value + "\nWe can not do the " + n + " down-chain actions because .fork() has not been called yet");
                }
            }
            return;
        }

        if (stateAR.compareAndSet(FORKED, value)) {
            // Previous state was FORKED, so set completes the onFireAction and continues the chain
            if (DEBUG) {
                final int n = thenAltFutureList.size();
                if (n == 0) {
                    vv(this, origin, "SettableAltFuture set, value= " + value + "\nNo down-chain actions");
                } else {
                    vv(this, origin, "SettableAltFuture set, value= " + value + "\nWe now fork() the " + thenAltFutureList.size() + " down-chain actions because this.fork() was called previously");
                }
            }
            doThenActions();
            return;
        }

        // Already set, cancelled or error state
        throwIllegalStateException(this, origin, "Attempted to set " + this + " to value=" + value + ", but the value can only be set once");
    }

    public final boolean isHeadOfChain() {
        return getPreviousAltFuture() == null;
    }

    /**
     * See {@link java.util.concurrent.atomic.AtomicReference} for examples of this asynchronous pattern.
     * (There is no {@link java.util.concurrent.atomic.AtomicReference} in the implementation).
     * <p>
     * If the expected value is still the current value and the {@link SettableAltFuture}
     * has not been previously set, subscribe the value is accepted.
     *
     * @param expected
     * @param value
     * @return
     */
    public boolean compareAndSet(Object expected, OUT value) {
        assertTrue(this + ".compareAndSet() must expect STATE_NOT_SET or you are concurrently asserting an illegal value", expected == ZEN || expected == FORKED);
        return stateAR.compareAndSet(expected, value);
    }

    @Override // IAltFuture
    public void doThenOnCancelled(final CancellationException cancellationException) throws Exception {
        vv(this, origin, "Handling doThenOnCancelled " + origin + " for reason=" + cancellationException);
        this.stateAR.set(cancellationException);
        final IOnErrorAction oe = this.onError;

        if (oe != null && oe.call(cancellationException)) {
            return; // Error chain was consumed
        }
        cancelAllDownchainActions(cancellationException);
    }

    private void cancelAllDownchainActions(final CancellationException cancellationException) throws Exception {
        forEachThen(altFuture -> {
            altFuture.doThenOnCancelled(cancellationException);
        });
    }

    /**
     * Perform some action on an instantaneous snapshot of the list of .subscribe() down-chain actions
     *
     * @param action
     * @throws Exception
     */
    private void forEachThen(final IActionOne<IAltFuture<OUT, ?>> action) throws Exception {
        final Iterator<IAltFuture<OUT, ?>> iterator = thenAltFutureList.iterator();

        while (iterator.hasNext()) {
            action.call(iterator.next());
        }
    }

    /**
     * Notify the onError onFireAction specified for this AltFuture that there was an error either in this,
     * (or a previous up-chain) AltFuture execution.
     *
     * @param state
     * @return <code>true</code> if the onError chain ends here because it is a catch, otherwise false to signal
     * that onError farther down the chain should also be notified. "false" is the default behavior
     * since it is non-standard to not notify all who have expressed an interest in errors to be notified.
     */
    @NotCallOrigin
    @Override // IAltFuture
    public void doThenOnError(final IAltFutureState state) throws Exception {
        vv(this, origin, "Handling doThenOnError(): " + state);
        this.stateAR.set(state);
        final IOnErrorAction oe = onError;
        boolean consumed = false;

        if (oe != null) {
            final IActionR<IN, Boolean> errorAction;
            consumed = oe.call(state.getException());
        }

        if (consumed) {
            // When an error is consumed in the chain, we switch over to still notify with cancellation instead
            cancelAllDownchainActions(new CancellationException("Up-chain consumed the following: " + state.getException().toString()));
        } else {
            forEachThen(altFuture -> {
                altFuture.doThenOnError(state);
            });
        }
    }

    @Override // IAltFuture
    public IAltFuture<IN, OUT> onError(IOnErrorAction action) {
        return setOnError(action);
    }

    private void assertErrorState() {
        if (DEBUG && !(stateAR.get() instanceof AltFutureStateError)) {
            throwIllegalStateException(this, origin, "Do not call doThenOnError() directly. It can only be called when we are already in an error state and this is done for you when the AltFuture enters an error state by running code which throws an Exception");
        }
    }

//    private <UPCHAIN_IN> boolean callOnError(IAltFuture<UPCHAIN_IN, IN> previousAltFuture, Exception e, IOnErrorAction<IN> onError) throws Exception {
//        IN IN = null;
//
//        if (DEBUG && previousAltFuture == null) {
//            throwIllegalArgumentException(this, origin, "The previous AltFuture in the chain must be non-null to use an IOnErrorActionOne " + origin + ". Did you mean to use IOnErrorAction instead?");
//        }
//        if (previousAltFuture.isDone()) {
//            IN = previousAltFuture.get();
//        } else {
//            vv(this, origin, "AltFuture error before final getValue determined on previous AltFuture in functional chain- will pass a getValue of null");
//        }
//
//        return onError.call(e);
//    }

    //----------------------------------- .subscribe() style actions ---------------------------------------------
    private <N> IAltFuture<OUT, N> addToThenQueue(@NonNull IAltFuture<OUT, N> altFuture) {
        altFuture.setPreviousAltFuture(this);
        this.thenAltFutureList.add(altFuture);
        if (isDone()) {
            vv(this, origin, "Warning: an AltFuture was added as a .subscribe() onFireAction to an already completed AltFuture. Being aggressive, are you? It is supported but in most cases you probably want top setup your entire chain before you fork any part of it");
//            altFuture.then((IActionOne) v -> {
//                visualize(origin.getName(), v.toString(), "AltFuture");
//            });
            altFuture.fork();
        }

        return altFuture;
    }

    protected void doThenActions() throws Exception {
        //vv(origin, TAG, "Start doThenActions, count=" + this.thenAltFutureList.size() + ", state=" + stateAR.get());
        if (DEBUG && !isDone()) {
            vv(this, origin, "This AltFuture is not yet done, so can't doNextActions() yet");
            return;
        }

        if (thenAltFutureList.isEmpty()) {
            return;
        }

        final Object state = this.stateAR.get();
        if (state instanceof IAltFutureStateCancelled) {
            if (state instanceof AltFutureStateCancelled || isConsumed(state)) {
                final String reason = ((AltFutureStateCancelled) state).reason;
                final CancellationException cancellationException = new CancellationException(reason);
                forEachThen(altFuture -> {
                    altFuture.doThenOnCancelled(cancellationException);
                });
            } else if (state instanceof AltFutureStateError) {
                forEachThen(altFuture -> {
                    altFuture.doThenOnError((AltFutureStateError) state);
                });
            } else {
                throw new UnsupportedOperationException("Unsupported SettableAltFuture state: " + state.getClass());
            }
        } else {
            forEachThen(af -> {
                if (af.isForked()) {
                    vv(this, origin, "Potential race such as adding .subscribe() after .fork(): This is acceptable but aggressive. doThenActions() finds one of the actions chained after the current AltFuture has already been forked: " + af);
                } else {
                    af.fork();
                }
            });
        }
    }

    /**
     * Complete an onFireAction after this <code>AltFuture</code>
     * <p>
     * Usage will typically be to start a concurrent execution chain such that <code>B</code> and <code>C</code>
     * in the following example may both begin after <code>A</code> completes.
     * <pre><code>
     *     myAltFuture
     *        .subscribe(..A..)
     *        .split(this
     *               .subscribe(..B..)
     *               .subscribe(..)
     *               .onError(..))
     *        .subscribe(..C..)
     *        .onError(..)
     * </code></pre>
     * <p>
     * Additional {@link #split(com.futurice.cascade.i.functional.IAltFuture)} split {@link AltFuture#then(com.futurice.cascade.i.functional.IAltFuture)}
     * functions chained after this will receive the same input argument split (depending on the {@link com.futurice.cascade.i.IThreadType}
     * may execute concurrently.
     *
     * @param altFuture
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @Override // IAltFuture
    public <DOWNCHAIN_OUT> IAltFuture<OUT, OUT> split(@NonNull IAltFuture<OUT, DOWNCHAIN_OUT> altFuture) {
        assertNotDone();

        then(altFuture);

        return (IAltFuture<OUT, OUT>) this;
    }

    /**
     * Indicate that the chain will continue only when the SettableAltFuture is set. This may be
     * after some inner loop/chain completes, or at another arbitrary point in the future determined
     * by external events.
     * <p>
     * All other .subscribe(function) and .split() operations create an {@link AltFuture}
     * if needed and terminate internally in a call to this method.
     * <p>
     * FAQ: Did your chain fail to execute? Did you remember to call {@link #fork()} when you are ready
     * to execute it? Many chains are ready to execute when they are constructed, so <code>.fork()</code>
     * is often the last step of an subscribe function chain.
     *
     * @param altFuture
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @Override // IAltFuture
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(IAltFuture<OUT, DOWNCHAIN_OUT> altFuture) {
        addToThenQueue(altFuture);
        if (isDone()) {
            dd(this, origin, ".subscribe() to an already cancelled/error state AltFuture. It will be fork()ed. Would your code be more clear if you delay fork() of the original chain until you finish building it, or .fork() a new chain at this point?");
            altFuture.fork();
        }

        return altFuture;
    }

    /**
     * Execute the onFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @Override // IAltFuture
    public IAltFuture<OUT, OUT> then(IActionOne<OUT> action) {
        return then(threadType, action);
    }

    /**
     * Execute the onFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @return
     */
    @Override // IAltFuture
    public IAltFuture<OUT, OUT> then(IThreadType threadType, IActionOne<OUT> action) {
        return then(new AltFuture(threadType, action));
    }

    /**
     * Execute the onFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @Override // IAltFuture
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(IActionR<OUT, DOWNCHAIN_OUT> action) {
        return then(threadType, action);
    }

    /**
     * Execute the onFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @return
     */
    @Override // IAltFuture
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(IThreadType threadType, IActionR<OUT, DOWNCHAIN_OUT> action) {
        return then(new AltFuture(threadType, action));
    }

    /**
     * Execute the onFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @Override // IAltFuture
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        return then(threadType, action);
    }

    /**
     * Execute the onFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @Override // IAltFuture
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(IThreadType threadType, IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        return then(new AltFuture(threadType, action));
    }

    /**
     * Execute the onFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @Override // IAltFuture
    public IAltFuture<OUT, OUT> then(IAction<OUT> action) {
        return then(threadType, action);
    }

    /**
     * Execute the onFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @return
     */
    @Override // IAltFuture
    public IAltFuture<OUT, OUT> then(IThreadType threadType, IAction<OUT> action) {
        return then(new AltFuture<>(threadType, action));
    }

    /**
     * Execute the onFireAction for each element of the {@link java.util.List}, creating a new <code>List</code>
     *
     * @param action
     * @return
     */
    @Override // IAltFuture
    public IAltFuture<List<IN>, List<OUT>> map(IActionOneR<IN, OUT> action) {
        return map(threadType, action);
    }

    /**
     * Execute the onFireAction for each element of the {@link java.util.List}, creating a new <code>List</code>
     *
     * @param threadType
     * @param action
     * @return
     */
    @Override // IAltFuture
    public IAltFuture<List<IN>, List<OUT>> map(IThreadType threadType, IActionOneR<IN, OUT> action) {
        return new AltFuture<>(threadType,
                (List<IN> listIN) -> {
                    //TODO Mapping is single-threaded even for long lists or complex transforms
                    //TODO Idea: create the list of things to call(), and offer that to other threads in the ThreadType if they have freetime to help out
                    final List<OUT> outputList = new ArrayList<>(listIN.size());
                    for (IN IN : listIN) {
                        outputList.add(action.call(IN));
                    }
                    return outputList;
                }
        );
    }

    @Override // IAltFuture
    public IAltFuture<List<IN>, List<IN>> filter(IActionOneR<IN, Boolean> action) {
        return filter(threadType, action);
    }

    @Override // IAltFuture
    public IAltFuture<List<IN>, List<IN>> filter(IThreadType threadType, IActionOneR<IN, Boolean> action) {
        return new AltFuture<>(threadType,
                (List<IN> listIN) -> {
                    final List<IN> outputList = new ArrayList<>(listIN.size());
                    for (IN IN : listIN) {
                        if (action.call(IN)) {
                            outputList.add(IN);
                        }
                    }
                    return outputList;
                }
        );
    }

    /**
     * Set an atomic value with the output value of this {@link AltFuture}. If
     * this <code>AltFuture</code> does not assert a value change (its onFireAction is for example {@link com.futurice.cascade.i.action.IActionOne}
     * which does not return a new value) subscribe the value assigned will be the upchain value. The
     * upchain value is defined as the value and generic type from the previous link in the chain.
     * <p>
     * The return type is a bit ugly. If you need to continue the functional chain, consider adding a
     * {@link #split(com.futurice.cascade.i.functional.IAltFuture)} unless you are really interested in tracking when all bindings fired as
     * a result of this set() operation are completed.
     *
     * @param reactiveTarget
     * @return
     */
    @Override // IAltFuture
    public IAltFuture<OUT, OUT> set(@NonNull IReactiveTarget<OUT> reactiveTarget) {
        return then((OUT value) -> {
            reactiveTarget.fire(value);
        });
    }

    /**
     * An {@link ImmutableValue} is a simpler structure than {@link SettableAltFuture}.
     * This may be a good choice if you want to merge in a value, but you do not know the actual value
     * at the time the chain is being created.
     *
     * @param immutableValue
     * @return
     */
    @Override // IAltFuture
    public IAltFuture<OUT, OUT> set(@NonNull ImmutableValue<OUT> immutableValue) {
        return then((OUT value) ->
                immutableValue.set(value));
    }

//=============================== End .subscribe() Actions ========================================

    /**
     * A value similar to null, but meaning "no mind", "unasserted" or "state not set".
     * <p>
     * Many would use <code>null</code> instead of <code>ZEN</code> to initialize a variable. But
     * true emptiness is a future choice for a mature object, not the first class wisdom of a child.
     * The difference can matter, for example to differentiate between "the value has been set to null"
     * and "the value has not yet been set".
     * <p>
     * The contract is: once a state of ZEN has been lost, it can not be regained.
     * <p>
     * A Cup of Tea
     * <p>
     * Nan-in, a Japanese master during the Meiji era (1868-1912), received a university
     * professor who came to inquire about Zen.
     * <p>
     * Nan-in served tea. He poured his visitor's cup full, and subscribe kept on pouring.
     * The professor watched the overflow until he no longer could restrain himself.
     * "It is overfull. No more will go in! "Like this cup," Nan-in said, "you are full
     * of your own opinions and speculations. How can I show you Zen unless you first empty your cup?"
     * <p>
     * {@link "http://www.lotustemple.us/resources/koansandmondo.html"}
     * <p>
     * TODO Document ZEN and apply to use to allow collections and arguments that currently might not accept null to accept null as a first class value. Not yet used in many places.
     */
    protected static final IAltFutureState ZEN = new IAltFutureState() {
        @Override
        public Exception getException() {
            throw new IllegalStateException("Can not getException() for a non-exception state ZEN");
        }

        @Override
        public String toString() {
            return "ZEN";
        }
    };

    protected static final IAltFutureState FORKED = new IAltFutureState() {
        @Override
        public Exception getException() {
            throw new IllegalStateException("Can not getException() for a non-exception state FORKED");
        }

        @Override
        public String toString() {
            return "FORKED";
        }
    };

    /**
     * This is a marker interface. If you return state information, the atomic inner state of your
     * implementation should implement this interface.
     */
    @NotCallOrigin
    protected interface IAltFutureStateCancelled extends IAltFutureState {
    }

    @NotCallOrigin
    protected static final class AltFutureStateCancelled implements IAltFutureStateCancelled {
        final String reason;

        AltFutureStateCancelled(String reason) {
            if (DEBUG && (reason == null || reason.length() == 0)) {
                throwIllegalArgumentException(this, "You must specify the cancellation reason to keep debugging sane");
            }
            this.reason = reason;
            dd(this, "Moving to StateCancelled:\n" + this.reason);
        }

        @Override // IAltFutureStateCancelled
        public Exception getException() {
            throw new IllegalStateException("Can not getException() for a non-exception state " + AltFutureStateCancelled.class.getSimpleName());
        }

        @Override // Object
        public String toString() {
            return "CANCELLED: reason=" + reason;
        }
    }

    /**
     * An atomic state change marking also the reason for entering the exception state
     */
    @NotCallOrigin
    protected static class AltFutureStateError implements IAltFutureStateCancelled {
        final String reason;
        final Exception e;
        private volatile boolean consumed = false; // Set true to indicate that no more down-chain error notifications should occur, the developer asserts that the error is handled and of no further interest for all global states and down-chain listeners

        AltFutureStateError(String reason, Exception e) {
            if (DEBUG && (reason == null || reason.length() == 0 || e == null)) {
                throwIllegalArgumentException(this, "You must specify the error reason and exception to keep debugging sane");
            }
            this.reason = reason;
            this.e = e;
            ee(this, "Moving to StateError:\n" + this.reason, e);
        }

        /**
         * Note that this is not thread-safe. You may only process errors and consume them on a single thread
         */
        void consume() {
            consumed = true;
        }

        boolean isConsumed() {
            return consumed;
        }

        @Override // IAltFutureStateCancelled
        public Exception getException() {
            return e;
        }

        @Override // Object
        public String toString() {
            return "ERROR: reason=" + reason + " error=" + e + " consumed=" + consumed;
        }
    }
}