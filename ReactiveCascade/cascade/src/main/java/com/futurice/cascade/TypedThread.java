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

package com.futurice.cascade;

import com.futurice.cascade.i.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This is a marker class to aid in runtime tests.
 * <p>
 * If you create a custom {@link com.futurice.cascade.i.IThreadType} or {@link java.util.concurrent.ExecutorService}, the <code>Thread</code>s
 * should come from this marker class to avoid breaking any application code that makes use of
 * {@link Async#assertTypedThread()}
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
        public void uncaughtException(Thread t, Throwable throwable) {
            Async.e(TAG, "uncaughtException in " + t, throwable);
        }
    };

    public TypedThread(IThreadType threadType, Runnable runnable) {
        super(THREAD_GROUP, runnable);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(IThreadType threadType, Runnable runnable, String threadName) {
        super(THREAD_GROUP, runnable, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(IThreadType threadType, String threadName) {
        super(THREAD_GROUP, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(IThreadType threadType, ThreadGroup group, Runnable runnable) {
        super(group, runnable);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(IThreadType threadType, ThreadGroup group, Runnable runnable, String threadName) {
        super(group, runnable, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(IThreadType threadType, ThreadGroup group, String threadName) {
        super(group, threadName);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

    public TypedThread(IThreadType threadType, ThreadGroup group, Runnable runnable, String threadName, long stackSize) {
        super(group, runnable, threadName, stackSize);

        this.threadTypes.add(new AltWeakReference<>(threadType));
    }

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
