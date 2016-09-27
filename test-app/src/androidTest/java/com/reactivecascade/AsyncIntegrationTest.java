package com.reactivecascade;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class AsyncIntegrationTest extends CascadeIntegrationTest {
    Async async;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        async = new AsyncBuilder(appContext)
                .setStrictMode(false)
                .build();
    }

    @Test
    public void testTimer() throws Exception {
        Async.TIMER.schedule(this::signal, 10, TimeUnit.MILLISECONDS);

        awaitSignal();
        assertNotNull(appContext.getAssets().getLocales());
    }

    @Test
    public void testUiThreadNotNull() throws Exception {
        assertNotNull(Async.UI_THREAD);
    }

    @Test
    public void testWorkerNotNull() throws Exception {
        assertNotNull(Async.WORKER);
    }

    @Test
    public void testSerialWorkerNotNull() throws Exception {
        assertNotNull(Async.SERIAL_WORKER);
    }

    @Test
    public void testUiNotNull() throws Exception {
        assertNotNull(Async.UI);
    }

    @Test
    public void testNetReadNotNull() throws Exception {
        assertNotNull(Async.NET_READ);
    }

    @Test
    public void testNetWriteNotNull() throws Exception {
        assertNotNull(Async.NET_WRITE);
    }
}
