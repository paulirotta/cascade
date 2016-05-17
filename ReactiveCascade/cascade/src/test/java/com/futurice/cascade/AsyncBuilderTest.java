package com.futurice.cascade;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.i.IRunnableAltFuture;
import com.futurice.cascade.i.ISettableAltFuture;
import com.futurice.cascade.i.IThreadType;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Future;

import mockit.Mocked;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class AsyncBuilderTest {
    @Mocked
    Context context;

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
    }

    @Test
    public void testIsInitialized() throws Exception {
        assertFalse(AsyncBuilder.isInitialized());
        asyncBuilder.build();
        assertTrue(AsyncBuilder.isInitialized());
    }

    @Test
    public void testIsRuntimeAssertionsEnabled() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, asyncBuilder.isRuntimeAssertionsEnabled());
    }

    @Test
    public void testSetRuntimeAssertionsEnabled() throws Exception {
        asyncBuilder
                .setRuntimeAssertionsEnabled(false)
                .build();
        assertFalse(asyncBuilder.isRuntimeAssertionsEnabled());
    }

    @Test
    public void testIsUseForkedState() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, asyncBuilder.isUseForkedState());
    }

    @Test
    public void testSetUseForkedState() throws Exception {
        asyncBuilder
                .setUseForkedState(false)
                .build();
        assertFalse(asyncBuilder.isUseForkedState());
    }

    @Test
    public void testIsStrictMode() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, asyncBuilder.isStrictMode());
    }

    @Test
    public void testSetStrictMode() throws Exception {
        asyncBuilder
                .setStrictMode(false)
                .build();
        assertFalse(asyncBuilder.isStrictMode());
    }

    @Test
    public void testIsFailFast() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, asyncBuilder.isFailFast());
    }

    @Test
    public void testSetFailFast() throws Exception {
        asyncBuilder
                .setFailFast(false)
                .build();
        assertFalse(asyncBuilder.isFailFast());
    }

    @Test
    public void testIsShowErrorStackTraces() throws Exception {
        asyncBuilder.build();
        assertEquals(BuildConfig.DEBUG, asyncBuilder.isShowErrorStackTraces());
    }

    @Test
    public void testSetShowErrorStackTraces() throws Exception {
        asyncBuilder
                .setShowErrorStackTraces(false)
                .build();
        assertFalse(asyncBuilder.isShowErrorStackTraces());
    }

    @Test
    public void testGetWorkerThreadType() throws Exception {
        asyncBuilder
                .build();
        assertFalse(asyncBuilder.getWorkerThreadType().isInOrderExecutor());
    }

    @Test
    public void testSetWorkerThreadType() throws Exception {
        asyncBuilder
                .setWorkerThreadType(asyncBuilder.getSerialWorkerThreadType())
                .build();
        assertTrue(asyncBuilder.getWorkerThreadType().isInOrderExecutor());
    }

    @Test
    public void testGetSerialWorkerThreadType() throws Exception {
        asyncBuilder
                .build();
        assertTrue(asyncBuilder.getSerialWorkerThreadType().isInOrderExecutor());

    }

    @Test
    public void testSetSerialWorkerThreadType() throws Exception {
        IThreadType threadType = new IThreadType() {
            @Override
            public boolean isInOrderExecutor() {
                return false;
            }

            @Override
            public <IN> void execute(@NonNull IAction<IN> action) {
            }

            @Override
            public void run(@NonNull Runnable runnable) {
            }

            @Override
            public <OUT> void run(@NonNull IAction<OUT> action, @NonNull IActionOne<Exception> onErrorAction) {

            }

            @Override
            public <OUT> void runNext(@NonNull IAction<OUT> action) {

            }

            @Override
            public void runNext(@NonNull Runnable runnable) {

            }

            @Override
            public boolean moveToHeadOfQueue(@NonNull Runnable runnable) {
                return false;
            }

            @Override
            public <OUT> void runNext(@NonNull IAction<OUT> action, @NonNull IActionOne<Exception> onErrorAction) {

            }

            @NonNull
            @Override
            public <IN> Runnable wrapActionWithErrorProtection(@NonNull IAction<IN> action) {
                return null;
            }

            @NonNull
            @Override
            public <IN> Runnable wrapActionWithErrorProtection(@NonNull IAction<IN> action, @NonNull IActionOne<Exception> onErrorAction) {
                return null;
            }

            @NonNull
            @Override
            public <IN> IAltFuture<IN, IN> then(@NonNull IAction<IN> action) {
                return null;
            }

            @NonNull
            @Override
            public <IN> IAltFuture<IN, IN> then(@NonNull IActionOne<IN> action) {
                return null;
            }

            @NonNull
            @Override
            public <IN> List<IAltFuture<IN, IN>> then(@NonNull IAction<IN>... actions) {
                return null;
            }

            @NonNull
            @Override
            public <OUT> ISettableAltFuture<OUT> from(@NonNull OUT value) {
                return null;
            }

            @NonNull
            @Override
            public <OUT> ISettableAltFuture<OUT> from() {
                return null;
            }

            @NonNull
            @Override
            public <IN, OUT> IAltFuture<IN, OUT> then(@NonNull IActionR<OUT> action) {
                return null;
            }

            @NonNull
            @Override
            public <IN, OUT> List<IAltFuture<IN, OUT>> then(@NonNull IActionR<OUT>... actions) {
                return null;
            }

            @NonNull
            @Override
            public <IN, OUT> IAltFuture<IN, OUT> map(@NonNull IActionOneR<IN, OUT> action) {
                return null;
            }

            @NonNull
            @Override
            public <IN, OUT> List<IAltFuture<IN, OUT>> map(@NonNull IActionOneR<IN, OUT>... actions) {
                return null;
            }

            @Override
            public <IN, OUT> void fork(@NonNull IRunnableAltFuture<IN, OUT> runnableAltFuture) {

            }

            @NonNull
            @Override
            public <IN> Future<Boolean> shutdown(long timeoutMillis, @Nullable IAction<IN> afterShutdownAction) {
                return null;
            }

            @Override
            public boolean isShutdown() {
                return false;
            }

            @NonNull
            @Override
            public <IN> List<Runnable> shutdownNow(@NonNull String reason, @Nullable IAction<IN> actionOnDedicatedThreadAfterAlreadyStartedTasksComplete, @Nullable IAction<IN> actionOnDedicatedThreadIfTimeout, long timeoutMillis) {
                return null;
            }

            @NonNull
            @Override
            public String getName() {
                return null;
            }
        };
        asyncBuilder
                .setSerialWorkerThreadType(threadType)
                .build();
        assertEquals(threadType, asyncBuilder.getSerialWorkerThreadType());
    }

    @Test
    public void testGetUiThreadType() throws Exception {
        asyncBuilder.build();
        assertTrue(asyncBuilder.getUiThreadType().isInOrderExecutor());
    }

    @Test
    public void testSetUIThreadType() throws Exception {
        asyncBuilder
                .setUIThreadType(asyncBuilder.getSerialWorkerThreadType())
                .build();
        assertEquals(asyncBuilder.getSerialWorkerThreadType(), asyncBuilder.getUiThreadType());
    }

    @Test
    public void testGetNetReadThreadType() throws Exception {
        asyncBuilder.build();
        assertFalse(asyncBuilder.getNetReadThreadType().isInOrderExecutor());
    }

    @Test
    public void testSetNetReadThreadType() throws Exception {
        asyncBuilder
                .setNetReadThreadType(asyncBuilder.getNetWriteThreadType())
                .build();
        assertEquals(asyncBuilder.getNetWriteThreadType(), asyncBuilder.getNetReadThreadType());
    }

    @Test
    public void testGetNetWriteThreadType() throws Exception {
        asyncBuilder
                .setNetWriteThreadType(asyncBuilder.getSerialWorkerThreadType())
                .build();
        assertEquals(asyncBuilder.getSerialWorkerThreadType(), asyncBuilder.getNetWriteThreadType());
    }

    @Test
    public void testSetNetWriteThreadType() throws Exception {

    }

    @Test
    public void testGetFileThreadType() throws Exception {

    }

    @Test
    public void testSetFileThreadType() throws Exception {

    }

    @Test
    public void testGetWorkerExecutorService() throws Exception {

    }

    @Test
    public void testGetSerialWorkerExecutorService() throws Exception {

    }

    @Test
    public void testGetWorkerQueue() throws Exception {

    }

    @Test
    public void testSetWorkerQueue() throws Exception {

    }

    @Test
    public void testGetSerialWorkerQueue() throws Exception {

    }

    @Test
    public void testSetSerialWorkerQueue() throws Exception {

    }

    @Test
    public void testGetFileQueue() throws Exception {

    }

    @Test
    public void testSetFileQueue() throws Exception {

    }

    @Test
    public void testGetNetReadQueue() throws Exception {

    }

    @Test
    public void testSetNetReadQueue() throws Exception {

    }

    @Test
    public void testGetNetWriteQueue() throws Exception {

    }

    @Test
    public void testSetNetWriteQueue() throws Exception {

    }

    @Test
    public void testGetFileExecutorService() throws Exception {

    }

    @Test
    public void testGetNetReadExecutorService() throws Exception {

    }

    @Test
    public void testGetNetWriteExecutorService() throws Exception {

    }

    @Test
    public void testGetUiExecutorService() throws Exception {

    }

    @Test
    public void testSetUiExecutorService() throws Exception {

    }

    @Test
    public void testSetWorkerExecutorService() throws Exception {

    }

    @Test
    public void testSetSerialWorkerExecutorService() throws Exception {

    }

    @Test
    public void testSingleThreadedWorkerExecutorService() throws Exception {

    }

    @Test
    public void testSetFileReadExecutorService() throws Exception {

    }

    @Test
    public void testSetFileWriteExecutorService() throws Exception {

    }

    @Test
    public void testSetNetReadExecutorService() throws Exception {

    }

    @Test
    public void testSetNetWriteExecutorService() throws Exception {

    }

    @Test
    public void testSetUI_Thread() throws Exception {

    }

    @Test
    public void testBuild() throws Exception {

    }
}