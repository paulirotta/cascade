/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.util;

import android.support.annotation.NonNull;

import com.futurice.cascade.Async;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.nonnull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.futurice.cascade.Async.e;

/**
 * This is a marker class to aid in runtime tests.
 */
@NotCallOrigin
public class TypedThread extends Thread {
    private static final String TAG = TypedThread.class.getSimpleName();
    public static final ThreadGroup THREAD_GROUP = new ThreadGroup("ThreadTypeThreadGroup") {
        @Override
        public void uncaughtException(@NonNull @nonnull final Thread t, @NonNull @nonnull final Throwable throwable) {
            e(TAG, "uncaughtException in " + t, throwable);
        }
    };
    /*
     * Each thread may belong to multiple threadTypes. At the moment we only track the
     * first ThreadType which created it. But since the Thread may outlive the ThreadType
     * (if used by other ThreadTypes) we use a WeakReference to allow the original
     * ThreadType to be garbage collected.
     *
     * TODO A better solution would be to track ThreadTypes split Threads without this potential for confusion. To be determined.
     */
    private final CopyOnWriteArrayList<AltWeakReference<IThreadType>> threadTypes = new CopyOnWriteArrayList<>();

    public TypedThread(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final Runnable runnable) {
        super(THREAD_GROUP, runnable);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final Runnable runnable,
            @NonNull @nonnull final String threadName) {
        super(THREAD_GROUP, runnable, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final String threadName) {
        super(THREAD_GROUP, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final ThreadGroup group,
            @NonNull @nonnull final Runnable runnable) {
        super(group, runnable);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final ThreadGroup group,
            @NonNull @nonnull final Runnable runnable,
            @NonNull @nonnull final String threadName) {
        super(group, runnable, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final ThreadGroup group,
            @NonNull @nonnull final String threadName) {
        super(group, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final ThreadGroup group,
            @NonNull @nonnull final Runnable runnable,
            @NonNull @nonnull final String threadName,
            final long stackSize) {
        super(group, runnable, threadName, stackSize);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    @NonNull
    @nonnull
    public List<IThreadType> getThreadTypes() {
        Iterator<AltWeakReference<IThreadType>> iterator = threadTypes.iterator();
        ArrayList<IThreadType> currenThreadTypes = new ArrayList<>(threadTypes.size());

        while (iterator.hasNext()) {
            final AltWeakReference<IThreadType> wr = iterator.next();
            final IThreadType threadType = wr.get();

            if (threadType != null) {
                currenThreadTypes.add(threadType);
            } else {
                threadTypes.remove(wr);
            }
        }

        return currenThreadTypes;
    }

    @NonNull
    @nonnull
    public IThreadType getThreadType() {
        final List<IThreadType> currentThreadTypes = getThreadTypes();
        IThreadType currentThreadType = null;

        if (currentThreadTypes.size() == 1) {
            currentThreadType = currentThreadTypes.get(0);
        } else {
            Async.vv(this, currentThreadTypes.size() + " threadTypes for this Thread, can not disambiguate");
        }

        return currentThreadType;
    }
}
