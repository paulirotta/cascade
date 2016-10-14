package com.reactivecascade.functional;

import com.reactivecascade.Async;
import com.reactivecascade.AsyncBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class SettableAltFutureTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private Async async;

    @Mock
    AsyncBuilder asyncBuilder;

    @Before
    public void setup() {
        async = mock(Async.class);
    }

    @Test
    public void testSet() {
        String s = "BAA";
        SettableAltFuture<String> settableAltFuture = new SettableAltFuture<>(Async.UI);

        settableAltFuture.set(s);
        assertTrue(settableAltFuture.isDone());
        assertEquals(s, settableAltFuture.get());
    }
}
