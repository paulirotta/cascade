package com.reactivecascade.util;

import android.support.annotation.NonNull;

import com.reactivecascade.i.INamed;
import com.reactivecascade.i.IThreadType;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A tailored {@link ThreadPoolExecutor} with graceful degredation when the queue length grows beyond
 * the normal bounds. Under heavy load when the associated task queue is full, the calling thread will
 * complete the execution thereby providing back pressure to prevent the queue from growing too large.
 * <p>
 * This is suitable only for <code>{@link IThreadType#isInOrderExecutor() == false}</code> since the
 * overflow policy does not guarantee in-order execution.
 */
public class AsyncThreadTypeExecutor extends ThreadPoolExecutor implements INamed {
    private static final String TAG = AsyncThreadTypeExecutor.class.getSimpleName();
    private final String name;

    public AsyncThreadTypeExecutor(@NonNull String name,
                                   int corePoolSize,
                                   int maximumPoolSize,
                                   long keepAliveTime,
                                   @NonNull TimeUnit unit,
                                   @NonNull BlockingQueue<Runnable> workQueue,
                                   @NonNull ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, getRejectionHandler(name));

        this.name = name;
    }

    @NonNull
    private static RejectedExecutionHandler getRejectionHandler(@NonNull String name) {
        return new CallerRunsPolicy() {
            @Override
            public void rejectedExecution(@NonNull Runnable r,
                                          @NonNull ThreadPoolExecutor executor) {
                RCLog.i(TAG, name + " rejected execution- it will be performed by the caller as backpressure");

                super.rejectedExecution(r, executor);
            }
        };
    }

    /**
     * Called automatically when the thread pool shuts down
     */
    @Override
    public void terminated() {
        RCLog.i(this, "ThreadType " + getName() + " terminated, queueSize=" + getQueue().size());
    }

    @NonNull
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "AsyncThreadTypeExecutor{" +
                "name='" + name +
                "\' queueSize=" +
                this.getQueue().size() +
                '}';
    }
}
