package com.reactivecascade;

import android.content.Context;
import android.content.res.Resources;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TestTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    Resources resources;

    @Mock
    Context context;

    @Before
    public void setUp() {
        when(context.getResources()).thenReturn(resources);
    }

    @Test
    public void test() {
        assertTrue(true);
    }
}
