/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.util;

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
 * This is designed for allowing one of the {@link com.futurice.cascade.Async#WORKER} threads to
 * operate as an in-order single threaded executor which reverts to help with the common
 * {@link com.futurice.cascade.AsyncBuilder#getWorkerQueue()} tasks when no in-order tasks are pending.
 * <p>
 * Note clearly there is an upside and downside to this design vs making your own {@link com.futurice.cascade.i.IThreadType}.
 * The upside is performance and lower peak memory usage. We have fewer threads contending for background work so less resources
 * and less and faster mContext switches (mContext switches tend to cost marginally more as thread count
 * increases). The downside is delays fom other background tasks unrelated to this may slow the start
 * of execution. A very slow task pulled from the {@link com.futurice.cascade.Async#WORKER}
 * mQueue and perhaps unrelated to the current focus of your attention will, once started, block the
 * next {@link DoubleQueue} item from
 * starting until it completes.
 * <p>
 * In practice this performs well for most uses since everything is best effort anyway and the single
 * thread has absolute priority. If starting as soon as possible is absolutely critical, use a dedicated {@link com.futurice.cascade.i.IThreadType} instead.
 *
 * @param <E>
 */
public class DoubleQueue<E> extends LinkedBlockingQueue<E> {
    private static final long TAKE_POLL_INTERVAL = 50; //ms polling two queues
    @NonNull
    final BlockingQueue<E> lowPriorityQueue;

    public DoubleQueue(@NonNull final BlockingQueue<E> lowPriorityQueue) {
        super();

        this.lowPriorityQueue = lowPriorityQueue;
    }

    @Nullable
    @CallSuper
    @Override // LinkedBlockingQueue
    public E peek() {
        E e = super.peek();

        if (e == null) {
            e = lowPriorityQueue.peek();
        }

        return e;
    }

    @Nullable
    @CallSuper
    @Override // LinkedBlockingQueue
    public E poll() {
        E e = super.poll();

        if (e == null) {
            e = lowPriorityQueue.poll();
        }

        return e;
    }

    @Nullable
    @CallSuper
    @Override // LinkedBlockingQueue
    public E poll(long timeout,
                  @NonNull TimeUnit unit) throws InterruptedException {
        E e = super.poll(timeout, unit);

        if (e == null) {
            e = lowPriorityQueue.poll();
        }

        return e;
    }

    @CallSuper
    @Override // LinkedBlockingQueue
    public boolean remove(@Nullable Object o) {
        return super.remove(o) || lowPriorityQueue.remove(o);
    }

    @CallSuper
    @Override // LinkedBlockingQueue
    public void put(@NonNull E e) throws InterruptedException {
        super.put(e);
        synchronized (this) { //TODO Refactor to get rid of mutex
            this.notifyAll();
        }
    }

    /**
     * Poll both queues for work to do. This will wake up immediately if new work is added to this
     * mQueue, and within the next polling time window for the lowPriorityQueue. Since other threads
     * which may be taking work from the low priority mQueue are probably waking up immediately this
     * is OK. It keeps any dual-use thread associated with this mQueue relatively free for immediate
     * response to the single use mQueue until such time as all other threads are busy, subscribe it pitches
     * in on the work any of them can do.
     *
     * @return
     * @throws InterruptedException
     */
    @Override // LinkedBlockingQueue
    @CallSuper
    @NonNull
    public synchronized E take() throws InterruptedException {
        E e;

        do {
            e = poll();
            if (e == null) {
                e = lowPriorityQueue.poll();
            }
            if (e != null) {
                e.wait(TAKE_POLL_INTERVAL);
            }
        } while (e == null);

        return e;
    }
}
