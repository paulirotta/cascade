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
package com.futurice.cascade;

import android.os.*;
import android.support.annotation.NonNull;

import java.util.*;
import java.util.concurrent.*;

import static com.futurice.cascade.Async.*;

/**
 * Treat the system UI thread as an ExecutorService
 * <p>
 * This is done to allow UI codes to be part of UI {@link com.futurice.cascade.i.IThreadType}
 * so that {@link com.futurice.cascade.i.functional.IAltFuture} objects can be easily defined to
 * execute on the UI thread in {@link Async#UI} vs background worker threads.
 * <p>
 * Since the system UI thread runs forever, not all {@link java.util.concurrent.ExecutorService}
 * items, for example related to lifecycle. make sense to implement.
 */
public class UIExecutorService implements ExecutorService {
    private static final String TAG = UIExecutorService.class.getSimpleName();
    private final Handler handler;

    public UIExecutorService(@NonNull Handler handler) {
        this.handler = handler;
    }

    @Override
    public void shutdown() {
        ii(TAG, "shutdown() called on UiAsync default ExecutorService");
        throw new UnsupportedOperationException("Shutdown() called on UiAsync default ExecutorService");
    }

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        ii(TAG, "shutdownNow() called on UiAsync default ExecutorService");
        throw new UnsupportedOperationException("ShutdownNow() called on UiAsync default ExecutorService");
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull final TimeUnit unit) throws InterruptedException {
        ii(TAG, "awaitTermination() called on UiAsync default ExecutorService");
        throw new UnsupportedOperationException("awaitTermination() called on UiAsync default ExecutorService");
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull final Callable<T> callable) {
        FutureTask<T> future = new FutureTask<T>(callable);
        execute(future);

        return future;
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull final Runnable runnable, @NonNull final T result) {
        FutureTask<T> future = new FutureTask<T>(() -> {
            runnable.run();
            return result;
        });
        execute(future);

        return future;
    }

    @NonNull
    @Override
    public Future submit(@NonNull final Runnable runnable) {
        if (runnable instanceof RunnableFuture) {
            handler.post(runnable);
            return (Future) runnable;
        }

        FutureTask<Object> future = new FutureTask<>(() -> {
            runnable.run();
            return null;
        });
        handler.post(future);

        return future;
    }

    @Override
    public <T> List<Future<T>> invokeAll(@NonNull final Collection<? extends Callable<T>> callables) throws InterruptedException {
        final ArrayList<Future<T>> futures = new ArrayList<>(callables.size());

        for (Callable<T> callable : callables) {
            futures.add(submit(callable));
        }

        return futures;
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull final Collection<? extends Callable<T>> callables, long timeout, @NonNull final TimeUnit unit) throws InterruptedException {
        return doInvoke(callables, callables.size(), timeout, unit);
    }

    @NonNull
    private <T> List<Future<T>> doInvoke(@NonNull final Collection<? extends Callable<T>> callables, int latchSize, long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        if (isUiThread()) {
            ii(TAG, "Calling UiExecutorService.invokeAll() with a timeout from the UI thread would result in deadlock");
            throw new UnsupportedOperationException("Calling UiExecutorService.invokeAll() with a timeout from the UI thread would result in deadlock");
        }
        if (callables.size() == 0) {
            return new ArrayList<>();
        }

        final List<Future<T>> futures = invokeAll(callables);
        final CountDownLatch latch = new CountDownLatch(latchSize);
        if (unit != null) {
            latch.await(timeout, unit);
        } else {
            latch.await();
        }

        return futures;
    }

    @Override
    public <T> T invokeAny(
            @NonNull final Collection<? extends Callable<T>> callables)
            throws InterruptedException, ExecutionException {
        final List<Future<T>> list = doInvoke(callables, 1, 0, null);

        return doFindAny(list);
    }

    @NonNull
    private <T> T doFindAny(
            @NonNull final List<Future<T>> futures)
            throws ExecutionException, InterruptedException {
        for (Future<T> future : futures) {
            if (future.isDone()) {
                return future.get();
            }
        }
        throwIllegalStateException(TAG, "Reached end of invokeAny() without finding the result which finished");
        return (T) new Object(); // This line is never reached but the IDE doesn't know that
    }

    @NonNull
    @Override
    public <T> T invokeAny(
            @NonNull final Collection<? extends Callable<T>> callables,
            long timeout,
            @NonNull final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        final List<Future<T>> list = doInvoke(callables, 1, timeout, unit);

        return doFindAny(list);
    }

    @Override
    public void execute(@NonNull final Runnable command) {
        final boolean posted = handler.post(command);
        if (!posted) {
            throwIllegalStateException(TAG, "Can not Handler.post() to UIThread in this Context right now, probably app is shutting down");
        }
    }
}
