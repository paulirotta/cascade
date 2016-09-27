package com.reactivecascade;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.reactivecascade.util.FileUtil;
import com.reactivecascade.util.NetUtil;
import com.reactivecascade.util.TestUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AsyncBuilderIntegrationTest extends CascadeIntegrationTest {
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        new AsyncBuilder(appContext)
                .setStrictMode(false)
                .build();
    }

    @Test
    public void testAppContext() throws Exception {
        assertNotNull(appContext.getAssets().getLocales());
    }
}
