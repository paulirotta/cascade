package com.reactivecascade;

import android.support.test.runner.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public abstract class AsyncBuilderIntegrationTest extends CascadeIntegrationTest {
    protected AsyncBuilder asyncBuilder;
    protected Async async;

    @BeforeClass
    public void setUpClass() throws Exception {
        asyncBuilder = new AsyncBuilder(getContext()).setStrictMode(false);
        async = asyncBuilder.build();
    }
}
