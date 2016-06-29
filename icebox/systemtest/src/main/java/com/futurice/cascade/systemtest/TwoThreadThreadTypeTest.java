package com.reactivecascade.systemtest;

import com.reactivecascade.util.TypedThread;
import com.reactivecascade.util.DefaultThreadType;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Paul Houghton on 16-03-2015.
 */
public class TwoThreadThreadTypeTest extends ThreadTypeTest {
    public TwoThreadThreadTypeTest() {
        super();
        tag = this.getClass().getSimpleName();
        AtomicInteger i = new AtomicInteger();
        LinkedBlockingDeque<Runnable> q = new LinkedBlockingDeque<>();
        threadType = new DefaultThreadType("TwoThreadAspect", new ThreadPoolExecutor(
                2,
                2,
                1000,
                TimeUnit.MILLISECONDS,
                q,
                r ->
                        new TypedThread(threadType, r, "TwoThreadAspectTestThread" + i.getAndIncrement())
        ),
                q);
    }
}
