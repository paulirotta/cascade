/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.util;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.futurice.cascade.Async;
import com.futurice.cascade.active.AltFuture;
import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.active.IRunnableAltFuture;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.active.SettableAltFuture;
import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;

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
 */
public abstract class AbstractThreadType implements IThreadType {
    @NonNull
    protected final ExecutorService executorService;
    @NonNull
    protected final BlockingQueue<Runnable> mQueue;
    @NonNull
    protected final ImmutableValue<String> mOrigin;
    @NonNull
    private final String name;

    /**
     * Create an asynchronous mOnFireAction handler that embodies certain rules for threading split concurrency
     * in a set of lambda-friendly methods
     *
     * @param executorService the thread or thread pool for this thread type. Threads and thread pools may be
     *                        shared with other thread types, however note that though this this cooperative execution
     *                        reduces mContext switching and peak memory load it may delay the start of execution
     *                        of tasks in one thread type by tasks in another thread type
     */
    public AbstractThreadType(
            @NonNull final String name,
            @NonNull final ExecutorService executorService,
            @NonNull final BlockingQueue<Runnable> queue) {
        this.name = name;
        this.executorService = executorService;
        this.mQueue = queue;
        this.mOrigin = originAsync();
    }

//============================= Internal Utility Methods =========================================

    private static boolean isMistakenlyCalledDirectlyFromOutsideTheCascadeLibrary() {
        //TODO This check doesn't really allow 3rd party implementations. Not testing would mean unsafe/less obvious problems can come later. Package hiding would disallow replacement implementations that follow the interface contracts. What we have here is a half measure to guide people since currently there are no alternate implementations.
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        assertTrue("Stack trace[3] is AbstractThreadType.fork(IRunnableAltFuture)", ste[3].getMethodName().contains("fork"));
        return !ste[4].getClassName().startsWith("com.futurice.cascade");
    }

    @NotCallOrigin
    public abstract void run(@NonNull Runnable runnable);

    private <IN, OUT> IAltFuture<IN, OUT> runAltFuture(@NonNull final IRunnableAltFuture<IN, OUT> altFuture) {
        run(altFuture);

        return altFuture;
    }

    @Override
    @NonNull
    @NotCallOrigin
    public <IN> Runnable wrapActionWithErrorProtection(@NonNull final IAction<IN> action) {
        return new Runnable() {
            @Override
            @NotCallOrigin
            public void run() {
                try {
                    action.call();
                } catch (Exception e) {
                    ee(this, mOrigin, "run(IAction) problem", e);
                }
            }
        };
    }

    @Override
    @NonNull
    @NotCallOrigin
    public <IN> Runnable wrapActionWithErrorProtection(
            @NonNull final IAction<IN> action,
            @NonNull final IOnErrorAction onErrorAction) {
        return new Runnable() {
            @Override
            @NotCallOrigin
            public void run() {
                try {
                    action.call();
                } catch (Exception e) {
                    ee(this, mOrigin, "run(Runnable) problem", e);
                    try {
                        onErrorAction.call(e);
                    } catch (Exception e1) {
                        ee(this, mOrigin, "run(Runnable) problem " + e + " lead to another problem in onErrorAction", e1);
                    }
                }
            }
        };
    }

//========================== .subscribe() and .run() Methods ================================

    @Override // IThreadType
    @NotCallOrigin
    public <IN> void execute(@NonNull final IAction<IN> action) {
        run(wrapActionWithErrorProtection(action));
    }

    @Override // IThreadType
    @NotCallOrigin
    public <IN> void run(
            @NonNull final IAction<IN> action,
            @NonNull final IOnErrorAction onErrorAction) {
        run(wrapActionWithErrorProtection(action, onErrorAction));
    }

    @Override // IThreadType
    @NotCallOrigin
    public <IN> void runNext(@NonNull final IAction<IN> action) {
        runNext(wrapActionWithErrorProtection(action));
    }

    @Override // IThreadType
    @SuppressWarnings("unchecked")
    public boolean moveToHeadOfQueue(@NonNull final Runnable runnable) {
        //TODO Analyze if this non-atomic operation is a risk for closing a ThreadType and moving all pending actions to a new thread type as we would like to do for NET_READ when the available bandwdith changes

        if (mQueue instanceof Deque) {
            final boolean moved = mQueue.remove(runnable);

            if (moved) {
                ((Deque<Runnable>) mQueue).addFirst(runnable);
            }
            vv(this, mOrigin, "moveToHeadOfQueue() moved=" + moved);

            return moved;
        }

        return false; // The UI thread does not have a visible mQueue, and some queues choose not to support re-ordering
    }

    @Override // IThreadType
    @NotCallOrigin
    public <IN> void runNext(
            @NonNull final IAction<IN> action,
            @NonNull final IOnErrorAction onErrorAction) {
        vv(this, mOrigin, "runNext()");
        runNext(wrapActionWithErrorProtection(action, onErrorAction));
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <IN> IAltFuture<IN, IN> then(@NonNull final IAction<IN> action) {
        return runAltFuture(new AltFuture<IN, IN>(this, action));
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <IN> IAltFuture<IN, IN> then(@NonNull final IActionOne<IN> action) {
        return runAltFuture(new AltFuture<IN, IN>(this, action));
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <IN, OUT> IAltFuture<IN, OUT> map(@NonNull final IActionOneR<IN, OUT> action) {
        return runAltFuture(new AltFuture<>(this, action));
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <IN, OUT> IAltFuture<IN, OUT> then(@NonNull final IActionR<IN, OUT> action) {
        return runAltFuture(new AltFuture<>(this, action));
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <IN> IAltFuture<?, IN> value(@NonNull final IN value) {
        return new SettableAltFuture<>(this, value);
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <IN> IAltFuture<?, IN> value() {
        return new SettableAltFuture<>(this);
    }

    //======================= .subscribe() List Operations =========================================

    @Override // IThreadType
    @SafeVarargs
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public final <IN> List<IAltFuture<IN, IN>> then(@NonNull final IAction<IN>... actions) {
        final List<IAltFuture<IN, IN>> altFutures = new ArrayList<>(actions.length);

        vv(this, mOrigin, "map(List[" + actions.length + "])");
        for (final IAction<IN> action : actions) {
            altFutures.add(then(action));
        }

        return altFutures;
    }

    @Override // IThreadType
    @SafeVarargs
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public final <IN, OUT> List<IAltFuture<IN, OUT>> then(@NonNull final IActionR<IN, OUT>... actions) {
        vv(this, mOrigin, "map(List[" + actions.length + "])");
        final List<IAltFuture<IN, OUT>> altFutures = new ArrayList<>(actions.length);

        for (final IActionR<IN, OUT> action : actions) {
            altFutures.add(then(action));
        }

        return altFutures;
    }

    @Override // IThreadType
    @SafeVarargs
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public final <IN, OUT> List<IAltFuture<IN, OUT>> map(@NonNull final IActionOneR<IN, OUT>... actions) {
        final List<IAltFuture<IN, OUT>> altFutures = new ArrayList<>(actions.length);

        for (final IActionOneR<IN, OUT> action : actions) {
            altFutures.add(map(action));
        }

        return altFutures;
    }

    //TODO add mapEach(IActionOneR) value list to list
    //TODO add thenEach(IActionOne) value list

//=============================== Public Utility Methods ======================================

    //TODO public <A> AltFuture<A> flush()  - current thread type - wait for everything forked before this point and their side effects queued before other things to complete before next step on the specified threadtype

//    @Override // IThreadType
//    public <IN, OUT> void fork(@NonNull  final IRunnableAltFuture<IN, OUT> runnableAltFuture) {
//        assertTrue("Call runnableAltFuture().fork() instead. AbstractThreadType.fork() expected the IRunnableAltFuture should return isForked() and !isDone()", runnableAltFuture.isForked() && !runnableAltFuture.isDone());
//        if (Async.DEBUG && isMistakenlyCalledDirectlyFromOutsideTheCascadeLibrary()) {
//            throw new UnsupportedOperationException("Method for internal use only. Please call your IRunnableAltFuture " + runnableAltFuture + ".fork() on instead of calling IThreadType.fork(IRunnableAltFuture)");
//        }
//
//        run(runnableAltFuture); // Atomic state checks must be completed later in the .run() method
//    }

    @Override // IThreadType
    public boolean isShutdown() {
        return executorService.isShutdown();
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
        final ImmutableValue<String> origin = originAsync()
                .then(o -> {
                    i(this, "shutdown " + timeout + " mOrigin=" + o + " ThreadType creationOrigin=" + mOrigin.safeGet());
                    executorService.shutdown();
                });
        final FutureTask<Boolean> futureTask = new FutureTask<>(() -> {
            boolean terminated = timeout == 0;
            try {
                if (!terminated) {
                    terminated = executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Log.e(AbstractThreadType.class.getSimpleName(), "Could not shutdown. afterShutdownAction will not be called: " + origin, e);
                terminated = false;
            } catch (Exception e) {
                e(this, "Could not shutdown. afterShutdownAction will not be called: " + origin, e);
                terminated = false;
            } finally {
                if (terminated && afterShutdownAction != null) {
                    try {
                        afterShutdownAction.call();
                    } catch (Exception e) {
                        ee(this, mOrigin, "Problem during afterShutdownAction after successful workerExecutorService.shutdown: " + origin, e);
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
