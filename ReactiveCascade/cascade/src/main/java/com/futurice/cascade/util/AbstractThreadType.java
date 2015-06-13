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
import android.support.annotation.Nullable;
import android.util.Log;

import com.futurice.cascade.Async;
import com.futurice.cascade.functional.AltFuture;
import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.action.IActionOneR;
import com.futurice.cascade.i.action.IActionR;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.i.functional.IAltFuture;
import com.futurice.cascade.i.functional.IRunnableAltFuture;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static com.futurice.cascade.Async.assertTrue;
import static com.futurice.cascade.Async.e;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.i;
import static com.futurice.cascade.Async.ii;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.vv;

/**
 * The baseline implementation of ThreadType convenience classes. It provides functional interfaces
 * (lambda-friendly if you are using the RetroLambda library or similar) to execRunnable code in a background
 * WORKER thread pool.
 * <p>
 * For more specialized behaviour a class may choose to replace this.
 * <p>
 * TODO Add a DEBUG build timer which notifies you of dangling forks (.subscribe() which you forget to call .fork() on after some time period)
 */
public abstract class AbstractThreadType implements IThreadType, INamed {
    protected final ExecutorService executorService;
    private final String name;
    protected final BlockingQueue<Runnable> queue;
    protected final ImmutableValue<String> origin;

    /**
     * Create an asynchronous onFireAction handler that embodies certain rules for threading split concurrency
     * in a set of lambda-friendly methods
     *
     * @param executorService the thread or thread pool for this thread type. Threads and thread pools may be
     *                        shared with other thread types, however note that though this this cooperative execution
     *                        reduces context switching and peak memory load it may delay the start of execution
     *                        of tasks in one thread type by tasks in another thread type
     */
    public AbstractThreadType(
            @NonNull final String name,
            @NonNull final ExecutorService executorService,
            @NonNull final BlockingQueue<Runnable> queue) {
        this.name = name;
        this.executorService = executorService;
        this.queue = queue;
        this.origin = originAsync();
    }

//============================= Internal Utility Methods =========================================

    public abstract void run(@NonNull Runnable runnable);

    @Override
    @NonNull
    public <IN> Runnable wrapRunnableAsErrorProtection(@NonNull final IAction<IN> action) {
        return () -> {
            try {
                action.call();
            } catch (Exception e) {
                ee(this, origin, "run(IAction) problem", e);
            }
        };
    }

    @Override
    @NonNull
    public <IN> Runnable wrapRunnableAsErrorProtection(
            @NonNull final IAction<IN> action,
            @NonNull final IOnErrorAction onErrorAction) {
        return () -> {
            try {
                action.call();
            } catch (Exception e) {
                ee(this, origin, "run(Runnable) problem", e);
                try {
                    onErrorAction.call(e);
                } catch (Exception e1) {
                    ee(this, origin, "run(Runnable) problem " + e + " lead to another problem in onErrorAction", e1);
                }
            }
        };
    }

    private boolean isMistakenlyCalledDirectlyFromOutsideTheCascadeLibrary() {
        //TODO This check doesn't really allow 3rd party implementations. Not testing would mean unsafe/less obvious problems can come later. Package hiding would disallow replacement implementations that follow the interface contracts. What we have here is a half measure to guide people since currently there are no alternate implementations.
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        assertTrue("Stack trace[3] is AbstractThreadType.fork(IRunnableAltFuture)", ste[3].getMethodName().contains("fork"));
        return !ste[4].getClassName().startsWith("com.futurice.cascade");
    }

//========================== .subscribe() and .run() Methods ================================

    @Override // IThreadType
    public <IN> void execute(@NonNull final IAction<IN> action) {
        run(wrapRunnableAsErrorProtection(action));
    }

    @Override // IThreadType
    public <IN> void run(
            @NonNull final IAction<IN> action,
            @NonNull final IOnErrorAction onErrorAction) {
        run(wrapRunnableAsErrorProtection(action, onErrorAction));
    }

    @Override // IThreadType
    public <IN> void runNext(@NonNull final IAction<IN> action) {
        runNext(wrapRunnableAsErrorProtection(action));
    }

    @Override // IThreadType
    public boolean moveToHeadOfQueue(@NonNull final Runnable runnable) {
        //TODO Analyze if this non-atomic operation is a risk for closing a ThreadType and moving all pending actions to a new thread type as we would like to do for NET_READ when the available bandwdith changes

        if (!(queue instanceof Deque)) {
            return false; // The UI thread does not have a visible queue, and some queues choose not to support re-ordering
        }

        final boolean moved = queue.remove(runnable);
        if (moved) {
            ((Deque<Runnable>) queue).addFirst(runnable);
        }

        vv(this, origin, "moveToHeadOfQueue() moved=" + moved);
        return moved;
    }

    @Override // IThreadType
    public <IN> void runNext(
            @NonNull final IAction<IN> action,
            @NonNull final IOnErrorAction onErrorAction) {
        vv(this, origin, "runNext()");
        runNext(wrapRunnableAsErrorProtection(action, onErrorAction));
    }

    @Override // IThreadType
    @NonNull
    public <IN> IAltFuture<IN, IN> then(@NonNull final IAction<IN> action) {
        vv(this, origin, "map()");
        return new AltFuture<>(this, action);
    }

    @Override // IThreadType
    @NonNull
    public <IN> IAltFuture<IN, IN> then(@NonNull final IActionOne<IN> action) {
        vv(this, origin, "map()");
        return new AltFuture<>(this, action);
    }

    @Override // IThreadType
    @NonNull
    public <IN, OUT> IAltFuture<IN, OUT> map(@NonNull final IActionOneR<IN, OUT> action) {
        vv(this, origin, "map()");
        return new AltFuture<>(this, action);
    }

    @Override // IThreadType
    @NonNull
    public <IN, OUT> IAltFuture<IN, OUT> then(@NonNull final IActionR<IN, OUT> action) {
        vv(this, origin, "map()");
        return new AltFuture<IN, OUT>(this, action);
    }

    //======================= .subscribe() List Operations =========================================

    @Override // IThreadType
    @NonNull
    public <IN> IAltFuture<?, IN> from(@NonNull final IN value) {
        return then(() -> value);
    }

    @Override // IThreadType
    @SafeVarargs
    @NonNull
    public final <IN> List<IAltFuture<IN, IN>> then(@NonNull final IAction<IN>... actions) {
        final List<IAltFuture<IN, IN>> altFutures = new ArrayList<>(actions.length);
        vv(this, origin, "map(List[" + actions.length + "])");
        for (IAction<IN> action : actions) {
            altFutures.add(then(action));
        }
        return altFutures;
    }

    @Override // IThreadType
    @SafeVarargs
    @NonNull
    public final <IN, OUT> List<IAltFuture<IN, OUT>> then(@NonNull final IActionR<IN, OUT>... actions) {
        vv(this, origin, "map(List[" + actions.length + "])");
        final List<IAltFuture<IN, OUT>> altFutures = new ArrayList<>(actions.length);
        for (IActionR<IN, OUT> action : actions) {
            altFutures.add(then(action));
        }
        return altFutures;
    }

    @Override // IThreadType
    @SafeVarargs
    @NonNull
    public final <IN, OUT> List<IAltFuture<IN, OUT>> map(@NonNull final IActionOneR<IN, OUT>... actions) {
        vv(this, origin, "map(List[" + actions.length + "])");
        final List<IAltFuture<IN, OUT>> altFutures = new ArrayList<>(actions.length);
        for (IActionOneR<IN, OUT> action : actions) {
            altFutures.add(map(action));
        }
        return altFutures;
    }

//=============================== Public Utility Methods ======================================

    //TODO public <A> AltFuture<A> flush()  - current thread type
    //TODO public <A> AltFuture<A> flush(IThreadType threadType)   - wait for everything forked before this point and their side effects queued before other things to complete before next step on the specified threadtype

    @Override // IThreadType
    public <IN, OUT> void fork(@NonNull final IRunnableAltFuture<IN, OUT> runnableAltFuture) {
        assertTrue("AbstractThreadType.fork() expected the IRunnableAltFuture should return isForked() and !isDone()", runnableAltFuture.isForked() && !runnableAltFuture.isDone());
        if (Async.DEBUG && isMistakenlyCalledDirectlyFromOutsideTheCascadeLibrary()) {
            throw new UnsupportedOperationException("Method for internal use only. Please call your IRunnableAltFuture " + runnableAltFuture + ".fork() on instead of calling IThreadType.fork(IRunnableAltFuture)");
        }

        vv(this, origin, "fork()");
        run(runnableAltFuture); // Atomic state checks must be completed later in the .run() method
    }

    @Override // IThreadType
    @NonNull
    public <IN> Future<Boolean> shutdown(
            final long timeout,
            @Nullable final IAction<IN> afterShutdownAction) {
        if (timeout < 1) {
            Async.throwIllegalArgumentException(this, "shutdown(" + timeout + ") is illegal, time must be > 0");
        }
        if (timeout == 0 && afterShutdownAction != null) {
            Async.throwIllegalArgumentException(this, "shutdown(0) is legal, but do not supply a afterShutdownAction() as it would run immediately which is probably an error");
        }
        final ImmutableValue<String> originImmutableValue = originAsync()
                .then(o -> {
                    i(this, "shutdown " + timeout + " origin=" + o + " ThreadType creationOrigin=" + origin.safeGet());
                    executorService.shutdown();
                });
        final FutureTask<Boolean> futureTask = new FutureTask<>(() -> {
            boolean terminated = timeout == 0;
            try {
                if (!terminated) {
                    terminated = executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Log.e(AbstractThreadType.class.getSimpleName(), "Could not shutdown. afterShutdownAction will not be called: " + originImmutableValue, e);
                terminated = false;
            } catch (Exception e) {
                e(this, "Could not shutdown. afterShutdownAction will not be called: " + originImmutableValue, e);
                terminated = false;
            } finally {
                if (terminated && afterShutdownAction != null) {
                    try {
                        afterShutdownAction.call();
                    } catch (Exception e) {
                        ee(this, origin, "Problem during afterShutdownAction after successful workerExecutorService.shutdown: " + originImmutableValue, e);
                        terminated = false;
                    }
                }
            }
            return terminated;
        });
        (new Thread(futureTask, "Shutdown ThreadType " + getName())).start();

        return futureTask;
    }

    @Override // IThreadType
    @NonNull
    public <IN> List<Runnable> shutdownNow(
            @NonNull final String reason,
            @Nullable final IAction<IN> actionOnDedicatedThreadAfterAlreadyStartedTasksComplete,
            @Nullable final IAction<IN> actionOnDedicatedThreadIfTimeout,
            long timeoutMillis) {
        ii(this, "shutdownNow: reason=" + reason);
        final List<Runnable> pendingActions = executorService.shutdownNow();

        if (actionOnDedicatedThreadAfterAlreadyStartedTasksComplete != null) {
            new Thread(() -> {
                try {
                    if (executorService.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
                        actionOnDedicatedThreadAfterAlreadyStartedTasksComplete.call();
                    } else {
                        if (actionOnDedicatedThreadIfTimeout != null) {
                            actionOnDedicatedThreadIfTimeout.call();
                        } else {
                            Log.i(AbstractThreadType.class.getSimpleName(), "Timeout in shutdownNow, reason=" + reason + " for ThreadType " + getName() + ". Consider providing a non-null actionOnDedicatedThreadIfTimeout");
                        }
                    }
                } catch (Exception e) {
                    Log.e(AbstractThreadType.class.getSimpleName(), "Problem in awaitTermination for ServiceExecutor, reason=" + reason + ", ThreadType " + getName(), e);
                }
            }, "shutdownNow" + reason)
                    .start();
        }

        return pendingActions;
    }

    @Override // INamed
    @NonNull
    public String getName() {
        return name;
    }
}
