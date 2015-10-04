package com.futurice.cascade.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.Async;
import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.active.IRunnableAltFuture;
import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;

import java.util.List;
import java.util.concurrent.Future;

import static com.futurice.cascade.Async.assertEqual;

/**
 * A base class which automatically initiates {@link IThreadType#fork(IRunnableAltFuture)} operations on the head of a functional chain
 *
 * Created by phou on 14-Sep-15.
 */
public class AutoforkThreadType implements IThreadType {
    private int mChainedActionsCurrentlyBeingDefined = 0;

    @Override
    public boolean isInOrderExecutor() {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @Override
    public <IN> void execute(@NonNull @nonnull IAction<IN> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @Override
    public void run(@NonNull @nonnull Runnable runnable) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @Override
    public <IN> void run(@NonNull @nonnull IAction<IN> action, @NonNull @nonnull IOnErrorAction onErrorAction) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @Override
    public <IN> void runNext(@NonNull @nonnull IAction<IN> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @Override
    public void runNext(@NonNull @nonnull Runnable runnable) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @Override
    public boolean moveToHeadOfQueue(@NonNull @nonnull Runnable runnable) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @Override
    public <IN> void runNext(@NonNull @nonnull IAction<IN> action, @NonNull @nonnull IOnErrorAction onErrorAction) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    public <IN> Runnable wrapActionWithErrorProtection(@NonNull @nonnull IAction<IN> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    public <IN> Runnable wrapActionWithErrorProtection(@NonNull @nonnull IAction<IN> action, @NonNull @nonnull IOnErrorAction onErrorAction) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    public <IN> IAltFuture<IN, IN> then(@NonNull @nonnull IAction<IN> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    public <IN> IAltFuture<IN, IN> then(@NonNull @nonnull IActionOne<IN> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <IN> List<IAltFuture<IN, IN>> then(@NonNull @nonnull IAction<IN>... actions) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    public <IN> IAltFuture<?, IN> from(@NonNull @nonnull IN value) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    public <IN, OUT> IAltFuture<IN, OUT> then(@NonNull @nonnull IActionR<IN, OUT> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <IN, OUT> List<IAltFuture<IN, OUT>> then(@NonNull @nonnull IActionR<IN, OUT>... actions) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    public <IN, OUT> IAltFuture<IN, OUT> map(@NonNull @nonnull IActionOneR<IN, OUT> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <IN, OUT> List<IAltFuture<IN, OUT>> map(@NonNull @nonnull IActionOneR<IN, OUT>... actions) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @Override
    public <IN, OUT> void fork(@NonNull @nonnull IRunnableAltFuture<IN, OUT> runnableAltFuture) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @Override // IThreadType
    public void startDefiningFunctionChain() {
        assertEqual(Async.currentThreadType(), this);
        mChainedActionsCurrentlyBeingDefined++;
    }

    @Override // IThreadType
    public <IN, OUT> void endDefiningFunctionChain(@NonNull @nonnull final IRunnableAltFuture<IN, OUT> action) {
        assertEqual(Async.currentThreadType(), this);
        mChainedActionsCurrentlyBeingDefined--;
        if (mChainedActionsCurrentlyBeingDefined == 0) {
            fork(action);
        }
    }

    @Override
    public boolean isShutdown() {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    public <IN> Future<Boolean> shutdown(long timeoutMillis, @Nullable @nullable IAction<IN> afterShutdownAction) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    public <IN> List<Runnable> shutdownNow(@NonNull @nonnull String reason, @Nullable @nullable IAction<IN> actionOnDedicatedThreadAfterAlreadyStartedTasksComplete, @Nullable @nullable IAction<IN> actionOnDedicatedThreadIfTimeout, long timeoutMillis) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    @NonNull
    @nonnull
    @Override
    public String getName() {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }
}
