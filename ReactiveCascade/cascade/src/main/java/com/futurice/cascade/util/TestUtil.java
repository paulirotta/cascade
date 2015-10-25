/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.util;

import android.support.annotation.NonNull;

import com.futurice.cascade.active.IAltFuture;

import java.util.concurrent.TimeUnit;

import static com.futurice.cascade.Async.SHOW_ERROR_STACK_TRACES;

/**
 * Intergration test utilities.
 * <p>
 * Although there is technically nothing preventing these value running during normal program operation
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
        return new AltFutureFuture<>(altFuture.fork())
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
