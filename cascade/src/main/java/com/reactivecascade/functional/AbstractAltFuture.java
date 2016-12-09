/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.functional;

import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.reactivecascade.Async;
import com.reactivecascade.i.CallOrigin;
import com.reactivecascade.i.IAction;
import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IActionOneR;
import com.reactivecascade.i.IActionR;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IReactiveTarget;
import com.reactivecascade.i.ISettableAltFuture;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.AssertUtil;
import com.reactivecascade.util.Origin;
import com.reactivecascade.util.RCLog;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.reactivecascade.i.IAltFuture.AltFutureState.CANCELLED;
import static com.reactivecascade.i.IAltFuture.AltFutureState.ERROR;
import static com.reactivecascade.i.IAltFuture.AltFutureState.FORKED;
import static com.reactivecascade.i.IAltFuture.AltFutureState.PENDING;

/**
 * The common base class for default implementations such as {@link SettableAltFuture} and {@link RunnableAltFuture}.
 * Most developers will not need to concern themselves with this abstract class.
 * <p>
 * {@link RunnableAltFuture} overrides this class.
 * TODO You may also use a {@link SettableAltFuture} to inject data where the from is determined from entirely outside of the current chain hierarchy.
 * This is currently an experimental feature so be warned, your results and chain behaviour may vary. Additional
 * testing is on the long list.
 * <p>
 * You may prefer to use {@link ImmutableValue} that a similar need in some cases. That is a
 * slightly faster, simpler implementation than {@link SettableAltFuture}.
 * <p>
 */
@NotCallOrigin
public abstract class AbstractAltFuture<IN, OUT> extends Origin implements IAltFuture<IN, OUT> {
    protected final AtomicReference<Object> stateAR = new AtomicReference<>(PENDING);

    @NonNull
    protected final IThreadType threadType;

    protected final CopyOnWriteArraySet<IAltFuture<? extends OUT, ?>> downchainAltFutures = new CopyOnWriteArraySet<>();

    private final AtomicReference<IAltFuture<?, ? extends IN>> upchainAltFutureAR = new AtomicReference<>();

    /**
     * Create, from is not yet determined
     *
     * @param threadType on which this alt future will evaluate and fire downchain events
     */
    public AbstractAltFuture(@NonNull final IThreadType threadType) {
        this.threadType = AssertUtil.assertNonNull(threadType);
    }

    @NonNull
    @Override
    public AltFutureState getState() {
        Object state = stateAR.get();

        if (state == CANCELLED || state == AltFutureState.DONE || state == AltFutureState.ERROR || state == FORKED) {
            return (AltFutureState) state;
        }
        return AltFutureState.DONE; //TODO IS this correct or a
    }

    @Override // IAltFuture
    @CallOrigin
    @CallSuper
    public boolean cancel(@NonNull CharSequence reason) {
//        AltFutureStateCancelled state = new AltFutureStateCancelled(reason);

        if (stateAR.compareAndSet(PENDING, CANCELLED) || stateAR.compareAndSet(FORKED, CANCELLED)) {
            RCLog.d(this, "Cancelled: reason=" + reason);
            String s = "Upchain cancelled: " + reason;
            Exception e = forEachThen(action -> {
                action.cancel(s);
            });
            AssertUtil.assertEqual("Exception during downchain onCancelled(\"" + reason + "\")", null, e);
            return true;
        }

        Object s = stateAR.get();

        if (s == CANCELLED) {
            RCLog.d(this, "Ignoring duplicate cancel(\"" + reason + "\"). State=" + s);
        } else {
            RCLog.d(this, "Ignoring cancel(\"" + reason + "\"). State=" + s);
        }

        return false;
    }

//    @Override // IAltFuture
//    public boolean cancel(@NonNull StateError stateError) {
//        Object state = this.stateAR.get();
//        StateCancelled stateCancelled = new StateCancelled() {
//            private final ImmutableValue<String> mOrigin = RCLog.originAsync();
//
//            @NonNull
//            @Override
//            public ImmutableValue<String> getOrigin() {
//                return mOrigin;
//            }
//
//            @NonNull
//            @Override
//            public String getReason() {
//                return "Cancelled by upchain error=" + getStateError();
//            }
//
//            @Nullable
//            @Override
//            public StateError getStateError() {
//                return stateError;
//            }
//        };
//
//        if (stateAR.compareAndSet(PENDING, stateCancelled) || stateAR.compareAndSet(FORKED, stateCancelled)) {
//            RCLog.d(this, "Cancelled from State " + state);
//            final Exception e = forEachThen(ignore ->
//                    onCancelled(stateCancelled));
//            if (e != null) {
//                RCLog.throwRuntimeException(this, "Problem executing onCancelled() downchain actions", e);
//            }
//
//            return true;
//        }
//
//        RCLog.d(this, "Ignoring cancel(" + stateError + "). State=" + stateAR.get());
//
//        return false;
//    }

    @Override // IAltFuture
    public boolean isCancelled() {
        return isCancelled(stateAR.get());
    }

    private boolean isCancelled(@NonNull Object objectThatMayBeAState) {
        return objectThatMayBeAState == CANCELLED;
    }

    @Override // IAltFuture
    public final boolean isDone() {
        return isDone(stateAR.get());
    }

    protected boolean isDone(@NonNull Object state) {
        return state != PENDING && state != FORKED;
    }

    @Override // IAltFuture
    public final boolean isForked() {
        return isForked(stateAR.get());
    }

    protected boolean isForked(@NonNull Object state) {
        return state != PENDING;
    }

    @Override // IAltFuture
    @NonNull
    public IAltFuture<IN, OUT> fork() {
        IAltFuture<?, ? extends IN> previousAltFuture = getUpchain();

        if (previousAltFuture != null && !previousAltFuture.isDone()) {
            RCLog.v(this, "Previous IAltFuture not forked, searching upchain: " + previousAltFuture);
            previousAltFuture.fork();
            return this;
        }

        Object s = null;
        if (Async.USE_FORKED_STATE ? !stateAR.compareAndSet(PENDING, FORKED) : (s = stateAR.get()) != PENDING) {
            if (s == null) {
                s = stateAR.get();
            }
            if (s == CANCELLED || s == ERROR) {
                RCLog.v(getOrigin(), "Can not fork(), RunnableAltFuture was cancelled: " + s);
                return this;
            }
            RCLog.i(getOrigin(), "Possibly a legitimate race condition. Ignoring duplicate fork(), already fork()ed or set(): " + s);
            return this;
        }
        doFork();

        return this;
    }

    protected abstract void doFork();

    /**
     * Implementations of {@link #fork()} must call this when completed. It reduces the window of time
     * in which past intermediate calculation values in a active chain are held in memory. It is
     * the equivalent of the (illegal) statement:
     * <code>{@link #setUpchain(IAltFuture)}</code> to null.
     * <p>
     * This may not be done until {@link #isDone()} == true, such as when the {@link #fork()} has completed.
     */
    protected final void clearPreviousAltFuture() {
        AssertUtil.assertTrue(isDone());
        this.upchainAltFutureAR.lazySet(null);
    }

    @Override // IAltFuture
    @Nullable
    public final IAltFuture<?, ? extends IN> getUpchain() {
        return this.upchainAltFutureAR.get();
    }

    @Override // IAltFuture
    public void setUpchain(@NonNull IAltFuture<?, ? extends IN> altFuture) {
        boolean set = this.upchainAltFutureAR.compareAndSet(null, altFuture);

        if (!set) {
            RCLog.v(this, "Second setUpchain(), merging two chains. Neither can proceed past this point until both burn to this point.");
        }
    }

    @Override // IAltFuture
    @NonNull
    @SuppressWarnings("unchecked")
    public OUT get() {
        Object state = stateAR.get();

        if (!isDone(state)) {
            RCLog.throwIllegalStateException(this, getOrigin(), "Attempt to get() RunnableAltFuture that is not yet finished. State=" + state);
        }
        if (isCancelled(state)) {
            RCLog.throwIllegalStateException(this, getOrigin(), "Attempt to get() RunnableAltFuture that is cancelled: State=" + state);
        }

        return (OUT) state;
    }

    @Override // IAltFuture
    @NonNull
    @SuppressWarnings("unchecked")
    public OUT unsafeGet() {
        Object state = stateAR.get();

        if (!isDone(state) || isCancelled(state)) {
            return (OUT) PENDING;
        }

        return (OUT) state;

    }

    @Override // IAltFuture
    @NonNull
    public final IThreadType getThreadType() {
        return this.threadType;
    }

    /**
     * Perform some action on an instantaneous snapshot of the list of .sub() down-chain actions
     *
     * @param action
     * @throws Exception
     */
    Exception forEachThen(@NonNull IActionOne<IAltFuture<? extends OUT, ?>> action) {
        Exception exception = null;

        for (IAltFuture<? extends OUT, ?> altFuture : downchainAltFutures) {
            try {
                action.call(altFuture);
            } catch (Exception e) {
                if (exception == null) {
                    exception = e;
                }
                RCLog.e(this, "Problem with forEachThen(): " + e);
            }
        }

        return exception;
    }

    @Override // IAltFuture
    public void onCancelled(@NonNull CharSequence reason) throws Exception {
        RCLog.v(this, "Handling onCancelled for reason=" + reason);
        if (!this.stateAR.compareAndSet(PENDING, CANCELLED) && !this.stateAR.compareAndSet(FORKED, CANCELLED)) {
            RCLog.i(this, "Can not onCancelled because IAltFuture State is already determined: " + stateAR.get());
            return;
        }

        Exception e = forEachThen(altFuture -> {
            altFuture.onCancelled(reason);
        });

        if (e != null) {
            throw e;
        }
    }

    @NotCallOrigin
    @NonNull
    @SuppressWarnings("unchecked")
    @Override // IAltFuture
    public ISettableAltFuture<OUT> onCancelled(@NonNull IActionOne<CharSequence> onCancelledAction) {
        return (ISettableAltFuture<OUT>) then(new OnCancelledAltFuture<>(threadType, onCancelledAction));
    }

    @NotCallOrigin
    @Override // IAltFuture
    public void onError(@NonNull Exception e) throws Exception {
        RCLog.d(this, "Handling onError(): " + e);

        if (!this.stateAR.compareAndSet(PENDING, ERROR) || (Async.USE_FORKED_STATE && !this.stateAR.compareAndSet(FORKED, ERROR))) {
            RCLog.i(this, "Will not repeat onError() because IAltFuture State is already determined: " + stateAR.get());
            return;
        }

        Exception e2 = forEachThen(af -> {
            af.onError(e);
        });

        if (e2 != null) {
            throw e2;
        }
    }

    @NotCallOrigin
    @NonNull
    @SuppressWarnings("unchecked")
    @Override // IAltFuture
    public ISettableAltFuture<OUT> onError(@NonNull IActionOne<Exception> onErrorAction) {
        return (ISettableAltFuture<OUT>) then(new OnErrorAltFuture<>(threadType, onErrorAction));
    }

    @Override // Object
    @NonNull
    public String toString() {
        return this.stateAR.get().toString();
    }

    //----------------------------------- .then() actions ---------------------------------------------
    protected void doThen() {
        AssertUtil.assertTrue("doThen(): State=" + stateAR.get(), isDone());

        Exception e = forEachThen(IAltFuture::fork);
        if (e != null) {
            throw new IllegalStateException("Problem completing downchain actions", e);
        }
    }

    /**
     * Continue downchain actions on the specified {@link IThreadType}
     *
     * @param theadType the thread execution group to change to for the next chain operation
     * @return the previous chain link masked to reflect the new {@link IThreadType}
     */
    @NonNull
    @Override
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, OUT> on(@NonNull IThreadType theadType) {
        if (theadType == threadType) {
            return this;
        }

        return then(new SettableAltFuture<>(theadType));
    }

    @NonNull
    @Override
    public IAltFuture<OUT, OUT> then(@NonNull IAction<OUT> action) {
        return then(new RunnableAltFuture<OUT, OUT>(threadType, action));
    }

    @Override // IAltFuture
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<OUT, OUT> then(@NonNull IActionOne<OUT> action) {
        return then(new RunnableAltFuture<OUT, OUT>(threadType, action));
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public ISettableAltFuture<OUT> then(@NonNull IActionOne<OUT>... actions) {
        AssertUtil.assertTrue("then(IActionOne...) with empty list of upchain things to await makes no sense", actions.length > 0);
        AssertUtil.assertTrue("then(IActionOne...) with single item in the list of upchain things to await is confusing. Use .then() instead", actions.length != 1);

        IAltFuture<OUT, OUT>[] altFutures = new RunnableAltFuture[actions.length];

        for (int i = 0; i < actions.length; i++) {
            final IActionOne<OUT> a = actions[i];

            altFutures[i] = then(new RunnableAltFuture<>(threadType,
                    a));
        }

        return await((IAltFuture<?, ?>[]) altFutures);
    }

    @Override // IAltFuture
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull IActionR<DOWNCHAIN_OUT> action) {
        return then(new RunnableAltFuture<>(threadType, action));
    }

    @Override // IAltFuture
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull IAltFuture<OUT, DOWNCHAIN_OUT> altFuture) {
        altFuture.setUpchain(this);

        this.downchainAltFutures.add(altFuture);
        if (isDone()) {
            altFuture.fork();
        }

        return altFuture;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public ISettableAltFuture<OUT> then(@NonNull IAction<? extends OUT>... actions) {
        AssertUtil.assertTrue("then(IActionOne...) with empty list of upchain things to await makes no sense", actions.length == 0);
        AssertUtil.assertTrue("then(IActionOne...) with single item in the list of upchain things to await is confusing. Use .then() instead", actions.length == 1);

        IAltFuture<?, ? extends OUT>[] altFutures = new RunnableAltFuture[actions.length];

        for (int i = 0; i < actions.length; i++) {
            final IAction<? extends OUT> a = actions[i];

            altFutures[i] = then(new RunnableAltFuture<>(threadType, a));
        }

        return await(altFutures);
    }

    @Override // IAltFuture
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> map(@NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        return then(new RunnableAltFuture<>(threadType, action));
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT>[] map(@NonNull IActionOneR<OUT, DOWNCHAIN_OUT>... actions) {
        AssertUtil.assertTrue("map(IActionOneR...) with empty list of upchain things to await makes no sense", actions.length == 0);
        AssertUtil.assertTrue("map(IActionOneR...) with single item in the list of upchain things to await is confusing. Use .then() instead", actions.length == 1);

        IAltFuture<OUT, DOWNCHAIN_OUT>[] altFutures = new IAltFuture[actions.length];

        for (int i = 0; i < actions.length; i++) {
            IActionOneR<OUT, DOWNCHAIN_OUT> a = actions[i];

            altFutures[i] = new RunnableAltFuture<>(threadType, a);
        }

        return altFutures;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public ISettableAltFuture<OUT> sleep(long sleepTime,
                                         @NonNull TimeUnit timeUnit) {
        ISettableAltFuture<OUT> outAltFuture = new SettableAltFuture<>(threadType);

        outAltFuture.setUpchain(this);
        Async.TIMER.schedule(() -> {
            outAltFuture.set(get());
        }, sleepTime, timeUnit);

        return outAltFuture;
    }

    @NonNull
    @Override // IAltFuture
    public ISettableAltFuture<OUT> await(@NonNull IAltFuture<?, ?>... altFutures) {
        AssertUtil.assertTrue("await(IAltFuture...) with empty list of upchain things to await makes no sense", altFutures.length > 0);
        AssertUtil.assertTrue("await(IAltFuture...) with single item in the list of upchain things to await is confusing. Use .then() instead", altFutures.length != 1);

        ISettableAltFuture<OUT> outAltFuture = new SettableAltFuture<>(threadType);
        AtomicInteger downCounter = new AtomicInteger(altFutures.length);

        outAltFuture.setUpchain(this);
        for (IAltFuture<?, ?> upchainAltFuture : altFutures) {
            IAltFuture<?, ?> ignore = upchainAltFuture.then(() -> {
                if (downCounter.decrementAndGet() == 0) {
                    outAltFuture.set(get());
                }
            });
        }

        return outAltFuture;
    }

    @Override // IAltFuture
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<IN, IN> filter(@NonNull IActionOneR<IN, Boolean> action) {
        return new RunnableAltFuture<>(threadType, in -> {
            if (!action.call(in)) {
                cancel("Filtered: " + in);
            }
            return in;
        }
        );
    }

    @Override // IAltFuture
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<OUT, OUT> set(@NonNull IReactiveTarget<OUT> reactiveTarget) {
        return then(reactiveTarget::fire);
    }

//    protected static abstract class AbstractState extends Origin implements IAltFuture.State {
//    }

//=============================== End .then() actions ========================================

//    @NotCallOrigin
//    protected final class AltFutureStateCancelled extends Origin implements StateCancelled {
//        final String reason;
//
//        AltFutureStateCancelled(@NonNull String reason) {
//            if (BuildConfig.DEBUG && reason.length() == 0) {
//                throw new IllegalArgumentException("You must specify the cancellation reason to keep debugging sane");
//            }
//            this.reason = reason;
//            com.reactivecascade.util.RCLog.d(this, "Moving to StateCancelled:\n" + this.reason);
//        }
//
//        /**
//         * The reason this task was cancelled. This is for debug purposes.
//         *
//         * @return
//         */
//        @NonNull
//        @Override // StateCancelled
//        public String getReason() {
//            return reason;
//        }
//
//        /**
//         * If the cancellation is because of an error State change elsewhere, provide the details
//         * of that original cause also.
//         *
//         * @return
//         */
//        @Nullable
//        @Override
//        public StateError getStateError() {
//            return null;
//        }
//
//        @Override // Object
//        @NonNull
//        public String toString() {
//            return "CANCELLED: reason=" + reason;
//        }
//    }
//
//    /**
//     * An atomic State change marking also the reason for entering the exception State
//     */
//    @NotCallOrigin
//    protected final class AltFutureStateError extends Origin implements StateError {
//        @NonNull
//        final String reason;
//
//        @NonNull
//        final Exception e;
//
//        public AltFutureStateError(@NonNull String reason,
//                                   @NonNull Exception e) {
//            this.reason = reason;
//            this.e = e;
//            RCLog.e(this, "Moving to StateError:\n" + this.reason, e);
//        }
//
//        @Override // State
//        @NonNull
//        public Exception getException() {
//            return e;
//        }
//
//        @Override // Object
//        @NonNull
//        public String toString() {
//            return "ERROR: reason=" + reason + " error=" + e;
//        }
//    }
}
