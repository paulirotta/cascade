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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This is a marker class to aid in runtime tests.
 */
@NotCallOrigin
public class TypedThread extends Thread {
    public static final ThreadGroup THREAD_GROUP = new ThreadGroup("ThreadTypeThreadGroup") {
        @Override
        public void uncaughtException(@NonNull final Thread t, @NonNull final Throwable throwable) {
            RCLog.e(this, "uncaughtException in " + t, throwable);
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

    public TypedThread(@NonNull IThreadType threadType,
                       @NonNull Runnable runnable) {
        super(THREAD_GROUP, runnable);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(@NonNull IThreadType threadType,
                       @NonNull Runnable runnable,
                       @NonNull String threadName) {
        super(THREAD_GROUP, runnable, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(@NonNull IThreadType threadType,
                       @NonNull String threadName) {
        super(THREAD_GROUP, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(@NonNull IThreadType threadType,
                       @NonNull ThreadGroup group,
                       @NonNull Runnable runnable) {
        super(group, runnable);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(@NonNull IThreadType threadType,
                       @NonNull ThreadGroup group,
                       @NonNull Runnable runnable,
                       @NonNull String threadName) {
        super(group, runnable, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(@NonNull IThreadType threadType,
                       @NonNull ThreadGroup group,
                       @NonNull String threadName) {
        super(group, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(@NonNull IThreadType threadType,
                       @NonNull ThreadGroup group,
                       @NonNull Runnable runnable,
                       @NonNull String threadName,
                       long stackSize) {
        super(group, runnable, threadName, stackSize);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    @NonNull
    public List<IThreadType> getThreadTypes() {
        Iterator<AltWeakReference<IThreadType>> iterator = threadTypes.iterator();
        ArrayList<IThreadType> currentThreadTypes = new ArrayList<>(threadTypes.size());

        while (iterator.hasNext()) {
            AltWeakReference<IThreadType> wr = iterator.next();
            IThreadType threadType = wr.get();

            if (threadType != null) {
                currentThreadTypes.add(threadType);
            } else {
                threadTypes.remove(wr);
            }
        }

        return currentThreadTypes;
    }

    @NonNull
    public IThreadType getThreadType() {
        List<IThreadType> currentThreadTypes = getThreadTypes();
        IThreadType currentThreadType = Async.NON_CASCADE_THREAD;

        if (currentThreadTypes.size() == 1) {
            currentThreadType = currentThreadTypes.get(0);
        } else {
            RCLog.v(this, currentThreadTypes.size() + " threadTypes for this Thread, can not disambiguate");
        }

        return currentThreadType;
    }
}
