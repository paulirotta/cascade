/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.util;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.futurice.cascade.Async;
import com.futurice.cascade.BuildConfig;
import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.functional.RunnableAltFuture;
import com.futurice.cascade.functional.SettableAltFuture;
import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.i.IRunnableAltFuture;
import com.futurice.cascade.i.ISettableAltFuture;
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

/**
 * The baseline implementation of ThreadType convenience classes. It provides functional interfaces
 * (lambda-friendly if you are using the RetroLambda library or similar) to execRunnable code in a background
 * WORKER thread pool.
 * <p>
 * For more specialized behaviour a class may choose to replace this.
 * <p>
 */
public abstract class AbstractThreadType extends Origin implements IThreadType {
    @NonNull
    protected final ExecutorService executorService;
    @NonNull
    protected final BlockingQueue<Runnable> mQueue;
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
    public AbstractThreadType(@NonNull String name,
                              @NonNull ExecutorService executorService,
                              @NonNull BlockingQueue<Runnable> queue) {
        this.name = name;
        this.executorService = executorService;
        this.mQueue = queue;
    }

//============================= Internal Utility Methods =========================================

    private static boolean isMistakenlyCalledDirectlyFromOutsideTheCascadeLibrary() {
        //TODO This check doesn't really allow 3rd party implementations. Not testing would mean unsafe/less obvious problems can come later. Package hiding would disallow replacement implementations that follow the interface contracts. What we have here is a half measure to guide people since currently there are no alternate implementations.
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        AssertUtil.assertTrue("Stack trace[3] is AbstractThreadType.fork(IRunnableAltFuture)", ste[3].getMethodName().contains("fork"));
        return !ste[4].getClassName().startsWith("com.futurice.cascade");
    }

    @NotCallOrigin
    public abstract void run(@NonNull Runnable runnable);

    private <IN, OUT> IAltFuture<IN, OUT> runAltFuture(@NonNull IRunnableAltFuture<IN, OUT> altFuture) {
        run(altFuture);

        return altFuture;
    }

    @Override
    @NonNull
    @NotCallOrigin
    public <IN> Runnable wrapActionWithErrorProtection(@NonNull IAction<IN> action) {
        return new Runnable() {
            @Override
            @NotCallOrigin
            public void run() {
                try {
                    action.call();
                } catch (Exception e) {
                    RCLog.e(this, "run(IAction) problem", e);
                }
            }
        };
    }

    @Override
    @NonNull
    @NotCallOrigin
    public <IN> Runnable wrapActionWithErrorProtection(@NonNull IAction<IN> action,
                                                       @NonNull IActionOne<Exception> onErrorAction) {
        return new Runnable() {
            @Override
            @NotCallOrigin
            public void run() {
                try {
                    action.call();
                } catch (Exception e) {
                    RCLog.e(this, "run(Runnable) problem", e);
                    try {
                        onErrorAction.call(e);
                    } catch (Exception e1) {
                        RCLog.e(this, "run(Runnable) problem " + e + " lead to another problem in onErrorAction", e1);
                    }
                }
            }
        };
    }

//========================== .subscribe() and .run() Methods ================================

    @Override // IThreadType
    @NotCallOrigin
    public <IN> void execute(@NonNull IAction<IN> action) {
        run(wrapActionWithErrorProtection(action));
    }

    @Override // IThreadType
    @NotCallOrigin
    public <IN> void run(@NonNull IAction<IN> action,
                         @NonNull IActionOne<Exception> onErrorAction) {
        run(wrapActionWithErrorProtection(action, onErrorAction));
    }

    @Override // IThreadType
    @NotCallOrigin
    public <IN> void runNext(@NonNull IAction<IN> action) {
        runNext(wrapActionWithErrorProtection(action));
    }

    @Override // IThreadType
    @SuppressWarnings("unchecked")
    public boolean moveToHeadOfQueue(@NonNull Runnable runnable) {
        //TODO Analyze if this non-atomic operation is a risk for closing a ThreadType and moving all pending actions to a new thread type as we would like to do for NET_READ when the available bandwdith changes

        if (mQueue instanceof Deque) {
            final boolean moved = mQueue.remove(runnable);

            if (moved) {
                ((Deque<Runnable>) mQueue).addFirst(runnable);
            }
            RCLog.v(this, "moveToHeadOfQueue() moved=" + moved);

            return moved;
        }

        return false; // The UI thread does not have a visible mQueue, and some queues choose not to support re-ordering
    }

    @Override // IThreadType
    @NotCallOrigin
    public <IN> void runNext(@NonNull IAction<IN> action,
                             @NonNull IActionOne<Exception> onErrorAction) {
        RCLog.v(this, "runNext()");
        runNext(wrapActionWithErrorProtection(action, onErrorAction));
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <IN> IAltFuture<IN, IN> then(@NonNull IAction<IN> action) {
        return runAltFuture(new RunnableAltFuture<>(this, action));
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <IN> IAltFuture<IN, IN> then(@NonNull IActionOne<IN> action) {
        return runAltFuture(new RunnableAltFuture<IN, IN>(this, action));
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <IN, OUT> IAltFuture<IN, OUT> map(@NonNull IActionOneR<IN, OUT> action) {
        return runAltFuture(new RunnableAltFuture<>(this, action));
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <IN, OUT> IAltFuture<IN, OUT> then(@NonNull IActionR<OUT> action) {
        return runAltFuture(new RunnableAltFuture<>(this, action));
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <OUT> ISettableAltFuture<OUT> from(@NonNull OUT value) {
        return new SettableAltFuture<>(this, value);
    }

    @Override // IThreadType
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <OUT> ISettableAltFuture<OUT> from() {
        return new SettableAltFuture<>(this);
    }

    //======================= .subscribe() List Operations =========================================

    @Override // IThreadType
    @SafeVarargs
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public final <IN> List<IAltFuture<IN, IN>> then(@NonNull IAction<IN>... actions) {
        List<IAltFuture<IN, IN>> altFutures = new ArrayList<>(actions.length);

        RCLog.v(this, "map(List[" + actions.length + "])");
        for (final IAction<IN> action : actions) {
            altFutures.add(then(action));
        }

        return altFutures;
    }

    @Override // IThreadType
    @SafeVarargs
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public final <IN, OUT> List<IAltFuture<IN, OUT>> then(@NonNull IActionR<OUT>... actions) {
        List<IAltFuture<IN, OUT>> altFutures = new ArrayList<>(actions.length);

        RCLog.v(this, "map(List[" + actions.length + "])");
        for (final IActionR<OUT> action : actions) {
            altFutures.add(then(action));
        }

        return altFutures;
    }

    @Override // IThreadType
    @SafeVarargs
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public final <IN, OUT> List<IAltFuture<IN, OUT>> map(@NonNull IActionOneR<IN, OUT>... actions) {
        final List<IAltFuture<IN, OUT>> altFutures = new ArrayList<>(actions.length);

        for (final IActionOneR<IN, OUT> action : actions) {
            altFutures.add(map(action));
        }

        return altFutures;
    }

    //TODO add mapEach(IActionOneR) from list to list
    //TODO add thenEach(IActionOne) from list

//=============================== Public Utility Methods ======================================

    //TODO public <A> RunnableAltFuture<A> flush()  - current thread type - wait for everything forked before this point and their side effects queued before other things to complete before next step on the specified threadtype

    @Override // IThreadType
    public <IN, OUT> void fork(@NonNull IRunnableAltFuture<IN, OUT> runnableAltFuture) {
        AssertUtil.assertTrue("Call runnableAltFuture().fork() instead. AbstractThreadType.fork() expected the IRunnableAltFuture should return isForked() and !isDone()", runnableAltFuture.isForked());
        if (BuildConfig.DEBUG) {
            if (isMistakenlyCalledDirectlyFromOutsideTheCascadeLibrary()) {
                throw new UnsupportedOperationException("Method for internal use only. Call " + runnableAltFuture + ".fork() instead. If you are implementing your own IAltFuture, do so in " + Async.class.getPackage());
            }
            if (runnableAltFuture.isDone()) {
                RCLog.v(this, "Warning: fork() called multiple times");
            }
        }

        run(runnableAltFuture); // Atomic state checks must be completed later in the .run() method
    }

    @Override // IThreadType
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override // IThreadType
    @NonNull
    public <IN> Future<Boolean> shutdown(long timeout,
                                         @Nullable IAction<IN> afterShutdownAction) {
        if (timeout < 1) {
            RCLog.throwIllegalArgumentException(this, "shutdown(" + timeout + ") is illegal, time must be > 0");
        }
        if (timeout == 0 && afterShutdownAction != null) {
            RCLog.throwIllegalArgumentException(this, "shutdown(0) is legal, but do not supply a afterShutdownAction() as it would run immediately which is probably an error");
        }
        final ImmutableValue<String> origin = RCLog.originAsync()
                .then(o -> {
                    RCLog.i(this, "shutdown " + timeout + " mOrigin=" + o + " ThreadType");
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
                RCLog.e(this, "Could not shutdown. afterShutdownAction will not be called: " + origin, e);
                terminated = false;
            } finally {
                if (terminated && afterShutdownAction != null) {
                    try {
                        afterShutdownAction.call();
                    } catch (Exception e) {
                        RCLog.e(this, "Problem during afterShutdownAction after successful workerExecutorService.shutdown: " + origin, e);
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
    public <IN> List<Runnable> shutdownNow(@NonNull String reason,
                                           @Nullable IAction<IN> actionOnDedicatedThreadAfterAlreadyStartedTasksComplete,
                                           @Nullable IAction<IN> actionOnDedicatedThreadIfTimeout,
                                           long timeoutMillis) {
        RCLog.i(this, "shutdownNow: reason=" + reason);
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

    @Override // Object
    public String toString() {
        return getName();
    }
}
