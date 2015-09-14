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

package com.futurice.cascade.util;

import android.support.annotation.NonNull;

import com.futurice.cascade.Async;
import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.i.nonnull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.futurice.cascade.Async.currentThreadType;

/**
 * A {@link java.util.concurrent.Future} which can be used to safely wait for the results
 * from an {@link IAltFuture}.
 * <p>
 * Normally we don't like to hold one thread waiting for the result of another thread. Doing this
 * on a routine basis causes lots of mContext switching and can backlog into either many threads
 * or deadlock because a limited number of threads are all in use.
 * <p>
 * This can be useful for example to externally and synchronously test the results of an
 * asynchronous process. There is no risk because the situation is tightly controlled and
 * requires performance be secondary to rigid conformance.
 *
 * @param <OUT>
 */
public class AltFutureFuture<IN, OUT> implements Future<OUT> {
    private static final long CHECK_INTERVAL = 50; // This is a fallback in case you for example have an error and fail to altFuture.notifyAll() when finished
    private final IAltFuture<IN, OUT> altFuture;
    private final Object mutex = new Object();

    /**
     * Create a new {@link Future} which wraps an {@link IAltFuture} to allow use of blocking
     * operations.
     *
     * Note that generally we do not wish to use this except for special circumstances such as synchronizing
     * the system test thread with the items being tested. They may exist other special cases, but most
     * often you can re-factor your code as a pure non-blocking chain instead of using this class.
     *
     * @param altFuture
     */
    public AltFutureFuture(@NonNull @nonnull final IAltFuture<IN, OUT> altFuture) {
        this.altFuture = altFuture;
    }

    @Override // Future
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return altFuture.cancel("DoneFuture was cancelled");
    }

    @Override // Future
    public boolean isCancelled() {
        return altFuture.isCancelled();
    }

    @Override // Future
    public boolean isDone() {
        return altFuture.isDone();
    }

    @Override // Future
    @NonNull @nonnull
    public OUT get() throws InterruptedException, ExecutionException {
        final OUT out;

        try {
            out = get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new ExecutionException("Timeout waiting for AltFuture to complete. Did you remember to .fork()?, new RuntimeException", e);
        }

        return out;
    }

    /**
     * Verify that calls to wait for this {@link Future} such as {@link #get(long, TimeUnit)} will not
     * deadlock due to single-threaded access on the same thread as the item which might block.
     */
    public void assertThreadSafe() {
        if (altFuture.getThreadType() == currentThreadType() && altFuture.getThreadType().isInOrderExecutor()) {
            throw new UnsupportedOperationException("Do not run your tests from the same single-threaded IThreadType as the threads you are testing: " + altFuture.getThreadType());
        }
    }

    @Override // Future
    @NonNull @nonnull
    public OUT get(
            final long timeout,
            @NonNull @nonnull final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (!isDone()) {
            assertThreadSafe();
        }

        final long t = System.currentTimeMillis();
        final long endTime = t + unit.toMillis(timeout);

        if (!isDone()) {
            altFuture.then(() -> {
                // Attach this to speed up and notify to continue the Future when the AltFuture finishes
                // For speed, we don't normally notify after AltFuture end
                synchronized (mutex) {
                    mutex.notifyAll();
                }
            })
                    .fork();
        }
        while (!isDone()) {
            if (System.currentTimeMillis() >= endTime) {
                Async.throwTimeoutException(this, "Waited " + (System.currentTimeMillis() - t) + "ms for AltFuture to end: " + altFuture);
            }
            synchronized (mutex) {
                final long t2 = Math.min(CHECK_INTERVAL, endTime - System.currentTimeMillis());
                if (t2 > 0) {
                    mutex.wait(t2);
                }
            }
        }

        return altFuture.get();
    }
}
