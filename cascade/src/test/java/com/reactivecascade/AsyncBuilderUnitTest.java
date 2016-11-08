/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.reactivecascade.functional.ImmutableValue;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.util.DefaultThreadType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import mockit.Mocked;
import mockit.integration.junit4.JMockit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;

@RunWith(JMockit.class)
public class AsyncBuilderUnitTest {
    @Mocked
    Thread thread;

    @Mocked
    Log log;

    @Mocked
    BlockingQueue<Runnable> queue;

    @Mocked
    ExecutorService executorService;

    @Mocked
    IThreadType threadType;

    @Mocked
    DefaultThreadType defaultThreadType;

    @Mocked
    Context context;

    @Mocked
    Looper looper;

    private AsyncBuilder asyncBuilder;

    @Before
    public void setUp() throws Exception {
        asyncBuilder = new AsyncBuilder(context)
                .setStrictMode(false)
                .setUiThread(Thread.currentThread());
    }

    @After
    public void tearDown() throws Exception {
        AsyncBuilder.reset();
        asyncBuilder = null;
    }

    @Test
    public void testIsInitialized() throws Exception {
        assertFalse(AsyncBuilder.initialized);
        asyncBuilder.build();
        assertTrue(AsyncBuilder.initialized);
    }

    @Test
    public void testIsRuntimeAssertionsEnabled() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, AsyncBuilder.runtimeAssertionsEnabled);
    }

    @Test
    public void testSetRuntimeAssertionsEnabledFalse() throws Exception {
        asyncBuilder
                .setRuntimeAssertionsEnabled(false)
                .build();
        assertFalse(AsyncBuilder.runtimeAssertionsEnabled);
    }

    @Test
    public void testSetRuntimeAssertionsEnabledTrue() throws Exception {
        asyncBuilder
                .setRuntimeAssertionsEnabled(true)
                .build();
        assertTrue(AsyncBuilder.runtimeAssertionsEnabled);
    }

    @Test
    public void testIsUseForkedState() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, AsyncBuilder.useForkedState);
    }

    @Test
    public void testSetUseForkedState() throws Exception {
        asyncBuilder
                .setUseForkedState(false)
                .build();
        assertFalse(AsyncBuilder.useForkedState);
    }

    @Test
    public void testIsStrictMode() throws Exception {
        asyncBuilder.build();
        assertFalse(AsyncBuilder.strictMode);
    }

    @Test
    public void testIsFailFast() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, AsyncBuilder.failFast);
    }

    @Test
    public void testSetFailFast() throws Exception {
        asyncBuilder
                .setFailFast(false)
                .build();
        assertFalse(AsyncBuilder.failFast);
    }

    @Test
    public void testIsShowErrorStackTraces() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, AsyncBuilder.showErrorStackTraces);
    }

    @Test
    public void testSetShowErrorStackTracesFalse() throws Exception {
        asyncBuilder
                .setShowErrorStackTraces(false)
                .build();
        assertFalse(AsyncBuilder.showErrorStackTraces);
    }

    @Test
    public void testSetShowErrorStackTracesTrue() throws Exception {
        asyncBuilder
                .setShowErrorStackTraces(true)
                .build();
        assertTrue(AsyncBuilder.showErrorStackTraces);
    }

    @Test
    public void testGetWorkerThreadType() throws Exception {
        asyncBuilder
                .setWorkerThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getWorkerThreadType());
    }

    @Test
    public void testGetSerialWorkerThreadType() throws Exception {
        asyncBuilder
                .setSerialWorkerThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getSerialWorkerThreadType());
    }

    @Test
    public void testSetSerialWorkerThreadType() throws Exception {
        asyncBuilder
                .setSerialWorkerThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getSerialWorkerThreadType());
    }

    @Test
    public void testGetUiThreadType() throws Exception {
        asyncBuilder
                .setUIThreadType(threadType)
                .build();
        assertEquals(threadType, AsyncBuilder.uiThreadType);
    }

    @Test
    public void testSetUIThreadType() throws Exception {
        asyncBuilder
                .setUIThreadType(threadType)
                .build();
        assertEquals(threadType, AsyncBuilder.uiThreadType);
    }

    @Test
    public void testGetNetReadThreadType() throws Exception {
        asyncBuilder
                .setNetReadThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getNetReadThreadType());
    }

    @Test
    public void testSetNetReadThreadType() throws Exception {
        asyncBuilder
                .setNetReadThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getNetReadThreadType());
    }

    @Test
    public void testSetNetWriteThreadType() throws Exception {
        asyncBuilder
                .setNetWriteThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getNetWriteThreadType());
    }

    @Test
    public void testSetFileThreadType() throws Exception {
        asyncBuilder
                .setFileThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getFileThreadType());
    }

    @Test
    public void testGetWorkerExecutorService() throws Exception {
        asyncBuilder
                .setWorkerExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getWorkerExecutorService(new ImmutableValue<>()));
    }

    @Test
    public void testGetSerialWorkerExecutorService() throws Exception {
        asyncBuilder
                .setSerialWorkerExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getSerialWorkerExecutorService(new ImmutableValue<>()));
    }

    @Test
    public void testGetWorkerQueue() throws Exception {
        asyncBuilder
                .setWorkerQueue(queue)
                .build();
        assertEquals(queue, asyncBuilder.getWorkerQueue());
    }

    @Test
    public void testGetSerialWorkerQueue() throws Exception {
        asyncBuilder
                .setSerialWorkerQueue(queue)
                .build();
        assertEquals(queue, asyncBuilder.getSerialWorkerQueue());
    }

    @Test
    public void testGetFileQueue() throws Exception {
        asyncBuilder
                .setFileQueue(queue)
                .build();
        assertEquals(queue, asyncBuilder.getFileQueue());
    }

    @Test
    public void testGetNetReadQueue() throws Exception {
        asyncBuilder
                .setNetReadQueue(queue)
                .build();
        assertEquals(queue, asyncBuilder.getNetReadQueue());

    }

    @Test
    public void testGetNetWriteQueue() throws Exception {
        asyncBuilder
                .setNetWriteQueue(queue)
                .build();
        assertEquals(queue, asyncBuilder.getNetWriteQueue());
    }

    @Test
    public void testGetFileExecutorService() throws Exception {
        asyncBuilder
                .setFileExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getFileExecutorService(new ImmutableValue<>()));
    }

    @Test
    public void testGetNetReadExecutorService() throws Exception {
        asyncBuilder
                .setNetReadExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getNetReadExecutorService(new ImmutableValue<>()));
    }

    @Test
    public void testGetNetWriteExecutorService() throws Exception {
        asyncBuilder
                .setNetWriteExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getNetWriteExecutorService(new ImmutableValue<>()));
    }

    @Test
    public void testGetUiExecutorService() throws Exception {
        asyncBuilder
                .setUiExecutorService(executorService)
                .build();
        assertEquals(executorService, AsyncBuilder.uiExecutorService);
    }

    @Test
    public void testSetUI_Thread() throws Exception {
        AsyncBuilder builder = asyncBuilder
                .setUiThread(thread);
        builder.build();
        assertEquals(thread, AsyncBuilder.uiThread);
    }
}