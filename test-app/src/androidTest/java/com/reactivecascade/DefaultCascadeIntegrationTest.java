package com.reactivecascade;

import android.support.annotation.CallSuper;
import android.support.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public abstract class DefaultCascadeIntegrationTest extends CascadeIntegrationTest {
    protected static AsyncBuilder asyncBuilder;
    protected static Async async;

    @Before
    @CallSuper
    public void setUp() throws Exception {
        super.setUp();

        if (DefaultCascadeIntegrationTest.asyncBuilder == null) {
            DefaultCascadeIntegrationTest.asyncBuilder = new AsyncBuilder(getContext()).setStrictMode(false);
            DefaultCascadeIntegrationTest.async = asyncBuilder.build();
        }
    }


    @CallSuper
    @AfterClass
    public static void cleanup() throws Exception {
        AsyncBuilder.reset();
        DefaultCascadeIntegrationTest.asyncBuilder = null;
        DefaultCascadeIntegrationTest.async = null;
    }
}
