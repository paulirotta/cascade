/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.reactivecascade.Async;
import com.reactivecascade.i.IAltFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.reactivecascade.Async.currentThreadType;

/**
 * A {@link java.util.concurrent.Future} which can be used to safely wait for the results
 * from an {@link IAltFuture}.
 * <p>
 * Normally we don't like to hold one thread waiting for the result of another thread. Doing this
 * on a routine basis causes lots of context switching and can backlog into either many threads
 * or deadlock because a limited number of threads are all in use.
 * <p>
 * This can be useful for example to externally and synchronously test the results of an
 * asynchronous process. There is no risk because the situation is tightly controlled and
 * requires performance be secondary to rigid conformance.
 *
 * @param <IN>  the upchain type
 * @param <OUT> the downchain type
 */
public class AltFutureFuture<IN, OUT> extends Origin implements Future<OUT> {
    private static final long DEFAULT_GET_TIMEOUT = 5000;
    private static final long CHECK_INTERVAL = 50; // This is a fallback in case you for example have an error and fail to altFuture.notifyAll() when finished

    @NonNull
    private final IAltFuture<IN, OUT> altFuture;

    //TODO Replace mutext with a CountDownLatch
    private final Object mutex = new Object();

    /**
     * Create a new {@link Future} which wraps an {@link IAltFuture} to allow use of blocking
     * operations.
     * <p>
     * Note that generally we do not wish to use this except for special circumstances such as synchronizing
     * the system test thread with the items being tested. They may exist other special cases, but most
     * often you can re-factor your code as a pure non-blocking chain instead of using this class.
     *
     * @param altFuture to be wrapped for traditional blocking access
     */
    public AltFutureFuture(@NonNull final IAltFuture<IN, OUT> altFuture) {
        this.altFuture = altFuture;
    }

    @Override // Future
    public boolean cancel(boolean mayInterruptIfRunning) {
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
    @Nullable
    public OUT get() {
        try {
            return get(DEFAULT_GET_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            RCLog.throwRuntimeException(this, "Problem waiting for IAltFuture to complete. Did you remember to .fork()?, new RuntimeException", e);
        }

        return null;
    }

    /**
     * Verify that calls to wait for this {@link Future} such as {@link #get(long, TimeUnit)} will not
     * deadlock due to single-threaded access on the same thread as the item which might block.
     */
    public void assertThreadSafe() {
        if (Async.RUNTIME_ASSERTIONS && altFuture.getThreadType() == currentThreadType() && altFuture.getThreadType().isInOrderExecutor()) {
            throw new UnsupportedOperationException("Do not run your tests from the same single-threaded IThreadType as the threads you are testing: " + altFuture.getThreadType());
        }
    }

    /**
     * Block the current thread until the associated IAltFuture completes or errors out
     *
     * @param timeout max time to wait for the RunnableAltFuture to complete
     * @param unit    timeout units
     * @return null if there was an exception during execution
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    @Override // Future
    @Nullable
    public OUT get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        final long t = System.currentTimeMillis();
        final long endTime = t + unit.toMillis(timeout);

        if (!isDone()) {
            assertThreadSafe();
            altFuture
                    .then(() -> {
                        // Attach this to speed up and notify to continue the Future when the RunnableAltFuture finishes
                        // For speed, we don't normally notify after RunnableAltFuture end
                        synchronized (mutex) {
                            mutex.notifyAll();
                        }
                    })
                    .fork();
        }
        while (!isDone()) {
            if (System.currentTimeMillis() >= endTime) {
                RCLog.throwTimeoutException(this, "Waited " + (System.currentTimeMillis() - t) + "ms for RunnableAltFuture to end: " + altFuture);
            }
            synchronized (mutex) {
                final long t2 = Math.min(CHECK_INTERVAL, endTime - System.currentTimeMillis());
                if (t2 > 0) {
                    mutex.wait(t2);
                }
            }
        }

        return altFuture.unsafeGet();
    }

    @Override
    public String toString() {
        return "AltFutureFuture{" +
                "altFuture=" + altFuture +
                '}';
    }
}
