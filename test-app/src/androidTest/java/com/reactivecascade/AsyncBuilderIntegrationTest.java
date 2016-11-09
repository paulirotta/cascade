package com.reactivecascade;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public abstract class AsyncBuilderIntegrationTest extends CascadeIntegrationTest {
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        new AsyncBuilder(getContext())
                .setStrictMode(false)
                .build();
    }
}
