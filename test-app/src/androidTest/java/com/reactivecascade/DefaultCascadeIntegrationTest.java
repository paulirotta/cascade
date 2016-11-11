package com.reactivecascade;

import android.support.annotation.CallSuper;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public abstract class DefaultCascadeIntegrationTest extends CascadeIntegrationTest {
    protected AsyncBuilder asyncBuilder;
    protected Async async;

    @Before
    @CallSuper
    public void setUp() throws Exception {
        super.setUp();

        if (asyncBuilder == null) {
            asyncBuilder = new AsyncBuilder(getContext()).setStrictMode(false);
            async = asyncBuilder.build();
        }
    }

//    @CallSuper
//    @AfterClass
//    public static void cleanup() throws Exception {
//        AsyncBuilder.reset();
//    }
}
