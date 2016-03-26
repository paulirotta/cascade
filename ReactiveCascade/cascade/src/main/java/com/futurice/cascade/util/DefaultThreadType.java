/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.util;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.NotCallOrigin;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * The default implementation of the subscribe executor
 * <p>
 * Other classes may utilize this with startup parameter, extend it or substitute an own fresh
 * implementation to the {@link com.futurice.cascade.i.IThreadType} interface.
 */
@NotCallOrigin
public class DefaultThreadType extends AbstractThreadType {
    final boolean inOrderExecution;
    private volatile boolean wakeUpIsPending = false; // Efficiency filter to wake the ServiceExecutor only once TODO Is there a simpler way with AtomicBoolean?
    private final Runnable wakeUpRunnable = () -> {
        // Do nothing, this is just used for insurance fast flushing the ServiceExecutor mQueue when items are added out-of-order to the associated BlockingQueue
        wakeUpIsPending = false;
    };

    /**
     * Construct a new thread group
     *
     * @param name
     * @param executorService
     * @param queue           may be null in which
     *                        case {@link #isInOrderExecutor()} will return <code>true</code>
     *                        ; may be {@link java.util.concurrent.BlockingDeque} in which
     *                        case {@link #isInOrderExecutor()} will return <code>false</code>
     */
    public DefaultThreadType(
            @NonNull String name,
            @NonNull ExecutorService executorService,
            @NonNull BlockingQueue<Runnable> queue) {
        super(name, executorService, queue);

        this.inOrderExecution = queue instanceof BlockingDeque;
    }

    @Override // IThreadType
    public void run(@NonNull Runnable runnable) {
        if (executorService.isShutdown()) {
            return;
        }

        executorService.submit(runnable);
    }

    @Override // IThreadType
    @SuppressWarnings("unchecked")
    @NotCallOrigin
    public void runNext(@NonNull final Runnable runnable) {
        if (executorService.isShutdown()) {
            return;
        }

        int n;
        if (inOrderExecution || (n = mQueue.size()) == 0) {
            run(runnable);
            return;
        }

        // Out of order execution is permitted and desirable to finish functional chains we have started before clouding memory and execution queues by starting more
        if (isInOrderExecutor()) {
            RCLog.v(this, "WARNING: runNext() on single threaded IThreadType. This will be run FIFO only after previously queued tasks");
            mQueue.add(runnable);
        } else {
            ((BlockingDeque) mQueue).addFirst(runnable);
        }
        if (!wakeUpIsPending && ++n != mQueue.size()) {
            // The mQueue changed during submit- just be sure something is submitted to wake the executor right now to pull from the mQueue
            wakeUpIsPending = true;
            executorService.execute(wakeUpRunnable);
        }
    }

    @Override // IThreadType
    public boolean isInOrderExecutor() {
        return inOrderExecution;
    }
}
