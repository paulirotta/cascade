/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.functional;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.reactivecascade.i.IAction;
import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IActionOneR;
import com.reactivecascade.i.IActionR;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IReactiveTarget;
import com.reactivecascade.i.ISettableAltFuture;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.util.AssertUtil;
import com.reactivecascade.util.Origin;
import com.reactivecascade.util.RCLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A chain of two or more {@link com.reactivecascade.i.IAltFuture}s merged into a single logical entity.
 * <p>
 * This is useful for returning a single logical entity from an async method. It allows receiving values
 * at the head and output values to next chain steps at the tail.
 */
public class CompoundAltFuture<IN, OUT> extends Origin implements IAltFuture<IN, OUT> {
    protected final List<IAltFuture<?, ?>> mSubchain = new ArrayList<>();

    @NonNull
    protected final IAltFuture<IN, ?> head;

    @NonNull
    protected final IAltFuture<?, OUT> tail;

    public CompoundAltFuture(@NonNull IAltFuture<IN, ?> head,
                             @NonNull IAltFuture<?, OUT> tail) {
        AssertUtil.assertTrue("Head of CompoundAltFuture must not be downchain from an existing chain", head.getUpchain() == null);
        AssertUtil.assertNotEqual(head, tail);

        this.head = head;
        this.tail = tail;

        boolean foundHeadUpchainFromTail;
        IAltFuture<?, ?> previous = tail;
        do {
            mSubchain.add(0, previous);
            foundHeadUpchainFromTail = head.equals(previous);
            previous = previous.getUpchain();
        } while (!foundHeadUpchainFromTail && previous != null);
        mSubchain.add(0, head);
        if (!foundHeadUpchainFromTail) {
            RCLog.throwIllegalArgumentException(head, "Head of CompoundAltFuture must be upchain from tail");
        }
    }

    @NonNull
    @Override // IAltFuture
    public IThreadType getThreadType() {
        return head.getThreadType();
    }

    @Override // IAltFuture
    public boolean isDone() {
        return tail.isDone();
    }

    @Override // IAltFuture
    public boolean isForked() {
        return head.isForked();
    }

    @Override // IAltFuture
    public boolean cancel(@NonNull String reason) {
        for (IAltFuture<?, ?> altFuture : mSubchain) {
            if (altFuture.cancel(reason)) {
                return true;
            }
        }

        return false;
    }

    @Override // IAltFuture
    public boolean cancel(@NonNull StateError stateError) {
        for (IAltFuture<?, ?> altFuture : mSubchain) {
            if (altFuture.cancel(stateError)) {
                RCLog.d(this, "Cancelled task within CompountAltFuture");
                return true;
            }
        }

        return false;
    }

    @Override // IAltFuture
    public boolean isCancelled() {
        for (IAltFuture<?, ?> altFuture : mSubchain) {
            if (altFuture.isCancelled()) {
                RCLog.d(this, "CompountAltFuture is cancelled");
                return true;
            }
        }

        return false;
    }

    @NonNull
    @Override // IAltFuture
    public IAltFuture<IN, OUT> fork() {
        head.fork();
        return this;
    }

    @Nullable
    @Override // IAltFuture
    public IAltFuture<?, ? extends IN> getUpchain() {
        return head.getUpchain();
    }

    @Override // IAltFuture
    public void setUpchain(@NonNull IAltFuture<?, ? extends IN> altFuture) {
        head.setUpchain(altFuture);
    }

    @Override // IAltFuture
    public void onError(@NonNull StateError stateError) throws Exception {
        head.onError(stateError);
    }

    @Override // IAltFuture
    public void onCancelled(@NonNull StateCancelled stateCancelled) throws Exception {
        head.onCancelled(stateCancelled);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<OUT, OUT> then(@NonNull IAction<OUT> action) {
        return tail.then(action);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    public ISettableAltFuture<OUT> then(@NonNull IAction<? extends OUT>... actions) {
        return tail.then(actions);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<OUT, OUT> then(@NonNull IActionOne<OUT> action) {
        return tail.then(action);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    public ISettableAltFuture<OUT> then(@NonNull IActionOne<OUT>... actions) {
        return tail.then(actions);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull IActionR<DOWNCHAIN_OUT> action) {
        return tail.then(action);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull IAltFuture<OUT, DOWNCHAIN_OUT> altFuture) {
        return tail.then(altFuture);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> map(@NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        return tail.map(action);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    public <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT>[] map(@NonNull IActionOneR<OUT, DOWNCHAIN_OUT>... actions) {
        return tail.map(actions);
    }

    /**
     * Pause execution of this chain for a fixed time interval
     * <p>
     * Note that the chain realizes immediately in the event of {@link #cancel(String)} or a runtime error
     *
     * @param sleepTime
     * @param timeUnit
     * @return
     */
    @NonNull
    @Override
    public ISettableAltFuture<OUT> sleep(long sleepTime,
                                         @NonNull final TimeUnit timeUnit) {
        throw new UnsupportedOperationException("Not yet implemented"); //TODO sleep a compound alt future
    }

//    @NonNull
//    @Override // IAltFuture
//    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
//    public ISettableAltFuture<OUT> await(@NonNull IAltFuture<?, ?> altFuture) {
//        return tail.await(altFuture);
//    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    public ISettableAltFuture<OUT> await(@NonNull IAltFuture<?, ?>... altFuturesToJoin) {
        return tail.await(altFuturesToJoin);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, OUT> on(@NonNull IThreadType theadType) {
        if (theadType == tail.getThreadType()) {
            return this;
        }

        return tail.on(theadType);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<IN, IN> filter(@NonNull IActionOneR<IN, Boolean> action) {
        return head.filter(action);
    }

    @NonNull
    @Override // IAltFuture
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<OUT, OUT> set(@NonNull IReactiveTarget<OUT> reactiveTarget) {
        return tail.set(reactiveTarget);
    }

    @NonNull
    @Override // IAltFuture
    public ISettableAltFuture<OUT> onError(@NonNull IActionOne<Exception> onErrorAction) {
        return tail.onError(onErrorAction);
    }

    @NonNull
    @Override // IAltFuture
    public ISettableAltFuture<OUT> onCancelled(@NonNull IActionOne<String> onCancelledAction) {
        return tail.onCancelled(onCancelledAction);
    }

    @NonNull
    @Override // IAltFuture
    public OUT safeGet() {
        return tail.safeGet();
    }

    @NonNull
    @Override // IAltFuture
    public OUT get() {
        return tail.get();
    }
}
