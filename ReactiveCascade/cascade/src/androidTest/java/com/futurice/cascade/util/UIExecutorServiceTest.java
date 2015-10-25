package com.futurice.cascade.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.test.suitebuilder.annotation.MediumTest;

import com.futurice.cascade.AsyncAndroidTestCase;
import com.futurice.cascade.active.AltFuture;
import com.futurice.cascade.active.IAltFuture;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.futurice.cascade.Async.SERIAL_WORKER;
import static com.futurice.cascade.Async.UI;
import static com.futurice.cascade.Async.WORKER;
import static com.futurice.cascade.Async.assertEqual;
import static com.futurice.cascade.Async.vv;
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

            for (; ; ) {
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
                    vv(mOrigin, "Looper flushed");
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
    public void testInvokeAllCallableTimeout() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        final IAltFuture<Object, Object> done1 = new AltFuture<>(WORKER, () -> {
        });
        final IAltFuture<Object, Object> done2 = new AltFuture<>(WORKER, () -> {
        });
        final ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        callableList.add(() -> {
            ai.set(100);
            return 100;
        });
        callableList.add(() -> {
            ai.set(200);
            return 200;
        });
        WORKER.execute(() -> {
            uiExecutorService.invokeAll(callableList, 1000, TimeUnit.MILLISECONDS);
        });

        awaitDone(done1);
        awaitDone(done2);
        assertThat(sendCount).isGreaterThan(0);
        assertThat(ai.get()).isGreaterThan(0);
    }

    @Test
    public void testInvokeAllCallable() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        final IAltFuture<?, String> done = SERIAL_WORKER.then(() -> {
        });
        final ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        callableList.add(() -> {
            ai.set(100);
            return 100;
        });
        callableList.add(() -> {
            ai.set(ai.get() + 200);
            return 200;
        });
        SERIAL_WORKER.execute(() -> {
            uiExecutorService.invokeAll(callableList);
        });

        awaitDone(done);
        assertThat(sendCount).isGreaterThan(0);
        assertThat(ai.get()).isEqualTo(300);
    }

    @Test
    public void testInvokeAnyCallable() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        final IAltFuture<Object, Object> done1 = new AltFuture<>(WORKER, () -> {
        });
        final IAltFuture<Object, Object> done2 = new AltFuture<>(WORKER, () -> {
        });
        final ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        callableList.add(() -> {
            ai.set(100);
            return 100;
        });
        callableList.add(() -> {
            ai.set(200);
            return 200;
        });
        WORKER.execute(() -> {
            uiExecutorService.invokeAny(callableList);
        });

        awaitDone(done1);
        awaitDone(done2);
        assertThat(sendCount).isGreaterThan(0);
        assertThat(ai.get()).isGreaterThan(0);
    }

    @Test
    @Ignore //FIXME Stopped working, not clear why
    public void testExecute() throws Exception {
        final AtomicInteger ai = new AtomicInteger(0);
        final AtomicInteger ai2 = new AtomicInteger(0);
        final IAltFuture<Object, Object> done = new AltFuture<>(WORKER, () -> {
            ai2.set(200);
        });
        uiExecutorService.execute(() -> {
            ai.set(100);
        });
        awaitDone(done); // Hold for UI to complete
        assertEqual(100, ai.get());
        assertEqual(200, ai2.get());
    }
}
