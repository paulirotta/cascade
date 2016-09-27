/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.support.annotation.NonNull;

import com.reactivecascade.Async;
import com.reactivecascade.i.IAltFuture;

import java.util.concurrent.TimeUnit;

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
    private static final TestUtil testUtil = new TestUtil();

    private TestUtil() {
    }

    /**
     * Access the test utilities
     *
     * @return singleton
     */
    public static TestUtil getTestUtil() {
        return TestUtil.testUtil;
    }

    /**
     * Run a unit of work on the specified thread. Block the current thread until it
     * completes.
     * <p>
     * Use of this type of blocking one thread by another outside of unit testing is strongly
     * discouraged. All cascade thread pools are strictly size limited so the result
     * could often be deadlock. Consider using a <code>.then(myFunctionToRunAfter)</code> instead.
     *
     * @param altFuture the action to be performed
     * @param <IN>      the input type passed to the altFuture
     * @param <OUT>     the output type returned from the altFuture
     * @return output returned from execution of the altFuture
     * @throws Exception
     */
    public <IN, OUT> OUT await(@NonNull IAltFuture<IN, OUT> altFuture,
                               long timeoutMillis) throws Exception {
        return new AltFutureFuture<>(altFuture).get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Run a unit of work on the specified thread
     * <p>
     * Stack traces from unit tests are expected and can be suppressed to keep the log file
     * clear and simple.
     * <p>
     * NOTE: While this is waiting, unrelated stack traces on other threads are also suppressed
     *
     * @param altFuture the action to be performed
     * @param <IN>      the input type passed to the altFuture
     * @param <OUT>     the output type returned from the altFuture
     * @return output returned from execution of the altFuture
     * @throws Exception
     */
    public <IN, OUT> OUT awaitHideStackTraces(@NonNull IAltFuture<IN, OUT> altFuture,
                                              long timeoutMillis) throws Exception {
        boolean previousState = Async.SHOW_ERROR_STACK_TRACES;
        Async.SHOW_ERROR_STACK_TRACES = false;
        try {
            return await(altFuture, timeoutMillis);
        } finally {
            Async.SHOW_ERROR_STACK_TRACES = previousState;
        }
    }
}
