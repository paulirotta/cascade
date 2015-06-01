package com.futurice.cascade;

import android.content.Context;
import android.test.AndroidTestCase;

import com.futurice.cascade.rest.FileUtil;

/**
 * A connectedTest harness which bootstraps the Async class
 *
 * Created by phou on 6/1/2015.
 */
public class AsyncAndroidTestCase extends AndroidTestCase {
    protected FileUtil fileUtil;

    public AsyncAndroidTestCase() {
        super();
    }

    @Override // TestCase
    protected void setUp() throws Exception {
        super.setUp();

        new AsyncBuilder(getContext()).build();
        fileUtil = new FileUtil(getContext(), Context.MODE_PRIVATE);
    }
}
