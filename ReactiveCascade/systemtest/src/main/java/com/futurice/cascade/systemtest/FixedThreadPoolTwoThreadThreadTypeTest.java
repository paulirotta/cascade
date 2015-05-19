package com.futurice.cascade.systemtest;

import com.futurice.cascade.TypedThread;
import com.futurice.cascade.DefaultThreadType;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Paul Houghton on 16-03-2015.
 */
public class FixedThreadPoolTwoThreadThreadTypeTest extends ThreadTypeTest {
    public FixedThreadPoolTwoThreadThreadTypeTest() {
        super();
        tag = this.getClass().getSimpleName();
        AtomicInteger i = new AtomicInteger();
        threadType = new DefaultThreadType("FixedThreadPoolTwoThreadAspect", Executors.newFixedThreadPool(
                2,
                r ->
                        new TypedThread(threadType, r, "FixedThreadPoolTwoThreadAspectTestThread" + i.getAndIncrement())
        ),
                new LinkedBlockingDeque<>());
    }
}
