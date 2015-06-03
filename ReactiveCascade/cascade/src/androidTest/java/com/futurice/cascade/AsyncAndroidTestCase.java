package com.futurice.cascade;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import com.futurice.cascade.i.functional.IAltFuture;
import com.futurice.cascade.test.TestUtil;
import com.futurice.cascade.util.FileUtil;
import com.futurice.cascade.util.NetUtil;

import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * A connectedTest harness which bootstraps the Async class
 * <p>
 * Created by phou on 6/1/2015.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class AsyncAndroidTestCase extends ActivityInstrumentationTestCase2<Activity> {
    private TestUtil testUtil;
    private FileUtil fileUtil;
    private NetUtil netUtil;
    private long defaultTimeoutMillis = 1000;
    protected final Context context;

    public AsyncAndroidTestCase() {
        super(Activity.class);

        context = InstrumentationRegistry.getContext();
    }

    /**
     * If you wish to use a non-default {@link Async configuration, construct that in an overriding method,
     * call this method, and then pass your implementation directly to your tests.
     *
     * @throws Exception
     */
    @Before
    @Override // TestCase
    public void setUp() throws Exception {
        super.setUp();

        new AsyncBuilder(context).build();
    }

    /**
     * Change the default timeout period when one thread waits for a result on another thread.
     *
     * @param defaultTimeoutMillis interval before the test is abandoned with a {@link java.util.concurrent.TimeoutException}
     */
    public void setDefaultTimeoutMillis(final long defaultTimeoutMillis) {
        this.defaultTimeoutMillis = defaultTimeoutMillis;
    }

    /**
     * Access {@link TestUtil} from within an integration test
     *
     * @return the test util implementation
     */
    @NonNull
    public TestUtil getTestUtil() {
        if (testUtil == null) {
            setTestUtil(new TestUtil());
        }
        return testUtil;
    }

    /**
     * Change from the default {@link TestUtil} implementation.
     *
     * It is usually not needed to call this method.
     *
     * @param testUtil the test util implementation
     */
    public void setTestUtil(@NonNull final TestUtil testUtil) {
        this.testUtil = testUtil;
    }

    @NonNull
    public FileUtil getFileUtil() {
        if (fileUtil == null) {
            setFileUtil(new FileUtil(context, Context.MODE_PRIVATE));
        }
        return fileUtil;
    }

    /**
     * Change from the default {@link FileUtil} implementation.
     *
     * It is usually not needed to call this method.
     *
     * @param fileUtil the file util implementation
     */
    public void setFileUtil(@NonNull final FileUtil fileUtil) {
        this.fileUtil = fileUtil;
    }

    @NonNull
    public NetUtil getNetUtil() {
        if (netUtil == null) {
            setNetUtil(new NetUtil(context));
        }
        return netUtil;
    }

    /**
     * Change from the default {@link NetUtil} implementation.
     *
     * It is usually not needed to call this method.
     *
     * @param netUtil the network utilities implementation
     */
    public void setNetUtil(@NonNull final NetUtil netUtil) {
        this.netUtil = netUtil;
    }

    /**
     * {@link #awaitDone(IAltFuture, long)} and hide intentional error stack traces from the logs to
     * avoid confusion.
     *
     * The default timeout of 1 second will be used unless this has been overridden by
     * {@link #setDefaultTimeoutMillis(long)}
     *
     * @param altFuture the action to be performed
     * @param <IN> the type passed into the altFuture
     * @param <OUT> the type returned from the altFuture
     * @return output from execution of altFuture
     * @throws Exception
     */
    @NonNull
    protected <IN, OUT> OUT awaitDoneNoErrorStackTraces(
            @NonNull final IAltFuture<IN, OUT> altFuture)
            throws Exception {
        return awaitDoneNoErrorStackTraces(altFuture, defaultTimeoutMillis);
    }

    /**
     * {@link #awaitDone(IAltFuture, long)} and hide intentional error stack traces from the logs to
     * avoid confusion.
     *
     * @param altFuture the action to be performed
     * @param timeoutMillis maximum time to wait for the action to complete before throwing a {@link java.util.concurrent.TimeoutException}
     * @param <IN> the type passed into the altFuture
     * @param <OUT> the type returned from the altFuture
     * @return output from execution of altFuture
     * @throws Exception
     */
    @NonNull
    protected <IN, OUT> OUT awaitDoneNoErrorStackTraces(
            @NonNull final IAltFuture<IN, OUT> altFuture,
            final long timeoutMillis)
            throws Exception {
        return getTestUtil().awaitDoneNoErrorStackTraces(altFuture, timeoutMillis);
    }

    /**
     * Perform an action, holding the calling thread until execution completes on another thread
     *
     * The default timeout of 1 second will be used unless this has been overridden by
     * {@link #setDefaultTimeoutMillis(long)}
     *
     * @param altFuture the action to be performed
     * @param <IN> the type passed into the altFuture
     * @param <OUT> the type returned from the altFuture
     * @return output from execution of altFuture
     * @throws Exception
     */

    @NonNull
    protected <IN, OUT> OUT awaitDone(
            @NonNull final IAltFuture<IN, OUT> altFuture)
            throws Exception {
        return awaitDone(altFuture, defaultTimeoutMillis);
    }

    /**
     * Perform an action, holding the calling thread until execution completes on another thread
     *
     * @param altFuture the action to be performed
     * @param timeoutMillis maximum time to wait for the action to complete before throwing a {@link java.util.concurrent.TimeoutException}
     * @param <IN> the type passed into the altFuture
     * @param <OUT> the type returned from the altFuture
     * @return output from execution of altFuture
     * @throws Exception
     */
    @NonNull
    protected <IN, OUT> OUT awaitDone(
            @NonNull final IAltFuture<IN, OUT> altFuture,
            final long timeoutMillis)
            throws Exception {
        return getTestUtil().awaitDone(altFuture, timeoutMillis);
    }
}
