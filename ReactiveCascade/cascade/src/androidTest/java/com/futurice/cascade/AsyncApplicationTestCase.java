package com.futurice.cascade;

import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;

import com.futurice.cascade.util.FileUtil;

/**
 * A connectedTest harness which bootstraps the Async class
 *
 * Created by phou on 6/1/2015.
 */
public class AsyncApplicationTestCase<T extends Application> extends ApplicationTestCase<T> {
    protected FileUtil fileUtil;

    public AsyncApplicationTestCase(Class<T> applicationClass) {
        super(applicationClass);
    }

    @Override // TestCase
    protected void setUp() throws Exception {
        super.setUp();

        new AsyncBuilder(getContext()).build();
        fileUtil = new FileUtil(getContext(), Context.MODE_PRIVATE);
    }
}
