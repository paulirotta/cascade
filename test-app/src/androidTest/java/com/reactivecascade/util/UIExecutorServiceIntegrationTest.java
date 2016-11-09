/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.support.test.runner.AndroidJUnit4;

import com.reactivecascade.AsyncBuilder;
import com.reactivecascade.DefaultCascadeIntegrationTest;
import com.reactivecascade.functional.SettableAltFuture;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class UIExecutorServiceIntegrationTest extends DefaultCascadeIntegrationTest {
    final Object looperFlushMutex = new Object();

    private void barrier() throws Exception {
        CountDownLatch sig = new CountDownLatch(1);
        AsyncBuilder.uiExecutorService.execute(sig::countDown);
        sig.await(defaultTimeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testUIIsShutdown() throws Exception {
        assertFalse(AsyncBuilder.getUiThreadType(getContext()).isShutdown());
    }

    @Test
    public void testIsTerminated() throws Exception {
        assertFalse(AsyncBuilder.uiExecutorService.isTerminated());
    }

    @Test
    public void testSubmitCallable() throws Exception {
        AsyncBuilder.uiExecutorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                signal();
                return null;
            }
        });
        awaitSignal();
    }

    @Test
    public void testSubmitRunnable() throws Exception {
        AsyncBuilder.uiExecutorService.submit(new Runnable() {
                                     @Override
                                     public void run() {
                                         signal();
                                     }
                                 }
        );
        awaitSignal();
    }

    @Test
    public void testInvokeAllCallable() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String> saf = new SettableAltFuture<>(AsyncBuilder.worker);

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
        AsyncBuilder.uiExecutorService.invokeAll(callableList);
        await(saf);
        assertEquals(300, ai.get());
    }

    @Test
    public void testInvokeAllCallableTimeout() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String> saf = new SettableAltFuture<>(AsyncBuilder.worker);

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
        AsyncBuilder.uiExecutorService.invokeAll(callableList, defaultTimeoutMillis, TimeUnit.MILLISECONDS);
        await(saf);
        assertEquals(300, ai.get());
    }

    @Test
    public void testInvokeAnyCallable() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();
        SettableAltFuture<String> saf = new SettableAltFuture<>(AsyncBuilder.worker);

        callableList.add(() -> {
            ai.set(100);
            return 100;
        });
        callableList.add(() -> {
            ai.set(200);
            return 200;
        });
        AsyncBuilder.uiExecutorService.invokeAny(callableList);
        assertTrue(ai.get() > 0);
    }

    @Test
    public void testInvokeAnyCallableTimeout() throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        ArrayList<Callable<Integer>> callableList = new ArrayList<>();

        callableList.add(() -> {
            ai.set(100);
            return 100;
        });
        callableList.add(() -> {
            ai.set(200);
            return 200;
        });
        AsyncBuilder.uiExecutorService.invokeAny(callableList, 1, TimeUnit.SECONDS);
        assertTrue(ai.get() > 0);
    }

    @Test
    public void testExecute() throws Exception {
        AsyncBuilder.worker.execute(() -> {
            AsyncBuilder.uiExecutorService.execute(this::signal);
        });
        awaitSignal();
    }
}
