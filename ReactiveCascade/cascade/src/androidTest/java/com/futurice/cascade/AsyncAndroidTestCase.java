/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.util.FileUtil;
import com.futurice.cascade.util.NetUtil;
import com.futurice.cascade.util.TestUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A connectedTest harness which bootstraps the Async class
 * <p>
 * Created by phou on 6/1/2015.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class AsyncAndroidTestCase extends ActivityInstrumentationTestCase2<Activity> {
    private TestUtil mTestUtil;
    private FileUtil fileUtil;
    private NetUtil netUtil;

    private long defaultTimeoutMillis = 5000;

    public AsyncAndroidTestCase() {
        super(Activity.class);

        injectInstrumentation(getInstrumentation());
    }

    public Context getContext() {
        return InstrumentationRegistry.getContext();
    }

    /**
     * If you wish to use a non-default {@link Async configuration, construct that in an overriding method,
     * call this method, and then pass your implementation directly to your tests.
     *
     * @throws Exception
     */
    @Before
    @CallSuper
    @Override
    public void setUp() throws Exception {
        super.setUp();

        if (!AsyncBuilder.isInitialized()) {
            new AsyncBuilder(getContext())
                    .setStrictMode(false)
                    .setShowErrorStackTraces(false)
                    .build();
        }
    }

    @Test
    public void dummyTest() {
        assertTrue(true);
    }

    public final long getDefaultTimeoutMillis() {
        return this.defaultTimeoutMillis;
    }

    /**
     * Change the default timeout period when one thread waits for a result on another thread.
     *
     * @param defaultTimeoutMillis interval before the test is abandoned with a {@link java.util.concurrent.TimeoutException}
     */
    public final void setDefaultTimeoutMillis(long defaultTimeoutMillis) {
        this.defaultTimeoutMillis = defaultTimeoutMillis;
    }

    /**
     * Access {@link TestUtil} from within an integration test
     *
     * @return the test util implementation
     */
    @NonNull
    public final TestUtil getTestUtil() {
        if (mTestUtil == null) {
            setTestUtil(new TestUtil());
        }
        return mTestUtil;
    }

    /**
     * Change from the default {@link TestUtil} implementation.
     * <p>
     * It is usually not needed to call this method.
     *
     * @param testUtil the test util implementation
     */
    public final void setTestUtil(@NonNull TestUtil testUtil) {
        this.mTestUtil = testUtil;
    }

    @NonNull
    public final FileUtil getFileUtil() {
        if (fileUtil == null) {
            setFileUtil(new FileUtil(getContext(), Context.MODE_PRIVATE));
        }
        return fileUtil;
    }

    /**
     * Change from the default {@link FileUtil} implementation.
     * <p>
     * It is usually not needed to call this method.
     *
     * @param fileUtil the file util implementation
     */
    public final void setFileUtil(@NonNull FileUtil fileUtil) {
        this.fileUtil = fileUtil;
    }

    @NonNull
    public final NetUtil getNetUtil() {
        if (netUtil == null) {
            setNetUtil(new NetUtil(getContext()));
        }
        return netUtil;
    }

    /**
     * Change from the default {@link NetUtil} implementation.
     * <p>
     * It is usually not needed to call this method.
     *
     * @param netUtil the network utilities implementation
     */
    public final void setNetUtil(@NonNull NetUtil netUtil) {
        this.netUtil = netUtil;
    }

    /**
     * {@link #awaitDone(IAltFuture, long)} and hide intentional error stack traces from the logs to
     * avoid confusion.
     * <p>
     * The default timeout of 1 second will be used unless this has been overridden by
     * {@link #setDefaultTimeoutMillis(long)}
     *
     * @param altFuture the action to be performed
     * @param <IN>      the type passed into the altFuture
     * @param <OUT>     the type returned from the altFuture
     * @return output from execution of altFuture
     * @throws Exception
     */
    @NonNull
    protected final <IN, OUT> OUT awaitDoneNoErrorStackTraces(@NonNull IAltFuture<IN, OUT> altFuture) throws Exception {
        return awaitDoneNoErrorStackTraces(altFuture, defaultTimeoutMillis);
    }

    /**
     * {@link #awaitDone(IAltFuture, long)} and hide intentional error stack traces from the logs to
     * avoid confusion.
     *
     * @param altFuture     the action to be performed
     * @param timeoutMillis maximum time to wait for the action to complete before throwing a {@link java.util.concurrent.TimeoutException}
     * @param <IN>          the type passed into the altFuture
     * @param <OUT>         the type returned from the altFuture
     * @return output from execution of altFuture
     * @throws Exception
     */
    @NonNull
    protected final <IN, OUT> OUT awaitDoneNoErrorStackTraces(@NonNull IAltFuture<IN, OUT> altFuture,
                                                              long timeoutMillis) throws Exception {
        return getTestUtil().awaitDoneNoErrorStackTraces(altFuture, timeoutMillis);
    }

    /**
     * Perform an action, holding the calling thread until execution completes on another thread
     * <p>
     * The default timeout of 1 second will be used unless this has been overridden by
     * {@link #setDefaultTimeoutMillis(long)}
     *
     * @param altFuture the action to be performed
     * @param <IN>      the type passed into the altFuture
     * @param <OUT>     the type returned from the altFuture
     * @return output from execution of altFuture
     * @throws Exception
     */

    @NonNull
    protected final <IN, OUT> OUT awaitDone(@NonNull IAltFuture<IN, OUT> altFuture) throws Exception {
        return awaitDone(altFuture, defaultTimeoutMillis);
    }

    /**
     * Perform an action, holding the calling thread until execution completes on another thread
     *
     * @param altFuture     the action to be performed
     * @param timeoutMillis maximum time to wait for the action to complete before throwing a {@link java.util.concurrent.TimeoutException}
     * @param <IN>          the type passed into the altFuture
     * @param <OUT>         the type returned from the altFuture
     * @return output from execution of altFuture
     * @throws Exception
     */
    @NonNull
    protected final <IN, OUT> OUT awaitDone(@NonNull IAltFuture<IN, OUT> altFuture,
                                            long timeoutMillis) throws Exception {
        return getTestUtil().awaitDone(altFuture, timeoutMillis);
    }
}
