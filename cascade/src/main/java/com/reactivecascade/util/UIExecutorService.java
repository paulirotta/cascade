/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.reactivecascade.Async;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.NotCallOrigin;

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

/**
 * Treat the system UI thread as an ExecutorService
 * <p>
 * This is done to allow UI codes to be part of UI {@link com.reactivecascade.i.IThreadType}
 * so that {@link IAltFuture} objects can be easily defined to
 * run on the UI thread in {@link Async#UI} vs background worker threads.
 * <p>
 * Since the system UI thread runs forever, not all {@link java.util.concurrent.ExecutorService}
 * items, for example related to lifecycle. make sense to implement.
 */
public final class UIExecutorService extends Origin implements ExecutorService {
    @NonNull
    private final Handler mHandler;

    public UIExecutorService(@NonNull Handler handler) {
        this.mHandler = handler;
    }

    @Override // ExecutorService
    public void shutdown() {
        RCLog.i(this, "shutdown() called on UiAsync default ExecutorService");
        throw new UnsupportedOperationException("Shutdown() called on UiAsync default ExecutorService");
    }

    @NonNull
    @Override // ExecutorService
    public List<Runnable> shutdownNow() {
        RCLog.i(this, "shutdownNow() called on UiAsync default ExecutorService");
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
    public boolean awaitTermination(long timeout,
                                    @NonNull TimeUnit unit) throws InterruptedException {
        RCLog.i(this, "awaitTermination() called on UiAsync default ExecutorService");
        throw new UnsupportedOperationException("awaitTermination() called on UiAsync default ExecutorService");
    }

    @NonNull
    @Override // ExecutorService
    public <T> Future<T> submit(@NonNull Callable<T> callable) {
        FutureTask<T> future = new FutureTask<>(callable);

        execute(future);

        return future;
    }

    @NonNull
    @Override // ExecutorService
    public <T> Future<T> submit(@NonNull Runnable runnable,
                                @NonNull T result) {
        FutureTask<T> future = new FutureTask<>(
                () -> {
                    runnable.run();
                    return result;
                });
        execute(future);

        return future;
    }

    @NonNull
    @NotCallOrigin
    @Override // ExecutorService
    public Future submit(@NonNull Runnable runnable) {
        if (runnable instanceof RunnableFuture) {
            mHandler.post(runnable);
            return (Future) runnable;
        }

        FutureTask<Object> future = new FutureTask<>(new Callable<Object>() {
            @Override
            @NotCallOrigin
            @Nullable
            public Object call() throws Exception {
                runnable.run();
                return null;
            }
        });

        mHandler.post(future);

        return future;
    }

    @NonNull
    @Override // ExecutorService
    @WorkerThread
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> callables) throws InterruptedException, NullPointerException, RejectedExecutionException {
        final ArrayList<Future<T>> futures = new ArrayList<>(callables.size());

        if (callables.size() > 0) {
            for (Callable<T> callable : callables) {
                futures.add(submit(callable));
            }
            try {
                futures.get(futures.size() - 1).get();
            } catch (ExecutionException e) {
                RCLog.throwRuntimeException(this, "Can not get() last element of invokeAll()", e);
            }
        }

        return futures;
    }

    @NonNull
    @Override // ExecutorService
    @WorkerThread
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> callables,
                                         long timeout,
                                         @NonNull TimeUnit unit) throws InterruptedException, NullPointerException, RejectedExecutionException {
        final ArrayList<Future<T>> futures = new ArrayList<>(callables.size());

        if (callables.size() > 0) {
            for (Callable<T> callable : callables) {
                futures.add(submit(callable));
            }
            try {
                futures.get(futures.size() - 1).get(timeout, unit);
            } catch (ExecutionException e) {
                RCLog.throwRuntimeException(this, "Can not get() last element of invokeAll()", e);
            } catch (TimeoutException e) {
                RCLog.throwRuntimeException(this, "Timeout waiting to get() last element of invokeAll()", e);
            }
        }

        return futures;
    }

    @Override // ExecutorService
    @WorkerThread
    @NonNull
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> callables) throws InterruptedException, NullPointerException, RejectedExecutionException, ExecutionException {
        ArrayList<Future<T>> futures = new ArrayList<>(callables.size());

        if (callables.size() == 0) {
            throw new NullPointerException("Empty list can not invokeAny() as there is no from to return");
        }
        for (final Callable<T> callable : callables) {
            futures.add(submit(callable));
        }

        return futures.get(0).get();
    }

    @NonNull
    @Override // ExecutorService
    @WorkerThread
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> callables,
                           long timeout,
                           @NonNull TimeUnit unit) throws InterruptedException, NullPointerException, RejectedExecutionException, TimeoutException, ExecutionException {
        ArrayList<Future<T>> futures = new ArrayList<>(callables.size());

        if (callables.size() == 0) {
            throw new NullPointerException("Empty list can not invokeAny() as there is no from to return");
        }
        for (Callable<T> callable : callables) {
            futures.add(submit(callable));
        }

        return futures.get(0).get(timeout, unit);
    }

    @Override // ExecutorService
    public void execute(@NonNull Runnable command) {
        if (!mHandler.post(command)) {
            RCLog.throwIllegalStateException(this, "Can not Handler.post() to UIThread in this Context right now, probably app is shutting down");
        }
    }
}
