package com.reactivecascade;

import android.support.annotation.CallSuper;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class AsyncIntegrationTest extends CascadeIntegrationTest {
    Async async;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        async = new AsyncBuilder(getContext())
                .setStrictMode(false)
                .build();
    }

    @CallSuper
    @After
    public void cleanup() throws Exception {
        AsyncBuilder.reset();
        super.cleanup();
    }

    @Test
    public void testTimer() throws Exception {
        Async.TIMER.schedule(this::signal, 10, TimeUnit.MILLISECONDS);

        awaitSignal();
        assertNotNull(getContext().getAssets().getLocales());
    }
}
