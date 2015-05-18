/*
 Copyright (c) 2015 Futurice GmbH. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 - Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package com.futurice.cascade.rest;

import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.IKeyFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Fetch the highest priority URL available at the last possible moment as the {@link RESTService}
 * is unblocked
 * <p>
 * Example to automatically start fetching the highest priority URL from the web as soon as the
 * web is not exceeding current bandwidth concurrency constraints:
 * <p><pre><code>
 * imageFactory = new PrioritizedUrlFactory<ImageUrl>(10, mostRecentFirstImageUrlComparator);
 * imageFactory.onAdd(ALog.NET.getAsync(imageFactory));
 * <p>
 * ..
 * <p>
 * ImageUrl imageUrl = new ImageUrl("http://this.com/image.jpg", -currentTime, ALog.UI(updateDisplay());
 * // Fetch sorted by most recently added first split call updateDisplay() from the UI thread
 * imageFactory.add(imageUrl);
 * </code></pre>
 *
 * @param <KF>
 */
public class PrioritizedKeyFactory<KEY, KF extends IKeyFactory<KEY>> implements IKeyFactory<KEY>, Comparable<PrioritizedKeyFactory> {
    private final PriorityBlockingQueue<KF> queue;
    private final int priority;
    private volatile IActionOne<KEY> onAddAction = null;

    //TODO FIX the T element, should be sortable String by priority
    public PrioritizedKeyFactory(int priority, int initialCapacity, Comparator<KF> comparator) {
        this.priority = priority;
        this.queue = new PriorityBlockingQueue<KF>(initialCapacity, comparator);
    }

    public PrioritizedKeyFactory<KEY, KF> setOnAdd(IActionOne<KEY> action) {
        this.onAddAction = action;

        return this;
    }

    /**
     * Add an item to the prioritized collection of URLs
     * <p>
     * {@link #setOnAdd(com.futurice.cascade.i.action.IActionOne)} call onFireAction will be executed on the <code>onAddAsync</code>
     * specified at creation time (or the current thread if this paramter was null) after each
     * successful <code>add()</code>
     *
     * @param sortableUrl
     * @return
     */
    public boolean add(KF sortableUrl) throws Exception {
        boolean added = this.queue.add(sortableUrl);
        final IActionOne binding;

        if (added && (binding = onAddAction) != null) {
            binding.call(sortableUrl);
        }

        return added;
    }

    /**
     * Offer the next high priority element, or null if no more elements
     *
     * @return
     */
    public KF poll() {
        return this.queue.poll();
    }

    /**
     * Atomically remove all elements from the queue
     *
     * @return a List of the items in the queue at the time it was cleared
     */
    public List<KF> clear() {
        ArrayList<KF> queued = new ArrayList<>(this.queue.size());
        this.queue.drainTo(queued);

        return queued;
    }

    @Override
    public KEY getKey() {
        KF KF = queue.poll();

        if (KF != null) {
            return KF.getKey();
        }

        return null;
    }

    @Override
    public int compareTo(PrioritizedKeyFactory another) {
        return this.priority - another.priority;
    }

    @Override
    public boolean equals(Object o) {
        return o != null
                && o instanceof PrioritizedKeyFactory
                && ((PrioritizedKeyFactory) o).queue.equals(queue)
                && ((PrioritizedKeyFactory) o).priority == priority
                && ((PrioritizedKeyFactory) o).onAddAction.equals(onAddAction);
    }

    @Override
    public int hashCode() {
        return 42; // Hashcode not defined
    }


    @Override
    public String toString() {
        return "PrioritizedUrlFactory- Priority:" + priority + " Size:" + queue.size();
    }
}
