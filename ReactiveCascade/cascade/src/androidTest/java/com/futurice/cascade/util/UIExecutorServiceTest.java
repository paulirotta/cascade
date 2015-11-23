package com.futurice.cascade.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.test.suitebuilder.annotation.MediumTest;

import com.futurice.cascade.AsyncAndroidTestCase;
import com.futurice.cascade.functional.SettableAltFuture;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.futurice.cascade.Async.UI;
import static com.futurice.cascade.Async.WORKER;
import static org.assertj.core.api.Assertions.assertThat;

@MediumTest
public class UIExecutorServiceTest extends AsyncAndroidTestCase {
    final Object looperFlushMutex = new Object();

    volatile int handleMessageCount;
    volatile int dispatchMessageCount;
    volatile int sendCount;

    volatile UIExecutorService uiExecutorService;
    private Thread fakeUiThread;

    @Before
    @Override
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
    }

    protected void flushLooper() throws InterruptedException {
        synchronized (looperFlushMutex) {
            uiExecutorService.execute(() -> {
                synchronized (looperFlushMutex) {
                    RCLog.v(UIExecutorServiceTest.this, "Looper flushed");
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
        assertThat(sendCount).isEqualTo(2);
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
        assertThat(sendCount).isEqualTo(2);
    }

    @Test
    public void testInvokeAllCallable() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String, String> saf = new SettableAltFuture<>(WORKER);

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
        awaitDone(saf);
        assertThat(sendCount).isGreaterThan(0);
        assertThat(ai.get()).isEqualTo(300);
    }

    @Test
    public void testInvokeAllCallableTimeout() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String, String> saf = new SettableAltFuture<>(WORKER);

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
        awaitDone(saf);
        assertThat(sendCount).isGreaterThan(0);
        assertThat(ai.get()).isEqualTo(300);
    }

    @Test
    public void testInvokeAnyCallable() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String, String> saf = new SettableAltFuture<>(WORKER);

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
        awaitDone(saf);
        assertThat(sendCount).isGreaterThan(0);
        assertThat(ai.get()).isGreaterThan(0);
    }

    @Test
    public void testInvokeAnyCallableTimeout() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String, String> saf = new SettableAltFuture<>(WORKER);

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
        awaitDone(saf);
        assertThat(sendCount).isGreaterThan(0);
        assertThat(ai.get()).isGreaterThan(0);
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
