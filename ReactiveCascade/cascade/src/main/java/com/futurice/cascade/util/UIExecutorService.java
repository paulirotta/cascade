/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.util;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.futurice.cascade.Async;
import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.NotCallOrigin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.futurice.cascade.Async.ii;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.throwIllegalStateException;
import static com.futurice.cascade.Async.throwRuntimeException;

/**
 * Treat the system UI thread as an ExecutorService
 * <p>
 * This is done to allow UI codes to be part of UI {@link com.futurice.cascade.i.IThreadType}
 * so that {@link IAltFuture} objects can be easily defined to
 * run on the UI thread in {@link Async#UI} vs background worker threads.
 * <p>
 * Since the system UI thread runs forever, not all {@link java.util.concurrent.ExecutorService}
 * items, for example related to lifecycle. make sense to implement.
 */
public final class UIExecutorService implements ExecutorService {
    @NonNull    private final Handler mHandler;
    @NonNull    private final ImmutableValue<String> mOrigin;

    public UIExecutorService(@NonNull  final Handler handler) {
        this.mHandler = handler;
        this.mOrigin = originAsync();
    }

    @Override // ExecutorService
    public void shutdown() {
        ii(mOrigin, "shutdown() called on UiAsync default ExecutorService");
        throw new UnsupportedOperationException("Shutdown() called on UiAsync default ExecutorService");
    }

    @NonNull
    @Override // ExecutorService
    public List<Runnable> shutdownNow() {
        ii(mOrigin, "shutdownNow() called on UiAsync default ExecutorService");
        throw new UnsupportedOperationException("ShutdownNow() called on UiAsync default ExecutorService");
    }

    @Override // ExecutorService
    public boolean isShutdown() {
        return false;
    }

    @Override // ExecutorService
    public boolean isTerminated() {
        return false;
    }

    @Override // ExecutorService
    public boolean awaitTermination(
            final long timeout,
            @NonNull  final TimeUnit unit)
            throws InterruptedException {
        ii(mOrigin, "awaitTermination() called on UiAsync default ExecutorService");
        throw new UnsupportedOperationException("awaitTermination() called on UiAsync default ExecutorService");
    }

    @NonNull
    @Override // ExecutorService
    public <T> Future<T> submit(@NonNull  final Callable<T> callable) {
        final FutureTask<T> future = new FutureTask<>(callable);
        execute(future);

        return future;
    }

    @NonNull
    @Override // ExecutorService
    public <T> Future<T> submit(
            @NonNull  final Runnable runnable,
            @NonNull  final T result) {
        final FutureTask<T> future = new FutureTask<>(() -> {
            runnable.run();
            return result;
        });
        execute(future);

        return future;
    }

    @NonNull
    @NotCallOrigin
    @Override // ExecutorService
    public Future submit(@NonNull  final Runnable runnable) {
        if (runnable instanceof RunnableFuture) {
            mHandler.post(runnable);
            return (Future) runnable;
        }

        FutureTask<Object> future = new FutureTask<>(new Callable<Object>() {
            @Override
            @NotCallOrigin
            @Nullable            public Object call() throws Exception {
                runnable.run();
                return null;
            }
        });

        mHandler.post(future);

        return future;
    }

    @Override // ExecutorService
    @WorkerThread
    public <T> List<Future<T>> invokeAll(
            @NonNull  final Collection<? extends Callable<T>> callables)
            throws InterruptedException, NullPointerException, RejectedExecutionException {
        final ArrayList<Future<T>> futures = new ArrayList<>(callables.size());
        if (callables.size() > 0) {
            for (Callable<T> callable : callables) {
                futures.add(submit(callable));
            }
            try {
                futures.get(futures.size() - 1).get();
            } catch (ExecutionException e) {
                throwRuntimeException(mOrigin, "Can not get() last element of invokeAll()", e);
            }
        }

        return futures;
    }

    @NonNull
    @Override // ExecutorService
    @WorkerThread
    public <T> List<Future<T>> invokeAll(
            @NonNull  final Collection<? extends Callable<T>> callables,
            final long timeout,
            @NonNull  final TimeUnit unit)
            throws InterruptedException, NullPointerException, RejectedExecutionException {
        final ArrayList<Future<T>> futures = new ArrayList<>(callables.size());
        if (callables.size() > 0) {
            for (Callable<T> callable : callables) {
                futures.add(submit(callable));
            }
            try {
                futures.get(futures.size() - 1).get(timeout, unit);
            } catch (ExecutionException e) {
                throwRuntimeException(mOrigin, "Can not get() last element of invokeAll()", e);
            } catch (TimeoutException e) {
                throwRuntimeException(mOrigin, "Timeout waiting to get() last element of invokeAll()", e);
            }
        }

        return futures;
    }

    @Override // ExecutorService
    @WorkerThread
    public <T> T invokeAny(
            @NonNull  final Collection<? extends Callable<T>> callables)
            throws InterruptedException, NullPointerException, RejectedExecutionException, ExecutionException {
        final ArrayList<Future<T>> futures = new ArrayList<>(callables.size());
        if (callables.size() == 0) {
            throw new NullPointerException("Empty list can not invokeAny() as there is no value to return");
        }
        for (Callable<T> callable : callables) {
            futures.add(submit(callable));
        }

        return futures.get(0).get();
    }

    @NonNull
    @Override // ExecutorService
    @WorkerThread
    public <T> T invokeAny(
            @NonNull  final Collection<? extends Callable<T>> callables,
            final long timeout,
            @NonNull  final TimeUnit unit)
            throws InterruptedException, NullPointerException, RejectedExecutionException, TimeoutException, ExecutionException {
        final ArrayList<Future<T>> futures = new ArrayList<>(callables.size());
        if (callables.size() == 0) {
            throw new NullPointerException("Empty list can not invokeAny() as there is no value to return");
        }
        for (Callable<T> callable : callables) {
            futures.add(submit(callable));
        }

        return futures.get(0).get(timeout, unit);
    }

    @Override // ExecutorService
    public void execute(@NonNull  final Runnable command) {
        if (!mHandler.post(command)) {
            throwIllegalStateException(mOrigin, "Can not Handler.post() to UIThread in this Context right now, probably app is shutting down");
        }
    }
}
