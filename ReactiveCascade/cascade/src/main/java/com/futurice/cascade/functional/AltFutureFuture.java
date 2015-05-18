/*
The MIT License (MIT)

Copyright (c) 2015 Futurice Oy and individual contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.futurice.cascade.functional;

import com.futurice.cascade.*;
import com.futurice.cascade.i.action.*;
import com.futurice.cascade.i.functional.*;

import java.util.concurrent.*;

/**
 * A {@link java.util.concurrent.Future} which can be used to safely wait for the results
 * from an {@link com.futurice.cascade.i.functional.IAltFuture}.
 * <p>
 * Normally we don't like to hold one thread waiting for the result of another thread. Doing this
 * on a routine basis causes lots of context switching and can backlog into either many threads
 * or deadlock because a limited number of threads are all in use.
 * <p>
 * This can be useful for example to externally and synchronously test the results of an
 * asynchronous process. There is no risk because the situation is tightly controlled and
 * requires performance be secondary to rigid conformance.
 *
 * @param <T>
 */
public class AltFutureFuture<T> implements Future<T> {
    private static final long CHECK_INTERVAL = 50; // This is a fallback in case you for example have an error and fail to altFuture.notifyAll() when finished
    private final IAltFuture<?, T> altFuture;
    private final Object mutex = new Object();

    public AltFutureFuture(IAltFuture<?, T> altFuture) {
        this.altFuture = altFuture;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return altFuture.cancel("DoneFuture was cancelled");
    }

    @Override
    public boolean isCancelled() {
        return altFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return altFuture.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            try {
                Async.throwTimeoutException(this, "Timeout waiting for AltFuture to complete. Did you remember to .fork()?, new RuntimeException");
            } catch (TimeoutException e1) {
                throw new ExecutionException(e1);
            }
            return null; // This line is never reached but the IDE doesn't know that
        }
    }

    private static final long TIMEOUT = 9000; //ms hold if the thread is blocked this entire time

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        final long t = System.currentTimeMillis();
        final long endTime = t + unit.toMillis(timeout);

        if (!isDone()) {
            final IAction<T> action = () -> {
                // Attach this to speed up and notify to continue the Future when the AltFuture finishes
                // For speed, we don't normally notify after AltFuture end
                synchronized (mutex) {
                    mutex.notifyAll();
                }
            };
            altFuture.then(action);
        }
        while (!isDone()) {
            if (System.currentTimeMillis() >= endTime) {
                Async.throwTimeoutException(this, "Waited " + (System.currentTimeMillis() - t) + "ms for AltFuture to end: " + altFuture);
            }
            synchronized (mutex) {
                final long t2 = Math.min(CHECK_INTERVAL, endTime - System.currentTimeMillis());
                if (t2 > 0) {
                    mutex.wait(t2);
                }
            }
        }
        if (altFuture.isCancelled()) {
            return null;
        }

        return altFuture.get();
    }
}
