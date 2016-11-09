package com.reactivecascade;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class AsyncTest extends DefaultCascadeIntegrationTest {

    @Test
    public void testTimer() throws Exception {
        Async.TIMER.schedule(this::signal, 10, TimeUnit.MILLISECONDS);

        awaitSignal();
        assertNotNull(getContext().getAssets().getLocales());
    }
}
