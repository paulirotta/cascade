/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import com.reactivecascade.AsyncBuilder;
import com.reactivecascade.CascadeIntegrationTest;
import com.reactivecascade.functional.SettableAltFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.reactivecascade.Async.UI;
import static com.reactivecascade.Async.WORKER;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class UIExecutorServiceIntegrationTest extends CascadeIntegrationTest {
    final Object looperFlushMutex = new Object();

    volatile int handleMessageCount;
    volatile int dispatchMessageCount;
    volatile int sendCount;

    volatile UIExecutorService uiExecutorService;
    private Thread fakeUiThread;

    @Before
    @Override
    @SuppressWarnings("HandlerLeak")
    public void setUp() throws Exception {
        if (fakeUiThread == null) {
            fakeUiThread = new HandlerThread("FakeUiHandler", Thread.NORM_PRIORITY) {
                protected void onLooperPrepared() {
                    uiExecutorService = new UIExecutorService(new Handler() {
                        public void handleMessage(@NonNull Message msg) {
                            super.handleMessage(msg);
                            handleMessageCount++;
                        }

                        /**
                         * Handle system messages here.
                         */
                        public void dispatchMessage(@NonNull Message msg) {
                            super.dispatchMessage(msg);
                            dispatchMessageCount++;
                        }

                        public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) {
                            sendCount++;
                            return super.sendMessageAtTime(msg, uptimeMillis);
                        }
                    });
                }
            };
            fakeUiThread.start();

            while (true) {
                if (uiExecutorService != null) {
                    break;
                }
                Thread.yield();
            }
        }

        super.setUp();
        new AsyncBuilder(appContext)
                .setStrictMode(false)
                .build();
    }

    private void flushLooper() throws InterruptedException {
        synchronized (looperFlushMutex) {
            uiExecutorService.execute(() -> {
                synchronized (looperFlushMutex) {
                    RCLog.v(UIExecutorServiceIntegrationTest.this, "Looper flushed");
                    looperFlushMutex.notifyAll();
                }
            });
            looperFlushMutex.wait();
        }
    }

    @Test
    public void testUIIsShutdown() throws Exception {
        assertFalse(UI.isShutdown());
    }

    @Test
    public void testIsTerminated() throws Exception {
        assertFalse(uiExecutorService.isTerminated());
    }

    @Test
    public void testSubmitCallable() throws Exception {
        uiExecutorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        });
        flushLooper();
        assertEquals(2, sendCount);
    }

    @Test
    public void testSubmitRunnable() throws Exception {
        uiExecutorService.submit(new Runnable() {
                                     @Override
                                     public void run() {

                                     }
                                 }
        );
        flushLooper();
        assertEquals(2, sendCount);
    }

    @Test
    public void testInvokeAllCallable() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String> saf = new SettableAltFuture<>(WORKER);

        callableList.add(() -> {
            ai.set(100);
            return 100;
        });
        callableList.add(() -> {
            ai.set(ai.get() + 200);
            return 200;
        });
        callableList.add(() -> {
            saf.set("done");
            return 1;
        });
        uiExecutorService.invokeAll(callableList);
        await(saf);
        assertTrue(sendCount > 0);
        assertEquals(300, ai.get());
    }

    @Test
    public void testInvokeAllCallableTimeout() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String> saf = new SettableAltFuture<>(WORKER);

        callableList.add(() -> {
            ai.set(100);
            return 100;
        });
        callableList.add(() -> {
            ai.set(ai.get() + 200);
            return 200;
        });
        callableList.add(() -> {
            saf.set("done");
            return 1;
        });
        uiExecutorService.invokeAll(callableList, 1000, TimeUnit.MILLISECONDS);
        await(saf);
        assertTrue(sendCount > 0);
        assertEquals(300, ai.get());
    }

    @Test
    public void testInvokeAnyCallable() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String> saf = new SettableAltFuture<>(WORKER);

        callableList.add(() -> {
            ai.set(100);
            return 100;
        });
        callableList.add(() -> {
            ai.set(ai.get() + 200);
            return 200;
        });
        callableList.add(() -> {
            saf.set("done");
            return 1;
        });
        uiExecutorService.invokeAny(callableList);
        await(saf);
        assertTrue(sendCount > 0);
        assertTrue(ai.get() > 0);
    }

    @Test
    public void testInvokeAnyCallableTimeout() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String> saf = new SettableAltFuture<>(WORKER);

        callableList.add(() -> {
            ai.set(100);
            return 100;
        });
        callableList.add(() -> {
            ai.set(ai.get() + 200);
            return 200;
        });
        callableList.add(() -> {
            saf.set("done");
            return 1;
        });
        uiExecutorService.invokeAny(callableList, 1000, TimeUnit.MILLISECONDS);
        await(saf);
        assertTrue(sendCount > 0);
        assertTrue(ai.get() > 0);
    }

    @Test
    public void testExecute() throws Exception {
        final AtomicInteger ai = new AtomicInteger(0);
        WORKER.execute(() -> {
            uiExecutorService.execute(() -> {
                ai.set(100);
            });
        });
        long endTime = System.currentTimeMillis() + 1000;
        while (!(ai.get() == 100)) {
            if (System.currentTimeMillis() > endTime) {
                throw new TimeoutException();
            }
            Thread.yield();
        }
    }
}
