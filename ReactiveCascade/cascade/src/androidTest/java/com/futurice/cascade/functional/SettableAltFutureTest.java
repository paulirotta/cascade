package com.futurice.cascade.functional;

import android.support.annotation.CallSuper;
import android.test.suitebuilder.annotation.SmallTest;

import com.futurice.cascade.Async;
import com.futurice.cascade.AsyncAndroidTestCase;
import com.futurice.cascade.i.IAltFuture;

import org.junit.Before;
import org.junit.Test;

import static com.futurice.cascade.Async.SHOW_ERROR_STACK_TRACES;

@SmallTest
public class SettableAltFutureTest extends AsyncAndroidTestCase {

    @Before
    @CallSuper
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testCancel() throws Exception {
        final SettableAltFuture<Integer> settableAltFuture = new SettableAltFuture<>(Async.WORKER);
        assertTrue(settableAltFuture.cancel("Just because"));
        assertEquals((Object) IAltFuture.VALUE_NOT_AVAILABLE, (Object) settableAltFuture.safeGet());

        SHOW_ERROR_STACK_TRACES = false;
        try {
            settableAltFuture.get();
            throw new IllegalStateException("IllegalStateException should have been thrown, but was not");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Just because"));
        } finally {
            SHOW_ERROR_STACK_TRACES = true;
        }
    }

    @Test
    public void testIsCancelled() throws Exception {
        final SettableAltFuture<Integer> settableAltFuture = new SettableAltFuture<>(Async.WORKER);
        assertFalse(settableAltFuture.isCancelled());
        assertTrue(settableAltFuture.cancel("Just because"));
        assertTrue(settableAltFuture.isCancelled());
    }

    @Test
    public void testIsDone() throws Exception {
        final SettableAltFuture<Integer> settableAltFuture = new SettableAltFuture<>(Async.WORKER);
        assertFalse(settableAltFuture.isDone());
        settableAltFuture.set(42);
        assertTrue(settableAltFuture.isDone());
    }

    @Test
    public void testForkIsDone() throws Exception {
        final SettableAltFuture<Integer> settableAltFuture = new SettableAltFuture<>(Async.WORKER);
        settableAltFuture.fork();
        assertFalse(settableAltFuture.isDone());
        settableAltFuture.set(42);
        assertTrue(settableAltFuture.isDone());
    }

    @Test
    public void testIsForked() throws Exception {
        final SettableAltFuture<Integer> settableAltFuture = new SettableAltFuture<>(Async.WORKER);
        assertFalse(settableAltFuture.isForked());
        settableAltFuture.fork();
        assertTrue(settableAltFuture.isForked());
        settableAltFuture.set(42);
        assertTrue(settableAltFuture.isForked());
    }

    @Test
    public void testForkAfterSet() throws Exception {
        final SettableAltFuture<Integer> settableAltFuture = new SettableAltFuture<>(Async.WORKER);
        settableAltFuture.set(42);
        assertTrue(settableAltFuture.isForked());
        settableAltFuture.fork();
    }

    @Test
    public void testSetPreviousAltFuture() throws Exception {

    }

    @Test
    public void testClearPreviousAltFuture() throws Exception {

    }

    @Test
    public void testGetPreviousAltFuture() throws Exception {

    }

    @Test
    public void testAssertNotDone() throws Exception {

    }

    @Test
    public void testGet() throws Exception {

    }

    @Test
    public void testSafeGet() throws Exception {

    }

    @Test
    public void testGetThreadType() throws Exception {

    }

    @Test
    public void testSet() throws Exception {

    }

    @Test
    public void testCompareAndSet() throws Exception {

    }

    @Test
    public void testDoThenOnCancelled() throws Exception {

    }

    @Test
    public void testDoThenOnError() throws Exception {

    }

    @Test
    public void testOnError() throws Exception {

    }

    @Test
    public void testDoThenActions() throws Exception {

    }

    @Test
    public void testSplit() throws Exception {

    }

    @Test
    public void testThen() throws Exception {

    }

    @Test
    public void testThen1() throws Exception {

    }

    @Test
    public void testThen2() throws Exception {

    }

    @Test
    public void testThen3() throws Exception {

    }

    @Test
    public void testThen4() throws Exception {

    }

    @Test
    public void testThen5() throws Exception {

    }

    @Test
    public void testThen6() throws Exception {

    }

    @Test
    public void testThen7() throws Exception {

    }

    @Test
    public void testThen8() throws Exception {

    }

    @Test
    public void testMap() throws Exception {

    }

    @Test
    public void testMap1() throws Exception {

    }

    @Test
    public void testFilter() throws Exception {

    }

    @Test
    public void testFilter1() throws Exception {

    }

    @Test
    public void testSet1() throws Exception {

    }

    @Test
    public void testSet2() throws Exception {

    }
}