package com.futurice.cascade.test;

import android.support.annotation.NonNull;

import com.futurice.cascade.util.AltFutureFuture;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.functional.IAltFuture;

import java.util.concurrent.TimeUnit;

import static com.futurice.cascade.Async.SHOW_ERROR_STACK_TRACES;
import static com.futurice.cascade.Async.currentThreadType;

/**
 * Intergration test utilities.
 * <p>
 * Although there is technically nothing preventing these from running during normal program operation
 * it is not advised. These methods may suspend one thread while waiting for a result run on another
 * thread. This is classic {@link java.util.concurrent.Future} behavior, but use of {@link IAltFuture}
 * is much safer and more performant in a production environment especially since the number of threads
 * is strictly controlled and tuned to the device in which the application is running.
 * <p>
 * Created by phou on 6/2/2015.
 */
public class TestUtil {
    public TestUtil() {
    }

    public void assertThreadSafe(@NonNull final IThreadType threadType) {
        if (threadType == currentThreadType() && threadType.isInOrderExecutor()) {
            throw new UnsupportedOperationException("Do not run your tests from the same single-threaded IThreadType as the threads you are testing: " + threadType);
        }
    }

    /**
     * Run a unit of work on the specified thread
     *
     * @param altFuture
     * @param <IN>
     * @param <OUT>
     * @return
     * @throws Exception
     */
    public <IN, OUT> OUT awaitDone(
            @NonNull final IAltFuture<IN, OUT> altFuture,
            final long timeoutMillis)
            throws Exception {
        assertThreadSafe(altFuture.getThreadType());
        return new AltFutureFuture<>(altFuture)
                .get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Run a unit of work on the specified thread
     * <p>
     * Error logging will be termporarily disabled during this test to avoid intentional and potentially
     * confusing messages appearing. This has termporary, global side effects and is not compatible
     *
     * @param altFuture
     * @param <IN>
     * @param <OUT>
     * @return
     * @throws Exception
     */
    public <IN, OUT> OUT awaitDoneNoErrorStackTraces(
            @NonNull final IAltFuture<IN, OUT> altFuture,
            final long timeoutMillis)
            throws Exception {
        SHOW_ERROR_STACK_TRACES = false;
        try {
            return awaitDone(altFuture, timeoutMillis);
        } finally {
            SHOW_ERROR_STACK_TRACES = true;
        }
    }
}
