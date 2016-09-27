/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A {@link java.util.concurrent.LinkedBlockingQueue} which, if empty, pulls information
 * from a second lower absolute priority {@link java.util.concurrent.BlockingQueue}.
 * <p>
 * This is designed for allowing one of the {@link com.reactivecascade.Async#WORKER} threads to
 * operate as an in-order single threaded executor which reverts to help with the common
 * {@link com.reactivecascade.AsyncBuilder#getWorkerQueue()} tasks when no in-order tasks are pending.
 * <p>
 * Note clearly there is an upside and downside to this design vs making your own {@link com.reactivecascade.i.IThreadType}.
 * The upside is performance and lower peak memory usage. We have fewer threads contending for background work so less resources
 * and less and faster context switches (context switches tend to cost marginally more as thread count
 * increases). The downside is delays fom other background tasks unrelated to this may slow the start
 * of execution. A very slow task pulled from the {@link com.reactivecascade.Async#WORKER}
 * queue and perhaps unrelated to the current focus of your attention will, once started, block the
 * next {@link DoubleQueue} item from
 * starting until it completes.
 * <p>
 * In practice this performs well for most uses since everything is best effort anyway and the single
 * thread has absolute priority. If starting as soon as possible is absolutely critical, use a dedicated {@link com.reactivecascade.i.IThreadType} instead.
 *
 * @param <T> queue item type
 */
public class DoubleQueue<T> extends LinkedBlockingQueue<T> {
    private static final long TAKE_POLL_INTERVAL = 50; //ms polling two queues

    @NonNull
    final BlockingQueue<T> lowPriorityQueue;

    public DoubleQueue(@NonNull BlockingQueue<T> lowPriorityQueue) {
        super();

        this.lowPriorityQueue = lowPriorityQueue;
    }

    @Nullable
    @CallSuper
    @Override // LinkedBlockingQueue
    public T peek() {
        T e = super.peek();

        if (e == null) {
            e = lowPriorityQueue.peek();
        }

        return e;
    }

    @Nullable
    @CallSuper
    @Override // LinkedBlockingQueue
    public T poll() {
        T e = super.poll();

        if (e == null) {
            e = lowPriorityQueue.poll();
        }

        return e;
    }

    @Nullable
    @CallSuper
    @Override // LinkedBlockingQueue
    public T poll(long timeout,
                  @NonNull TimeUnit unit) throws InterruptedException {
        T t = super.poll(timeout, unit);

        if (t == null) {
            t = lowPriorityQueue.poll();
        }

        return t;
    }

    @CallSuper
    @Override // LinkedBlockingQueue
    public boolean remove(@Nullable Object o) {
        return super.remove(o) || lowPriorityQueue.remove(o);
    }

    @CallSuper
    @Override // LinkedBlockingQueue
    public void put(@NonNull T t) throws InterruptedException {
        super.put(t);

        synchronized (this) { //TODO Refactor to get rid of mutex
            this.notifyAll();
        }
    }

    /**
     * Poll both queues for work to do. This will wake up immediately if new work is added to this
     * queue, and within the next polling time window for the lowPriorityQueue. Since other threads
     * which may be taking work from the low priority queue are probably waking up immediately this
     * is OK. It keeps any dual-use thread associated with this queue relatively free for immediate
     * response to the single use queue until such time as all other threads are busy, sub it pitches
     * in on the work any of them can do.
     *
     * @return
     * @throws InterruptedException
     */
    @Override // LinkedBlockingQueue
    @CallSuper
    @NonNull
    public synchronized T take() throws InterruptedException {
        T t;

        do {
            t = poll();
            if (t == null) {
                t = lowPriorityQueue.poll();
            }
            if (t != null) {
                t.wait(TAKE_POLL_INTERVAL);
            }
        } while (t == null);

        return t;
    }
}
