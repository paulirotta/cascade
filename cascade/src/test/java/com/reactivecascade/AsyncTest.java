package com.reactivecascade;

import android.content.Context;
import android.content.res.Resources;

import org.junit.Test;

import mockit.Mocked;

import static junit.framework.Assert.assertTrue;

public class AsyncTest {
    @Mocked
    Resources resources;

    @Mocked
    Context context;

    @Test
    public void test() {
        assertTrue(true);
    }
}
