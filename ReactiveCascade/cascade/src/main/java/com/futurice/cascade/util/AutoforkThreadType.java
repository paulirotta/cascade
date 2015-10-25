/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.IThreadType;

import java.util.List;
import java.util.concurrent.Future;

/**
 * A base class which automatically starts processing action at the head of a functional chain
 * <p>
 * Created by phou on 14-Sep-15.
 */
public class AutoforkThreadType implements IThreadType {
    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public boolean isInOrderExecutor() {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public <IN> void execute(@NonNull  IAction<IN> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void run(@NonNull  Runnable runnable) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public <IN> void run(@NonNull  IAction<IN> action, @NonNull  IOnErrorAction onErrorAction) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public <IN> void runNext(@NonNull  IAction<IN> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void runNext(@NonNull  Runnable runnable) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public boolean moveToHeadOfQueue(@NonNull  Runnable runnable) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public <IN> void runNext(@NonNull  IAction<IN> action, @NonNull  IOnErrorAction onErrorAction) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    public <IN> Runnable wrapActionWithErrorProtection(@NonNull  IAction<IN> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    public <IN> Runnable wrapActionWithErrorProtection(@NonNull  IAction<IN> action, @NonNull  IOnErrorAction onErrorAction) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    public <IN> IAltFuture<IN, IN> then(@NonNull  IAction<IN> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    public <IN> IAltFuture<IN, IN> then(@NonNull  IActionOne<IN> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <IN> List<IAltFuture<IN, IN>> then(@NonNull  IAction<IN>... actions) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    public <IN> IAltFuture<?, IN> from(@NonNull  IN value) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull    @Override
    public <IN> IAltFuture<?, IN> from() {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    public <IN, OUT> IAltFuture<IN, OUT> then(@NonNull  IActionR<IN, OUT> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <IN, OUT> List<IAltFuture<IN, OUT>> then(@NonNull  IActionR<IN, OUT>... actions) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    public <IN, OUT> IAltFuture<IN, OUT> map(@NonNull  IActionOneR<IN, OUT> action) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <IN, OUT> List<IAltFuture<IN, OUT>> map(@NonNull  IActionOneR<IN, OUT>... actions) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

//    @Override
//    public <IN, OUT> void fork(@NonNull  IRunnableAltFuture<IN, OUT> runnableAltFuture) {
//        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
//    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public boolean isShutdown() {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    public <IN> Future<Boolean> shutdown(long timeoutMillis, @Nullable  IAction<IN> afterShutdownAction) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    public <IN> List<Runnable> shutdownNow(@NonNull  String reason, @Nullable  IAction<IN> actionOnDedicatedThreadAfterAlreadyStartedTasksComplete, @Nullable  IAction<IN> actionOnDedicatedThreadIfTimeout, long timeoutMillis) {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }

    /**
     /**
     * This is a marker class only.
     *
     * @throws UnsupportedOperationException
     */
    @NonNull
    @Override
    public String getName() {
        throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
    }
}
