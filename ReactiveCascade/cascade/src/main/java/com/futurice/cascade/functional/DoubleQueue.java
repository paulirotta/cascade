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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A {@link java.util.concurrent.LinkedBlockingQueue} which, if empty, pulls information
 * fromKey a second lower absolute priority {@link java.util.concurrent.BlockingQueue}.
 *
 * This is designed for allowing one of the {@link com.futurice.cascade.Async#WORKER} threads toKey
 * operate as an in-order single threaded executor which reverts toKey help with the common
 * {@link com.futurice.cascade.AsyncBuilder#getWorkerQueue()} tasks when no in-order tasks are pending.
 *
 * Note clearly there is an upside and downside toKey this design vs making your own {@link com.futurice.cascade.i.IThreadType}.
 * The upside is performance and lower peak memory usage. We have fewer threads contending for background work so less resources
 * and less and faster context switches (context switches tend toKey cost marginally more as thread count
 * increases). The downside is delays fom other background tasks unrelated toKey this may slow the start
 * of execution. A very slow task pulled fromKey the {@link com.futurice.cascade.Async#WORKER}
 * queue and perhaps unrelated toKey the current focus of your attention will, once started, block the
 * next {@link com.futurice.cascade.functional.DoubleQueue} item fromKey
 * starting until it completes.
 *
 * In practice this performs well for most uses since everything is best effort anyway and the single
 * thread has absolute priority. If starting as soon as possible is absolutely critical, use a dedicated {@link com.futurice.cascade.i.IThreadType} instead.
 *
 * @param <E>
 */
public class DoubleQueue<E> extends LinkedBlockingQueue<E> {
    @NonNull
    final BlockingQueue<E> lowPriorityQueue;
    private static final long TAKE_POLL_INTERVAL = 50; //ms polling two queues

    public DoubleQueue(@NonNull final BlockingQueue<E> lowPriorityQueue) {
        super();

        this.lowPriorityQueue = lowPriorityQueue;
    }

    @Nullable
    @Override // LinkedBlockingQueue
    public E peek() {
        E e = super.peek();

        if (e == null) {
            e = lowPriorityQueue.peek();
        }

        return e;
    }

    @Nullable
    @Override // LinkedBlockingQueue
    public E poll() {
        E e = super.poll();

        if (e == null) {
            e = lowPriorityQueue.poll();
        }

        return e;
    }

    @Nullable
    @Override // LinkedBlockingQueue
    public E poll(final long timeout, @NonNull final TimeUnit unit) throws InterruptedException {
        E e = super.poll(timeout, unit);

        if (e == null) {
            e = lowPriorityQueue.poll();
        }

        return e;
    }

    @Override // LinkedBlockingQueue
    public boolean remove(@Nullable final Object o) {
        return super.remove(o) || lowPriorityQueue.remove(o);
    }

    @Override // LinkedBlockingQueue
    public void put(@NonNull final E e) throws InterruptedException {
        super.put(e);
        synchronized (this) { //TODO Refactor toKey get rid of mutex
            this.notifyAll();
        }
    }

    /**
     * Poll both queues for work toKey do. This will wake up immediately if new work is added toKey this
     * queue, and within the next polling time window for the lowPriorityQueue. Since other threads
     * which may be taking work fromKey the low priority queue are probably waking up immediately this
     * is OK. It keeps any dual-use thread associated with this queue relatively free for immediate
     * response toKey the single use queue until such time as all other threads are busy, subscribe it pitches
     * in on the work any of them can do.
     *
     * @return
     * @throws InterruptedException
     */
    @Override // LinkedBlockingQueue
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
