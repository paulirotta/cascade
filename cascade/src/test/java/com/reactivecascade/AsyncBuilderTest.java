/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade;

import android.content.Context;

import com.reactivecascade.functional.ImmutableValue;
import com.reactivecascade.i.IThreadType;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;

public class AsyncBuilderTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    Thread thread;

    @Mock
    BlockingQueue<Runnable> queue;

    @Mock
    ExecutorService executorService;

    @Mock
    IThreadType threadType;

    @InjectMocks
    Context context = mock(Context.class);

    private AsyncBuilder asyncBuilder;

    @Before
    public void setUp() throws Exception {
        asyncBuilder = new AsyncBuilder(context)
                .setStrictMode(false)
                .setUiThread(Thread.currentThread());
    }

    @After
    public void tearDown() throws Exception {
        AsyncBuilder.resetInstance();
        asyncBuilder = null;
    }

    @Test
    @Ignore
    public void testIsInitialized() throws Exception {
        assertFalse(AsyncBuilder.isInitialized());
        asyncBuilder.build();
        assertTrue(AsyncBuilder.isInitialized());
    }

    @Test
    @Ignore
    public void testIsRuntimeAssertionsEnabled() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, asyncBuilder.isRuntimeAssertionsEnabled());
    }

    @Test
    @Ignore
    public void testSetRuntimeAssertionsEnabled() throws Exception {
        asyncBuilder
                .setRuntimeAssertionsEnabled(false)
                .build();
        assertFalse(asyncBuilder.isRuntimeAssertionsEnabled());
    }

    @Test
    @Ignore
    public void testIsUseForkedState() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, asyncBuilder.isUseForkedState());
    }

    @Test
    @Ignore
    public void testSetUseForkedState() throws Exception {
        asyncBuilder
                .setUseForkedState(false)
                .build();
        assertFalse(asyncBuilder.isUseForkedState());
    }

    @Test
    @Ignore
    public void testIsStrictMode() throws Exception {
        asyncBuilder.build();
        assertFalse(asyncBuilder.isStrictMode());
    }

    @Test
    @Ignore
    public void testIsFailFast() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, asyncBuilder.isFailFast());
    }

    @Test
    @Ignore
    public void testSetFailFast() throws Exception {
        asyncBuilder
                .setFailFast(false)
                .build();
        assertFalse(asyncBuilder.isFailFast());
    }

    @Test
    @Ignore
    public void testIsShowErrorStackTraces() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, asyncBuilder.isShowErrorStackTraces());
    }

    @Test
    @Ignore
    public void testSetShowErrorStackTraces() throws Exception {
        asyncBuilder
                .setShowErrorStackTraces(false)
                .build();
        assertFalse(asyncBuilder.isShowErrorStackTraces());
    }

    @Test
    @Ignore
    public void testGetWorkerThreadType() throws Exception {
        asyncBuilder
                .setWorkerThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getWorkerThreadType());
    }

    @Test
    @Ignore
    public void testGetSerialWorkerThreadType() throws Exception {
        asyncBuilder
                .setSerialWorkerThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getSerialWorkerThreadType());
    }

    @Test
    @Ignore
    public void testSetSerialWorkerThreadType() throws Exception {
        asyncBuilder
                .setSerialWorkerThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getSerialWorkerThreadType());
    }

    @Test
    @Ignore
    public void testGetUiThreadType() throws Exception {
        asyncBuilder
                .setUIThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getUiThreadType());
    }

    @Test
    @Ignore
    public void testSetUIThreadType() throws Exception {
        asyncBuilder
                .setUIThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getUiThreadType());
    }

    @Test
    @Ignore
    public void testGetNetReadThreadType() throws Exception {
        asyncBuilder
                .setNetReadThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getNetReadThreadType());
    }

    @Test
    @Ignore
    public void testSetNetReadThreadType() throws Exception {
        asyncBuilder
                .setNetReadThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getNetReadThreadType());
    }

    @Test
    @Ignore
    public void testSetNetWriteThreadType() throws Exception {
        asyncBuilder
                .setNetWriteThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getNetWriteThreadType());
    }

    @Test
    @Ignore
    public void testSetFileThreadType() throws Exception {
        asyncBuilder
                .setFileThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getFileThreadType());
    }

    @Test
    @Ignore
    public void testGetWorkerExecutorService() throws Exception {
        asyncBuilder
                .setWorkerExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getWorkerExecutorService(new ImmutableValue<>()));
    }

    @Test
    @Ignore
    public void testGetSerialWorkerExecutorService() throws Exception {
        asyncBuilder
                .setSerialWorkerExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getSerialWorkerExecutorService(new ImmutableValue<>()));
    }

    @Test
    @Ignore
    public void testGetWorkerQueue() throws Exception {
        asyncBuilder
                .setWorkerQueue(queue)
                .build();
        assertEquals(queue, asyncBuilder.getWorkerQueue());
    }

    @Test
    @Ignore
    public void testGetSerialWorkerQueue() throws Exception {
        asyncBuilder
                .setSerialWorkerQueue(queue)
                .build();
        assertEquals(queue, asyncBuilder.getSerialWorkerQueue());
    }

    @Test
    @Ignore
    public void testGetFileQueue() throws Exception {
        asyncBuilder
                .setFileQueue(queue)
                .build();
        assertEquals(queue, asyncBuilder.getFileQueue());

    }

    @Test
    @Ignore
    public void testGetNetReadQueue() throws Exception {
        asyncBuilder
                .setNetReadQueue(queue)
                .build();
        assertEquals(queue, asyncBuilder.getNetReadQueue());

    }

    @Test
    @Ignore
    public void testGetNetWriteQueue() throws Exception {
        asyncBuilder
                .setNetWriteQueue(queue)
                .build();
        assertEquals(queue, asyncBuilder.getNetWriteQueue());
    }

    @Test
    @Ignore
    public void testGetFileExecutorService() throws Exception {
        asyncBuilder
                .setFileExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getFileExecutorService(new ImmutableValue<>()));
    }

    @Test
    @Ignore
    public void testGetNetReadExecutorService() throws Exception {
        asyncBuilder
                .setNetReadExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getNetReadExecutorService(new ImmutableValue<>()));
    }

    @Test
    @Ignore
    public void testGetNetWriteExecutorService() throws Exception {
        asyncBuilder
                .setNetWriteExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getNetWriteExecutorService(new ImmutableValue<>()));
    }

    @Test
    @Ignore
    public void testGetUiExecutorService() throws Exception {
        asyncBuilder
                .setUiExecutorService(executorService)
                .build();
        assertEquals(executorService, asyncBuilder.getUiExecutorService());
    }

    @Test
    @Ignore
    public void testSetUI_Thread() throws Exception {
        asyncBuilder
                .setUiThread(thread)
                .build();
    }
}