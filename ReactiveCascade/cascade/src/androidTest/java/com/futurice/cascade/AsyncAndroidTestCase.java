/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.util.FileUtil;
import com.futurice.cascade.util.NetUtil;
import com.futurice.cascade.util.TestUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.futurice.cascade.Async.originAsync;

/**
 * A connectedTest harness which bootstraps the Async class
 * <p>
 * Created by phou on 6/1/2015.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class AsyncAndroidTestCase extends ActivityInstrumentationTestCase2<Activity> {
    private static Async async;
    protected final Context mContext;
    protected ImmutableValue<String> mOrigin;
    private TestUtil mTestUtil;
    private FileUtil mFileUtil;
    private NetUtil mNetUtil;
    private long mDefaultTimeoutMillis = 1000;

    public AsyncAndroidTestCase() {
        super(Activity.class);

        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mContext = InstrumentationRegistry.getContext();
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

        if (async == null) {
            async = new AsyncBuilder(mContext)
                    .build();
        }

        mOrigin = originAsync();
    }

    @Test
    @SmallTest
    public void dummyTest() {
    }

    public final long getDefaultTimeoutMillis() {
        return this.mDefaultTimeoutMillis;
    }

    /**
     * Change the default timeout period when one thread waits for a result on another thread.
     *
     * @param defaultTimeoutMillis interval before the test is abandoned with a {@link java.util.concurrent.TimeoutException}
     */
    public final void setDefaultTimeoutMillis(final long defaultTimeoutMillis) {
        this.mDefaultTimeoutMillis = defaultTimeoutMillis;
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
    public final void setTestUtil(@NonNull final TestUtil testUtil) {
        this.mTestUtil = testUtil;
    }

    @NonNull
    public final FileUtil getFileUtil() {
        if (mFileUtil == null) {
            setFileUtil(new FileUtil(mContext, Context.MODE_PRIVATE));
        }
        return mFileUtil;
    }

    /**
     * Change from the default {@link FileUtil} implementation.
     * <p>
     * It is usually not needed to call this method.
     *
     * @param fileUtil the file util implementation
     */
    public final void setFileUtil(@NonNull final FileUtil fileUtil) {
        this.mFileUtil = fileUtil;
    }

    @NonNull
    public final NetUtil getNetUtil() {
        if (mNetUtil == null) {
            setNetUtil(new NetUtil(mContext));
        }
        return mNetUtil;
    }

    /**
     * Change from the default {@link NetUtil} implementation.
     * <p>
     * It is usually not needed to call this method.
     *
     * @param netUtil the network utilities implementation
     */
    public final void setNetUtil(@NonNull final NetUtil netUtil) {
        this.mNetUtil = netUtil;
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
    protected final <IN, OUT> OUT awaitDoneNoErrorStackTraces(
            @NonNull final IAltFuture<IN, OUT> altFuture)
            throws Exception {
        return awaitDoneNoErrorStackTraces(altFuture, mDefaultTimeoutMillis);
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
    protected final <IN, OUT> OUT awaitDoneNoErrorStackTraces(
            @NonNull final IAltFuture<IN, OUT> altFuture,
            final long timeoutMillis)
            throws Exception {
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
    protected final <IN, OUT> OUT awaitDone(
            @NonNull final IAltFuture<IN, OUT> altFuture)
            throws Exception {
        return awaitDone(altFuture, mDefaultTimeoutMillis);
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
    protected final <IN, OUT> OUT awaitDone(
            @NonNull final IAltFuture<IN, OUT> altFuture,
            final long timeoutMillis)
            throws Exception {
        return getTestUtil().awaitDone(altFuture, timeoutMillis);
    }
}
