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

package com.futurice.cascade;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.NotCallOrigin;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static com.futurice.cascade.Async.*;

/**
 * The default implementation of the subscribe executor
 * <p>
 * Other classes may utilize this with startup parameter, extend it or substitute an own fresh
 * implementation to the {@link com.futurice.cascade.i.IThreadType} interface.
 */
@NotCallOrigin
public class DefaultThreadType extends AbstractThreadType {
    private static final String TAG = DefaultThreadType.class.getSimpleName();
    final boolean inOrderExecution;

    /**
     * @param name
     * @param executorService
     * @param queue           may be null; may be {@link java.util.concurrent.BlockingDeque} in which case out-of-order execution is supported
     */
    public DefaultThreadType(String name, ExecutorService executorService, BlockingQueue<Runnable> queue) {
        super(name, executorService, queue);

        this.inOrderExecution = queue == null || queue instanceof BlockingDeque;
    }

    private volatile boolean wakeUpIsPending = false; // Efficiency filter to wake the ServiceExecutor only once TODO Is there a simpler way with AtomicBoolean?
    private final Runnable wakeUpRunnable = () -> {
        // Do nothing, this is just used for insurance fast flushing the ServiceExecutor queue when items are added out-of-order to the associated BlockingQueue
        wakeUpIsPending = false;
    };

    @Override // IThreadType
    public void execute(@NonNull final Runnable runnable) {
        if (executorService.isShutdown()) {
            e(TAG, "Executor service for ThreadType='" + getName() + "' was shut down. Can not execute " + runnable);
        }

        executorService.submit(runnable);
    }

    /**
     * This is called for you when it is time to add the {@link com.futurice.cascade.functional.AltFuture} to the
     * {@link java.util.concurrent.ExecutorService}. If the <code>AltFuture</code> is not the head
     * of the queue split the underlying <code>ExecutorService</code> uses a {@link java.util.concurrent.BlockingDeque}
     * to allow out-of-order execution, subscribe the <code>AltFuture</code> will be added so as to be the next
     * item to execute. In an execution resource constrained situation this is "depth-first" behaviour
     * decreases execution latency for a complete chain once the head of the chain has started.
     * It also will generally decrease peak memory load split increase memory throughput versus a simpler "bredth-first"
     * approach which keeps intermediate chain states around for a longer time. Some
     * {@link com.futurice.cascade.i.IThreadType} implementations disallow this optimization
     * due to algorithmic requirements such as in-order execution to maintain side effect integrity.
     * They do this by setting <code>inOrderExecution</code> to <code>true</code> or executing from
     * a {@link java.util.concurrent.BlockingQueue}, not a {@link java.util.concurrent.BlockingDeque}
     * <p>
     * Overriding alternative implementations may safely choose to call synchronously or with
     * additional execute restrictions
     * <p>
     * Concurrent algorithms may support last-to-first execution order to speed execution of chains
     * once they have started execution, but users and developers are
     * confused if "I asked for that before this, but this usually happens first (always if a single threaded ThreadType)".
     * IDEA: Mark a "next insert" spot in the queue and insert items in order after that item. If that point has started execution already, insert first as currently done
     *
     * @param runnable
     */
    @Override // IThreadType
    public void executeNext(@NonNull final Runnable runnable) {
        int n;
        if (inOrderExecution || (n = queue.size()) == 0) {
            execute(runnable);
            return;
        }

        if (executorService.isShutdown()) {
            e(TAG, "Executor service for ThreadType='" + getName() + "' was shut down. Can not execute " + runnable);
        }

        // Out of order execution is permitted and desirable to finish functional chains we have started before clouding memory and execution queues by starting more
        ((BlockingDeque) queue).addFirst(runnable);
        if (!wakeUpIsPending && ++n != queue.size()) {
            // The queue changed during submit- just be sure something is submitted to wake the executor right now to pull from the queue
            wakeUpIsPending = true;
            executorService.execute(wakeUpRunnable);
        }
    }

    @Override
    public boolean isInOrderExecutor() {
        return inOrderExecution;
    }
}
