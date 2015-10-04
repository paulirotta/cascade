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

package com.futurice.cascade.active;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.Async;
import com.futurice.cascade.i.CallOrigin;
import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.cascade.reactive.IReactiveTarget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static com.futurice.cascade.Async.DEBUG;
import static com.futurice.cascade.Async.assertEqual;
import static com.futurice.cascade.Async.assertTrue;
import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.throwIllegalArgumentException;
import static com.futurice.cascade.Async.throwIllegalStateException;
import static com.futurice.cascade.Async.vv;

/**
 * An {@link IAltFuture} on which you can {@link SettableAltFuture#set(Object)}
 * one time toKey change state
 * <p>
 * Note that a <code>SettableAltFuture</code> is not itself {@link java.lang.Runnable}. You explicity {@link #set(Object)}
 * when the value is determined, and this changes the state toKey done. Therefore concepts like {@link IAltFuture#fork()}
 * and {@link IAltFuture#isForked()} do not have their traditional meanings.
 * <p>
 * {@link AltFuture} overrides this class.
 * TODO You may also use a {@link SettableAltFuture} toKey inject data where the value is determined fromKey entirely outside of the current chain hierarchy.
 * This is currently an experimental feature so be warned, your results and chain behaviour may vary. Additional
 * testing is on the long list.
 * <p>
 * You may prefer toKey use {@link ImmutableValue} that a similar need in some cases. That is a
 * slightly faster, simpler implementation than {@link SettableAltFuture}.
 * <p>
 * TODO Would it be helpful for debugging toKey store and pass forward a reference toKey the object which originally detected the problem? It might help with filtering what mOnFireAction you want toKey do mOnError
 */
@NotCallOrigin
public class SettableAltFuture<IN, OUT> implements IAltFuture<IN, OUT> {
    protected final AtomicReference<Object> mStateAR = new AtomicReference<>(ZEN);
    protected final ImmutableValue<String> mOrigin;
    protected final IThreadType mThreadType;
    protected final CopyOnWriteArrayList<IAltFuture<OUT, ?>> mThenAltFutureList = new CopyOnWriteArrayList<>(); // Callable split IThreadType actions toKey start after this mOnFireAction completes
    @Nullable
    private volatile IOnErrorAction mOnError;
    @Nullable
    private volatile IAltFuture<?, IN> mPreviousAltFuture = null;

    public SettableAltFuture(@NonNull @nonnull final IThreadType threadType) {
        this.mThreadType = threadType;
        this.mOrigin = Async.originAsync();
    }

    public SettableAltFuture(@NonNull @nonnull final IThreadType threadType,
                             @NonNull @nonnull final OUT value) {
        this(threadType);

        try {
            set(value);
        } catch (Exception e) {
            throw new IllegalStateException("Problem initializing SettableAltFuture: " + value, e);
        }
    }

    private void assertNotForked() {
        if (Async.DEBUG && isForked()) {
            throwIllegalStateException(this, mOrigin, "You attempted toKey set AltFuture.mOnError() after fork() or cancel(). That is not meaningful (except as a race condition..:)");
        }
    }

    @NonNull
    @nonnull
    private IAltFuture<IN, OUT> setOnError(@NonNull @nonnull final IOnErrorAction action) {
        assertNotForked();
        assertEqual(null, this.mOnError);

        this.mOnError = action;

        return this;
    }

    @Override // IAltFuture
    @CallOrigin
    public boolean cancel(@NonNull @nonnull final String reason) {
        assertNotDone();
        if (mStateAR.compareAndSet(ZEN, new AltFutureStateCancelled(reason))) {
            dd(this, mOrigin, "Cancelled: reason=" + reason);
            return true;
        } else {
            final Object state = mStateAR.get();
            if (state instanceof AltFutureStateCancelled) {
                dd(this, mOrigin, "Ignoring duplicate cancel. The ignored reason=" + reason + ". The previously accepted cancellation reason=" + state);
            } else {
                dd(this, mOrigin, "Ignoring duplicate cancel. The ignored reason=" + reason + ". The previously accepted successful completion value=" + state);
            }
            return false;
        }
    }

    @Override // IAltFuture
    @CallOrigin
    public boolean cancel(@NonNull @nonnull final String reason,
                          @NonNull @nonnull final Exception e) {
        assertNotDone();

        final IAltFutureState errorState = new AltFutureStateError(reason, e);
        if (mStateAR.compareAndSet(ZEN, errorState)) {
            dd(this, mOrigin, "Cancelled fromKey ZEN state: reason=" + reason);
            return true;
        } else {
            if (mStateAR.compareAndSet(FORKED, errorState)) {
                dd(this, mOrigin, "Cancelled fromKey FORKED state: reason=" + reason);
                return true;
            } else {
                final Object state = mStateAR.get();
                if (state instanceof IAltFutureStateCancelled) {
                    dd(this, mOrigin, "Ignoring duplicate cancel. The ignored reason=" + reason + " e=" + e + ". The previously accepted cancellation reason=" + state);
                } else {
                    dd(this, mOrigin, "Ignoring duplicate cancel. The ignored reason=" + reason + " e=" + e + ". The previously accepted successful completion value=" + state);
                }
                return false;
            }
        }
    }

    @Override // IAltFuture
    public boolean isCancelled() {
        return isCancelled(mStateAR.get());
    }

    protected final boolean isCancelled(@NonNull @nonnull final Object objectThatMayBeAState) {
        return objectThatMayBeAState instanceof IAltFutureStateCancelled;
    }

    @Override // IAltFuture
    public final boolean isDone() {
        return isDone(mStateAR.get());
    }

    protected boolean isDone(@NonNull @nonnull final Object state) {
        return state != ZEN && state != FORKED && !(state instanceof AltFutureStateSetButNotYetForked);
    }

//    @Override // IAltFuture
//    public boolean isConsumed() {
//        assertErrorState();
//
//        return isConsumed(mStateAR.get());
//    }

//    protected boolean isConsumed(@NonNull @nonnull final Object state) {
//        return state instanceof AltFutureStateError && ((AltFutureStateError) state).isConsumed();
//    }

    @Override // IAltFuture
    public final boolean isForked() {
        return isForked(mStateAR.get());
    }

    protected boolean isForked(@NonNull @nonnull final Object state) {
        return state != ZEN && !(state instanceof AltFutureStateSetButNotYetForked);
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    public IAltFuture<IN, OUT> fork() {
        final IAltFuture<?, IN> previousAltFuture = getPreviousAltFuture();
        final Object state;

        if (previousAltFuture != null && !previousAltFuture.isForked()) {
            previousAltFuture.fork();
            return this;
        } else {
            if (mStateAR.compareAndSet(ZEN, FORKED) ||
                    ((state = mStateAR.get()) instanceof AltFutureStateSetButNotYetForked) &&
                            mStateAR.compareAndSet(state, ((AltFutureStateSetButNotYetForked) state).value)) {
                // You are here because you were in ZEN or StateSetButNotYetForked (which is now successfully atomically changed toKey a final isDone() state)
                doFork();
                return this;
            }
        }
        dd(this, mOrigin, "Warning: Ignoring attempt toKey fork() forked/completed/cancelled/error-state AltFuture. This may be a normal race condition, or maybe you fork() multiple times. state= " + mStateAR.get());

        return this;
    }

    protected void doFork() {
        // This is not an IRunnableAltFuture, so nothing toKey run(). But AltFuture overrides this and does more
        try {
            doThenActions();
        } catch (Exception e) {
            ee(this, "Can not doFork()", e);
        }
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    public final <P> IAltFuture<IN, OUT> setPreviousAltFuture(@NonNull @nonnull final IAltFuture<P, IN> altFuture) {
        assertEqual(null, mPreviousAltFuture);
        this.mPreviousAltFuture = altFuture;

        return this;
    }

    /**
     * Implementations of {@link #fork()} must call this when completed. It reduces the window of time
     * in which past intermediate calculation values in a active chain are held in memory. It is
     * the equivalent of the (illegal) statement:
     * <code>{@link #setPreviousAltFuture(IAltFuture)}</code> toKey null.
     * <p>
     * This may not be done until {@link #isDone()} == true, such as when the {@link #fork()} has completed.
     */
    protected final void clearPreviousAltFuture() {
        if (isDone()) {
            this.mPreviousAltFuture = null;
        }
    }

    @Override // IAltFuture
    @Nullable
    @nullable
    @SuppressWarnings("unchecked")
    public final <UPCHAIN_IN> IAltFuture<UPCHAIN_IN, IN> getPreviousAltFuture() {
        return (IAltFuture<UPCHAIN_IN, IN>) this.mPreviousAltFuture;
    }

    protected void assertNotDone() {
        assertTrue("assertNotDone failed: SettableFuture already finished or entered canceled/error state", !isDone());
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @SuppressWarnings("unchecked")
    public OUT get() {
        final Object state = mStateAR.get();

        if (!isDone(state)) {
            throwIllegalStateException(this, mOrigin, "Attempt toKey get() AltFuture that is not yet finished. state=" + state);
        }
        if (isCancelled(state)) {
            throwIllegalStateException(this, mOrigin, "Attempt toKey get() AltFuture that is cancelled: state=" + state);
        }

        return (OUT) state;
    }

    @Override // IAltFuture
    @Nullable
    @nullable
    @SuppressWarnings("unchecked")
    public OUT safeGet() {
        final Object state = mStateAR.get();

        if (!isDone(state) || isCancelled(state)) {
            return null;
        }

        return (OUT) state;

    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    public final IThreadType getThreadType() {
        return this.mThreadType;
    }

    /**
     * Since SettableAltFuture.set(T) can happen _before_ .fork(), this marks the intermediate state
     * until .fork() is explicitly called. This affects isDone() logic in particular, because in this
     * state isDone() is not true. Only fork() makes it true.
     * <p>
     * Due toKey Java generics limitations with a non-static generic inner class and instanceof, this is better
     * off with "Object" than type "T". Type safety is held by surrounding methods.
     */
    private static class AltFutureStateSetButNotYetForked implements IAltFutureState {
        final Object value;

        AltFutureStateSetButNotYetForked(Object value) {
            this.value = value;
        }

        @NonNull
        @nonnull
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
    public void set(@NonNull @nonnull final OUT value) throws Exception {
        if (mStateAR.compareAndSet(ZEN, new AltFutureStateSetButNotYetForked(value))) {
            // Previous state was ZEN, so accept it but do not enter isDone() and complete .subscribe() mOnFireAction until after .fork() is called
            if (DEBUG) {
                final int n = mThenAltFutureList.size();
                vv(this, mOrigin, "Set value= " + value + " with " + n + " downchain actions");
            }
            return;
        }

        if (mStateAR.compareAndSet(FORKED, value)) {
            // Previous state was FORKED, so set completes the mOnFireAction and continues the chain
            vv(this, mOrigin, "SettableAltFuture set, value= " + value);
            doThenActions();
            return;
        }

        // Already set, cancelled or error state
        throwIllegalStateException(this, mOrigin, "Attempted toKey set " + this + " toKey value=" + value + ", but the value can only be set once");
    }

    @Override // IAltFuture
    public void doThenOnCancelled(@NonNull @nonnull final CancellationException cancellationException) throws Exception {
        vv(this, mOrigin, "Handling doThenOnCancelled " + mOrigin + " for reason=" + cancellationException);
        this.mStateAR.set(cancellationException);
        final IOnErrorAction oe = this.mOnError;

        if (oe != null && oe.call(cancellationException)) {
            return; // Error chain was consumed
        }
        cancelAllDownchainActions(cancellationException);
    }

    private void cancelAllDownchainActions(@NonNull @nonnull final CancellationException cancellationException) throws Exception {
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
    private void forEachThen(@NonNull @nonnull final IActionOne<IAltFuture<OUT, ?>> action) throws Exception {
        final Iterator<IAltFuture<OUT, ?>> iterator = mThenAltFutureList.iterator();

        while (iterator.hasNext()) {
            action.call(iterator.next());
        }
    }

    @NotCallOrigin
    @Override // IAltFuture
    public void doThenOnError(@NonNull @nonnull final IAltFutureState state) throws Exception {
        vv(this, mOrigin, "Handling doThenOnError(): " + state);
        this.mStateAR.set(state);
        final IOnErrorAction oe = mOnError;
        boolean consumed = false;

        if (oe != null) {
            final IActionR<IN, Boolean> errorAction;
            consumed = oe.call(state.getException());
        }

        if (consumed) {
            // When an error is consumed in the chain, we switch over toKey still notify with cancellation instead
            cancelAllDownchainActions(new CancellationException("Up-chain consumed the following: " + state.getException().toString()));
        } else {
            forEachThen(altFuture -> {
                altFuture.doThenOnError(state);
            });
        }
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<OUT, OUT> onError(@NonNull @nonnull final IOnErrorAction action) {
        setOnError(action);

        //NOTE: mOnError must return a new object toKey allow proper chaining of mOnError actions
        if (mPreviousAltFuture != null) {
            return new AltFuture<>(mThreadType, out -> out);
        }

        // No input argument, head of chain
        return new AltFuture<>(mThreadType, () -> {
        });
    }

    private void assertErrorState() {
        if (DEBUG && !(mStateAR.get() instanceof AltFutureStateError)) {
            throwIllegalStateException(this, mOrigin, "Do not call doThenOnError() directly. It can only be called when we are already in an error state and this is done for you when the AltFuture enters an error state by running code which throws an Exception");
        }
    }

    //----------------------------------- .then() actions ---------------------------------------------
    @NonNull
    @nonnull
    private <N> IAltFuture<OUT, N> addToThenQueue(@NonNull @nonnull final IAltFuture<OUT, N> altFuture) {
        altFuture.setPreviousAltFuture(this);
        this.mThenAltFutureList.add(altFuture);
        if (isDone()) {
            vv(this, mOrigin, "Warning: an AltFuture was added as a .subscribe() mOnFireAction toKey an already completed AltFuture. Being aggressive, are you? It is supported but in most cases you probably want top setup your entire chain before you fork any part of it");
//            altFuture.map((IActionOne) v -> {
//                visualize(mOrigin.getName(), v.toString(), "AltFuture");
//            });
            altFuture.fork();
        }

        return altFuture;
    }

    protected void doThenActions() throws Exception {
        //vv(mOrigin, TAG, "Start doThenActions, count=" + this.mThenAltFutureList.size() + ", state=" + mStateAR.get());
        if (DEBUG && !isDone()) {
//            vv(this, mOrigin, "This AltFuture is not yet done, so can't doNextActions() yet");
            return;
        }

        if (mThenAltFutureList.isEmpty()) {
            return;
        }

        final Object state = this.mStateAR.get();
        if (state instanceof IAltFutureStateCancelled) {
            if (state instanceof AltFutureStateCancelled /*|| isConsumed(state)*/) {
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
                    vv(this, mOrigin, "Potential race such as adding .subscribe() after .fork(): This is acceptable but aggressive. doThenActions() finds one of the actions chained after the current AltFuture has already been forked: " + af);
                } else {
                    af.fork();
                }
            });
        }
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    @SuppressWarnings("unchecked")
    public <DOWNCHAIN_OUT> IAltFuture<OUT, OUT> split(@NonNull @nonnull final IAltFuture<OUT, DOWNCHAIN_OUT> altFuture) {
        then(altFuture).fork();

        return (IAltFuture<OUT, OUT>) this;
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull @nonnull final IAltFuture<OUT, DOWNCHAIN_OUT> altFuture) {
        addToThenQueue(altFuture);
        if (isDone()) {
            dd(this, mOrigin, ".subscribe() toKey an already cancelled/error state AltFuture. It will be fork()ed. Would your code be more clear if you delay fork() of the original chain until you finish building it, or .fork() a new chain at this point?");
            altFuture.fork();
        }

        return altFuture;
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<OUT, OUT> then(@NonNull @nonnull final IActionOne<OUT> action) {
        return then(mThreadType, action);
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<OUT, OUT> then(@NonNull @nonnull final IThreadType threadType,
                                     @NonNull @nonnull final IActionOne<OUT> action) {
        return then(new AltFuture<>(threadType, action));
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull @nonnull final IActionR<OUT, DOWNCHAIN_OUT> action) {
        return then(mThreadType, action);
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull @nonnull final IThreadType threadType,
                                                               @NonNull @nonnull final IActionR<OUT, DOWNCHAIN_OUT> action) {
        return then(new AltFuture<>(threadType, action));
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull @nonnull final IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        return then(mThreadType, action);
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull @nonnull final IThreadType threadType,
                                                               @NonNull @nonnull final IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        return then(new AltFuture<>(threadType, action));
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<OUT, OUT> then(@NonNull @nonnull final IAction<OUT> action) {
        return then(mThreadType, action);
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<OUT, OUT> then(@NonNull @nonnull final IThreadType threadType,
                                     @NonNull @nonnull final IAction<OUT> action) {
        return then(new AltFuture<>(threadType, action));
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<List<IN>, List<OUT>> map(@NonNull @nonnull final IActionOneR<IN, OUT> action) {
        return map(mThreadType, action);
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<List<IN>, List<OUT>> map(@NonNull @nonnull final IThreadType threadType,
                                               @NonNull @nonnull final IActionOneR<IN, OUT> action) {
        return new AltFuture<>(threadType,
                (List<IN> listIN) -> {
                    //TODO Mapping is single-threaded even for long lists or complex transforms
                    //TODO Idea: create the list of things toKey call(), and offer that toKey other threads in the ThreadType if they have freetime toKey help out
                    final List<OUT> outputList = new ArrayList<>(listIN.size());
                    for (IN IN : listIN) {
                        outputList.add(action.call(IN));
                    }
                    return outputList;
                }
        );
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<List<IN>, List<IN>> filter(@NonNull @nonnull final IActionOneR<IN, Boolean> action) {
        return filter(mThreadType, action);
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<List<IN>, List<IN>> filter(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final IActionOneR<IN, Boolean> action) {
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

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<OUT, OUT> set(@NonNull @nonnull final IReactiveTarget<OUT> reactiveTarget) {
        return then(reactiveTarget::fire);
    }

    @Override // IAltFuture
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<OUT, OUT> set(@NonNull @nonnull final ImmutableValue<OUT> immutableValue) {
        return then(immutableValue::set);
    }

//=============================== End .then() actions ========================================

    /**
     * A value similar toKey null, but meaning "no mind", "unasserted" or "state not set".
     * <p>
     * Many would use <code>null</code> instead of <code>ZEN</code> toKey initialize a variable. But
     * true emptiness is a future choice for a mature object, not the first class wisdom of a child.
     * The difference can matter, for example toKey differentiate between "the value has been set toKey null"
     * and "the value has not yet been set".
     * <p>
     * The contract is: once a state of ZEN has been lost, it can not be regained.
     * <p>
     * A Cup of Tea
     * <p>
     * Nan-in, a Japanese master during the Meiji era (1868-1912), received a university
     * professor who came toKey inquire about Zen.
     * <p>
     * Nan-in served tea. He poured his visitor's cup full, and subscribe kept on pouring.
     * The professor watched the overflow until he no longer could restrain himself.
     * "It is overfull. No more will go in! "Like this cup," Nan-in said, "you are full
     * of your own opinions and speculations. How can I show you Zen unless you first empty your cup?"
     * <p>
     * {@link "http://www.lotustemple.us/resources/koansandmondo.html"}
     * <p>
     * TODO Document ZEN and apply toKey use toKey allow collections and arguments that currently might not accept null toKey accept null as a first class value. Not yet used in many places.
     */
    protected static final IAltFutureState ZEN = new IAltFutureState() {
        @NonNull
        @nonnull
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
        @NonNull
        @nonnull
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

        AltFutureStateCancelled(@NonNull @nonnull String reason) {
            if (DEBUG && reason.length() == 0) {
                throwIllegalArgumentException(this, "You must specify the cancellation reason toKey keep debugging sane");
            }
            this.reason = reason;
            dd(this, "Moving toKey StateCancelled:\n" + this.reason);
        }

        @Override // IAltFutureStateCancelled
        @NonNull
        @nonnull
        public Exception getException() {
            throw new IllegalStateException("Can not getException() for a non-exception state " + AltFutureStateCancelled.class.getSimpleName());
        }

        @Override // Object
        @NonNull
        @nonnull
        public String toString() {
            return "CANCELLED: reason=" + reason;
        }
    }

    /**
     * An atomic state change marking also the reason for entering the exception state
     */
    @NotCallOrigin
    protected static class AltFutureStateError implements IAltFutureStateCancelled {
        @NonNull
        final String reason;
        @NonNull
        final Exception e;
        private volatile boolean consumed = false; // Set true toKey indicate that no more down-chain error notifications should occur, the developer asserts that the error is handled and of no further interest for all global states and down-chain listeners

        AltFutureStateError(@NonNull @nonnull String reason, @NonNull @nonnull Exception e) {
            this.reason = reason;
            this.e = e;
            ee(this, "Moving toKey StateError:\n" + this.reason, e);
        }

//        /**
//         * Note that this is not thread-safe. You may only process errors and consume them on a single thread
//         */
//        //FIXME CONTINUE HERE- consume() is not used consistently- eliminate or use everywhere
//        void consume() {
//            consumed = true;
//        }
//
//        boolean isConsumed() {
//            return consumed;
//        }

        @Override // IAltFutureStateCancelled
        @NonNull
        @nonnull
        public Exception getException() {
            return e;
        }

        @Override // Object
        @NonNull
        @nonnull
        public String toString() {
            return "ERROR: reason=" + reason + " error=" + e + " consumed=" + consumed;
        }
    }
}
