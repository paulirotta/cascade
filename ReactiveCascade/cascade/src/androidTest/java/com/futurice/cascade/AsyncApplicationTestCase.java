package com.futurice.cascade;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.test.ApplicationTestCase;

import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.functional.IAltFuture;
import com.futurice.cascade.util.FileUtil;
import com.futurice.cascade.util.NetUtil;

/**
 * A connectedTest harness which bootstraps the Async class
 *
 * Created by phou on 6/1/2015.
 */
public class AsyncApplicationTestCase<T extends Application> extends ApplicationTestCase<T> {
    protected FileUtil fileUtil;
    protected NetUtil netUtil;
    private final TestUtil testUtil;

    public AsyncApplicationTestCase(@NonNull final Class<T> applicationClass, @NonNull final IThreadType threadType) {
        super(applicationClass);

        this.testUtil = new TestUtil(threadType);
    }

    @Override // TestCase
    protected void setUp() throws Exception {
        super.setUp();

        new AsyncBuilder(getContext()).build();
        fileUtil = new FileUtil(getContext(), Context.MODE_PRIVATE);
        netUtil = new NetUtil(getContext());
    }

    protected final <IN> void hideIntentionalErrorStackTraces(
            @NonNull final IAction<IN> action) {
        testUtil.hideIntentionalErrorStackTraces(action);
    }

    protected final <IN, OUT> OUT awaitDone(@NonNull final IAltFuture<IN, OUT> altFuture) throws Exception {
        return testUtil.awaitDone(altFuture);
    }
}
