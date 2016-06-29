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
package com.reactivecascade.rest;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.reactivecascade.i.IGettable;
import com.reactivecascade.i.action.IActionOne;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Fetch the highest priority URL available at the last possible moment as the {@link AbstractRESTService}
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
 * @param <KEY> should implement {@link #toString()} which returns only the primary part of the key,
 *              not any supplimental data such as the priority used for comparison
 */
public class PrioritizedGettable<KEY extends Comparable> implements IGettable<KEY> {
    private final PriorityBlockingQueue<KEY> queue;
    private final int priority;
    private final IActionOne<KEY> onAddAction;

    public PrioritizedGettable(
            final int priority,
            @NonNull final Comparator<KEY> comparator) {
        this(priority, 10, comparator, null);
    }

    public PrioritizedGettable(
            final int priority,
            final int initialCapacity,
            @NonNull final Comparator<KEY> comparator,
            @Nullable final IActionOne<KEY> onAddAction) {
        this.priority = priority;
        this.queue = new PriorityBlockingQueue<>(initialCapacity, comparator);
        this.onAddAction = onAddAction;
    }

    /**
     * Add an item to the prioritized collection of URLs
     * <p>
     * The onAddAction (if any was supplied in the constructor) will be executed on the <code>onAddAsync</code>
     * specified at creation time (or the current thread if this paramter was null) after each
     * successful <code>add()</code>
     *
     * @param key
     * @return
     */
    public boolean add(@NonNull final KEY key) throws Exception {
        boolean added = this.queue.add(key);

        if (added && onAddAction != null) {
            onAddAction.call(key);
        }

        return added;
    }

    public boolean remove(@NonNull final KEY key) {
        return this.queue.remove(key);
    }

    /**
     * Offer the next high priority element, or null if no more elements
     *
     * @return
     */
    @Nullable
    public KEY poll() {
        return this.queue.poll();
    }

    /**
     * Atomically remove all elements from the queue
     *
     * @return a List of the items in the queue at the time it was cleared
     */
    @NonNull
    public List<KEY> clear() {
        final ArrayList<KEY> queued = new ArrayList<>(this.queue.size());
        this.queue.drainTo(queued);

        return queued;
    }

    @Override
    @Nullable
    public KEY get() {
        return queue.poll();
    }

    @Override
    public boolean equals(@NonNull final Object o) {
        return o instanceof PrioritizedGettable
                && ((PrioritizedGettable) o).queue.equals(queue)
                && ((PrioritizedGettable) o).priority == priority
                && ((PrioritizedGettable) o).onAddAction.equals(onAddAction);
    }

    @Override
    public int hashCode() {
        return queue.hashCode() ^ priority ^ (onAddAction == null ? 0 : onAddAction.hashCode());
    }

    @Override
    @Nullable
    public String toString() {
        final KEY key = get();

        if (key == null) {
            return null;
        }

        return key.toString();
    }
}
