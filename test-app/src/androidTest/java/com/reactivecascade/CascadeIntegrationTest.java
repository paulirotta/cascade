package com.reactivecascade;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.util.AssertUtil;
import com.reactivecascade.util.TestUtil;

import org.junit.After;
import org.junit.Before;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class CascadeIntegrationTest {
    protected Context appContext;
    protected long defaultTimeoutMillis = 5000;
    private CountDownLatch signal;

    /**
     * Override this method and initialize the library, for example
     * <pre><code>
     * new AsyncBuilder(appContext)
     *    .setStrictMode(false) // Relax threading restrictions for tests
     *    .build();
     * </code></pre>
     *
     * Be aware that due to the use of static for performance reasons, some parts of
     * the test library can not be overridden differently in different tests. This can
     * be circumvented by creating multiple test applications for each configuration which
     * varies
     *
     * @throws Exception
     */
    @CallSuper
    @Before
    public void setUp() throws Exception {
        appContext = InstrumentationRegistry.getTargetContext();
        if (appContext == null) {
            throw new NullPointerException("Test harness setup failure - App Context can not be null");
        }
        signal = new CountDownLatch(1);
    }

    @CallSuper
    @After
    public void cleanup() throws Exception {
        appContext = null;
        AsyncBuilder.reset();
    }

    /**
     * Indicate to main test thread that async test can proceed
     */
    protected final void signal() {
        signal.countDown();
    }

    /**
     * Wait on main test thread for {@link #signal()} from another thread before the test can proceed
     *
     * @throws InterruptedException
     */
    protected final void awaitSignal() throws InterruptedException {
        signal.await(defaultTimeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * {@link #await(IAltFuture, long)} and hide intentional error stack traces from the logs to
     * avoid confusion.
     * <p>
     * The timeout period can be adjusted by changing {@link #defaultTimeoutMillis}
     *
     * @param altFuture the action to be performed
     * @param <IN>      the type passed into the altFuture
     * @param <OUT>     the type returned from the altFuture
     * @return output from execution of altFuture
     * @throws Exception
     */
    @NonNull
    protected final <IN, OUT> OUT awaitHideStackTraces(@NonNull IAltFuture<IN, OUT> altFuture) throws Exception {
        return awaitHideStackTraces(altFuture, defaultTimeoutMillis);
    }

    /**
     * {@link #await(IAltFuture, long)} and hide intentional error stack traces from the logs to
     * avoid confusion.
     *
     * @param altFuture     the action to be performed
     * @param timeoutMillis maximum time to wait for the action to complete before throwing a {@link java.util.concurrent.TimeoutException}
     * @param <IN>          the input type passed to the altFuture
     * @param <OUT>         the output type returned from the altFuture
     * @return output returned from execution of the altFuture
     * @throws Exception
     */
    @NonNull
    protected final <IN, OUT> OUT awaitHideStackTraces(@NonNull IAltFuture<IN, OUT> altFuture,
                                                 long timeoutMillis) throws Exception {
        AssertUtil.assertTrue("Please call altFuture.fork() before await(altFuture)", altFuture.isForked());
        return TestUtil.awaitHideStackTraces(altFuture, timeoutMillis);
    }

    /**
     * Perform an action, holding the calling thread until execution completes on another thread
     * <p>
     * The timeout period can be adjusted by changing {@link #defaultTimeoutMillis}
     *
     * @param altFuture the action to be performed
     * @param <IN>          the input type passed to the altFuture
     * @param <OUT>         the output type returned from the altFuture
     * @return output returned from execution of the altFuture
     * @throws Exception
     */
    @NonNull
    protected <IN, OUT> OUT await(@NonNull IAltFuture<IN, OUT> altFuture) throws Exception {
        return await(altFuture, defaultTimeoutMillis);
    }

    /**
     * Perform an action, holding the calling thread until execution completes on another thread
     *
     * @param altFuture     the action to be performed
     * @param timeoutMillis maximum time to wait for the action to complete before throwing a {@link java.util.concurrent.TimeoutException}
     * @param <IN>          the input type passed to the altFuture
     * @param <OUT>         the output type returned from the altFuture
     * @return output returned from execution of the altFuture
     * @throws Exception
     */
    @NonNull
    protected <IN, OUT> OUT await(@NonNull IAltFuture<IN, OUT> altFuture,
                                  long timeoutMillis) throws Exception {
        AssertUtil.assertTrue("Please call altFuture.fork() before await(altFuture)", altFuture.isForked());
        return TestUtil.await(altFuture, timeoutMillis);
    }
}
