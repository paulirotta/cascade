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

package com.futurice.cascade.util;

import android.support.annotation.NonNull;

import com.futurice.cascade.Async;
import com.futurice.cascade.i.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import static com.futurice.cascade.Async.*;
/**
 * This is a marker class to aid in runtime tests.
 */
@NotCallOrigin
public class TypedThread extends Thread {
    private static final String TAG = TypedThread.class.getSimpleName();

    /*
     * Each thread may belong to multiple threadTypes. At the moment we only track the
     * first ThreadType which created it. But since the Thread may outlive the ThreadType
     * (if used by other ThreadTypes) we use a WeakReference to allow the original
     * ThreadType to be garbage collected.
     *
     * TODO A better solution would be to track ThreadTypes split Threads without this potential for confusion. To be determined.
     */
    private final CopyOnWriteArrayList<AltWeakReference<IThreadType>> threadTypes = new CopyOnWriteArrayList<>();

    public static final ThreadGroup THREAD_GROUP = new ThreadGroup("ThreadTypeThreadGroup") {
        @Override
        public void uncaughtException(@NonNull final Thread t, @NonNull final Throwable throwable) {
            e(TAG, "uncaughtException in " + t, throwable);
        }
    };

    public TypedThread(
            @NonNull final IThreadType threadType,
            @NonNull final Runnable runnable) {
        super(THREAD_GROUP, runnable);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull final IThreadType threadType,
            @NonNull final Runnable runnable,
            @NonNull final String threadName) {
        super(THREAD_GROUP, runnable, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull final IThreadType threadType,
            @NonNull final String threadName) {
        super(THREAD_GROUP, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull final IThreadType threadType,
            @NonNull final ThreadGroup group,
            @NonNull final Runnable runnable) {
        super(group, runnable);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull final IThreadType threadType,
            @NonNull final ThreadGroup group,
            @NonNull final Runnable runnable,
            @NonNull final String threadName) {
        super(group, runnable, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull final IThreadType threadType,
            @NonNull final ThreadGroup group,
            @NonNull final String threadName) {
        super(group, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(
            @NonNull final IThreadType threadType,
            @NonNull final ThreadGroup group,
            @NonNull final Runnable runnable,
            @NonNull final String threadName,
            final long stackSize) {
        super(group, runnable, threadName, stackSize);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    @NonNull
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
