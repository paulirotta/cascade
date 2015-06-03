package com.futurice.cascade.test;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.test.ApplicationTestCase;

import com.futurice.cascade.AsyncBuilder;
import com.futurice.cascade.i.functional.IAltFuture;
import com.futurice.cascade.util.FileUtil;
import com.futurice.cascade.util.NetUtil;

/**
 * A connectedTest harness which bootstraps the Async class
 * <p>
 * Created by phou on 6/1/2015.
 */
public class AsyncApplicationTestCase<T extends Application> extends ApplicationTestCase<T> {
    private TestUtil testUtil;
    private FileUtil fileUtil;
    private NetUtil netUtil;
    private long defaultTimeoutMillis = 1000;

    public AsyncApplicationTestCase(@NonNull final Class<T> applicationClass) {
        super(applicationClass);
    }

    /**
     * Change the default timeout period when one thread waits for a result on another thread.
     *
     * @param defaultTimeoutMillis interval before the test is abandoned with a {@link java.util.concurrent.TimeoutException}
     */
    public void setDefaultTimeoutMillis(final long defaultTimeoutMillis) {
        this.defaultTimeoutMillis = defaultTimeoutMillis;
    }

    @Override // TestCase
    protected void setUp() throws Exception {
        super.setUp();

        new AsyncBuilder(getContext()).build();
    }

    @NonNull
    public TestUtil getTestUtil() {
        if (testUtil == null) {
            setTestUtil(new TestUtil());
        }
        return testUtil;
    }

    public void setTestUtil(@NonNull final TestUtil testUtil) {
        this.testUtil = testUtil;
    }

    @NonNull
    public FileUtil getFileUtil() {
        if (fileUtil == null) {
            setFileUtil(new FileUtil(getContext(), Context.MODE_PRIVATE));
        }
        return fileUtil;
    }

    public void setFileUtil(@NonNull final FileUtil fileUtil) {
        this.fileUtil = fileUtil;
    }

    @NonNull
    public NetUtil getNetUtil() {
        if (netUtil == null) {
            setNetUtil(new NetUtil(getContext()));
        }
        return netUtil;
    }

    public void setNetUtil(@NonNull final NetUtil netUtil) {
        this.netUtil = netUtil;
    }

    @NonNull
    protected <IN, OUT> OUT hideIntentionalErrorStackTraces(
            @NonNull final IAltFuture<IN, OUT> action)
            throws Exception {
        return getTestUtil().awaitDoneNoErrorStackTraces(action, defaultTimeoutMillis);
    }

    @NonNull
    protected <IN, OUT> OUT awaitDone(
            @NonNull final IAltFuture<IN, OUT> altFuture)
            throws Exception {
        return getTestUtil().awaitDone(altFuture, defaultTimeoutMillis);
    }
}
