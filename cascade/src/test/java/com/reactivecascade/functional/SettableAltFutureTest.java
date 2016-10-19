package com.reactivecascade.functional;

import com.reactivecascade.Async;
import com.reactivecascade.AsyncBuilder;

import org.junit.Test;

import mockit.Mocked;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SettableAltFutureTest {
    private Async async;

    @Mocked
    AsyncBuilder asyncBuilder;

    @Mocked
    AsyncBuilder mockAsync;

    @Test
    public void testSet() {
        String s = "BAA";
        SettableAltFuture<String> settableAltFuture = new SettableAltFuture<>(Async.UI);

        settableAltFuture.set(s);
        assertTrue(settableAltFuture.isDone());
        assertEquals(s, settableAltFuture.get());
    }
}
