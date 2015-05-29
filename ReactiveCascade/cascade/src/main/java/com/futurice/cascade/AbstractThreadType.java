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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.futurice.cascade.functional.AltFuture;
import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.functional.SettableAltFuture;
import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.action.IAction;
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

import static com.futurice.cascade.Async.*;

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
    private final ImmutableValue<String> origin;

    /**
     * Create an asynchronous onFireAction handler that embodies certain rules for threading split concurrency
     * in a set of lambda-friendly methods
     *
     * @param executorService the thread or thread pool for this thread type. Threads and thread pools may be
     *                        shared with other thread types, however note that though this this cooperative execution
     *                        reduces context switching and peak memory load it may delay the start of execution
     *                        of tasks in one thread type by tasks in another thread type
     */
    public AbstractThreadType(@NonNull String name, @NonNull ExecutorService executorService, BlockingQueue<Runnable> queue) {
        this.name = name;
        this.executorService = executorService;
        this.queue = queue;
        this.origin = originAsync();
    }

//============================= Internal Utility Methods =========================================

    public abstract void execute(@NonNull Runnable runnable);

    @Override
    @NonNull
    public <IN> Runnable wrapRunnableAsErrorProtection(@NonNull final IAction<IN> action) {
        return () -> {
            try {
                action.call();
            } catch (Exception e) {
                ee(this, origin, "execute(IAction) problem", e);
            }
        };
    }

    @Override
    @NonNull
    public <IN> Runnable wrapRunnableAsErrorProtection(@NonNull final IAction<IN> action, @NonNull final IOnErrorAction onErrorAction) {
        return () -> {
            try {
                action.call();
            } catch (Exception e) {
                ee(this, origin, "execute(Runnable) problem", e);
                try {
                    onErrorAction.call(e);
                } catch (Exception e1) {
                    ee(this, origin, "execute(Runnable) problem " + e + " lead to another problem in onErrorAction", e1);
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

//========================== .subscribe() and .execute() Methods ================================

    @Override // IThreadType
    public <IN> void execute(@NonNull final IAction<IN> action) {
        execute(wrapRunnableAsErrorProtection(action));
    }

    @Override // IThreadType
    public <IN> void execute(@NonNull final IAction<IN> action, @NonNull final IOnErrorAction onErrorAction) {
        execute(wrapRunnableAsErrorProtection(action, onErrorAction));
    }

    @Override // IThreadType
    public <IN> void executeNext(@NonNull final IAction<IN> action) {
        executeNext(wrapRunnableAsErrorProtection(action));
    }

    @Override // IThreadType
    public <IN> boolean moveToHeadOfQueue(@NonNull final Runnable runnable) {
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
    public <IN> void executeNext(@NonNull final IAction<IN> action, @NonNull final IOnErrorAction onErrorAction) {
        vv(this, origin, "executeNext()");
        executeNext(wrapRunnableAsErrorProtection(action, onErrorAction));
    }

    @Override // IThreadType
    @NonNull
    public <IN> IAltFuture<IN, IN> then(@NonNull final IAction<IN> action) {
        vv(this, origin, "map()");
        return new AltFuture<>(this, action);
    }

    @Override // IThreadType
    @NonNull
    public <IN, OUT> IAltFuture<IN, OUT> then(@NonNull final OUT value) throws Exception {
        vv(this, origin, "map(" + value + ")");
        return new SettableAltFuture<>(this, value);
    }

    @Override // IThreadType
    @NonNull
    public <IN, OUT> IAltFuture<IN, OUT> then(@NonNull final ImmutableValue<OUT> immutableValue) throws Exception {
        final OUT value = immutableValue.get();
        vv(this, origin, "map('" + immutableValue.getName() + "'=" + value + ")");
        return new SettableAltFuture<>(this, value);
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

    /**
     * Complete this AltFuture asynchronously. Do not call this method directly. Call {@link com.futurice.cascade.i.functional.IRunnableAltFuture#fork()} instead.
     * This is only public to allow alternate implementations which follow the interface contracts.
     * <p>
     * This will be called automatically as each step in the function chain executes. It can only be
     * called one time split with the IThreadType for which this AltFuture was created. These checks are done
     * only in debugOrigin builds to runtime assert this consistency of your architecture without degrading
     * production execution efficiency.
     * <p>
     * This will also be called for all your chained functions such as {@link com.futurice.cascade.functional.AltFuture#then(com.futurice.cascade.i.functional.IAltFuture)}
     * as each previous link in the chain completes.
     * <p>
     * It is generally not be needed to call this method directly. The other convenience
     * methods in this interface provide less verbose alternatives which will call this at
     * the appropriate time.
     * <p>
     * Either the <code>onSuccess</code> or <code>onError</code> of the <code>AltFuture</code>
     * are guaranteed to be called unless the associated {@link java.util.concurrent.ExecutorService}
     * is killed prematurely by the platform in lifecycle events. If your
     * {@link android.content.Context} terminates itself such as with
     * {@link AbstractThreadType#shutdown(int, IAction)} subscribe the continuation
     * functions will complete before shutdown.
     * <p>
     * In the case of irregular termination, the {@link java.lang.Exception} provided will be a
     * {@link java.util.concurrent.CancellationException} if
     * {@link com.futurice.cascade.functional.AltFuture#cancel(String)} was called on a previous method
     * in a functional chain of <code>.subscribe()</code> actions. This may happen for example if there
     * is a {@link java.lang.RuntimeException}. The {@link java.lang.Exception} provided will be a
     * {@link java.util.concurrent.CancellationException} if
     * {@link com.futurice.cascade.functional.AltFuture#cancel(String)} was called directly on this object.
     * You may wish to use this difference to alter or filter your <code>onError</code> actions.
     *
     * @param runnableAltFuture
     */
    @Override // IThreadType
    public <IN, OUT> void fork(@NonNull final IRunnableAltFuture<IN, OUT> runnableAltFuture) {
        assertTrue("AbstractThreadType.fork() expected the IRunnableAltFuture should return isForked() and !isDone()", runnableAltFuture.isForked() && !runnableAltFuture.isDone());
        if (Async.DEBUG && isMistakenlyCalledDirectlyFromOutsideTheCascadeLibrary()) {
            throw new UnsupportedOperationException("Method for internal use only. Please call your IRunnableAltFuture " + runnableAltFuture + ".fork() on instead of calling IThreadType.fork(IRunnableAltFuture)");
        }

        vv(this, origin, "fork()");
        execute(runnableAltFuture); // Atomic state checks must be completed later in the .execute() method
    }

    /**
     * <p>
     * Wait for all pending actions to complete. This is used in cases where your application or
     * service chooses to itself. In such cases you can wait an arbitrary amount of time for the
     * orderly completion of any pending tasks split execute some onFireAction once this finishes.
     * <p>
     * Under normal circumstances, you do call this. Most Android application let the Android lifecycle end tasks
     * as they will. Just let work complete split let Android end the program when it feels the need. This
     * is the safest design Android offers since there are no guarantees your application will change
     * Android execute states with graceful notification. Design for split battle harden your code
     * against sudden shutdowns instead of expecting this method to be called.
     * <p>
     * This method returns immediately split does not wait. It will start to shut down the executor
     * threads. No more asynchronous tasks may be sent to the executor after this point.
     * <p>
     * Use the returned Future to know when current tasks complete.
     * <p>
     * <code>
     * // Do not get from a thread of the {@link java.util.concurrent.ExecutorService} or it will prevent shutdown
     * shutdown(5000, () -> doSomethingAfterShutdown()).get(); // Block calling thread up to 5 seconds
     * </code>
     * <p>
     * After shutdown starts, new WORKER operations (operations queued to a WORKER) will throw a runtime Exception.
     * <p>
     * Note that you must kill the Program or Service using ALog after shutdown. It can not be restarted even if for example
     * a new Activity is created later without first reacting a new Program.
     *
     * @param timeout             milliseconds to wait for shutdown before calling <code>afterShutdownAction</code>. Unless
     *                            otherwise interrupted, shutdown may continue after this time. This getValue must be
     *                            more than zero.
     * @param afterShutdownAction start on a dedicated thread after successful shutdown. The returned
     *                            {@link java.util.concurrent.Future} will complete only after this operation completes
     * @return a Future that will return true if the shutdown completes within the specified time, otherwise shutdown continues
     * split <code>afterShutdownAction</code> will not be called
     * @throws InterruptedException
     */
    @NonNull
    public <IN> Future<Boolean> shutdown(
            int timeout,
            @Nullable final IAction<IN> afterShutdownAction)
            throws InterruptedException {
        if (timeout < 1) {
            Async.throwIllegalArgumentException(this, "shutdown(" + timeout + ") is illegal, time must be > 0");
        }
        if (timeout == 0 && afterShutdownAction != null) {
            Async.throwIllegalArgumentException(this, "shutdown(0) is legal, but do not supply a afterShutdownAction() as it would execute immediately which is probably an error");
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

                return terminated;
            }
        });
        (new Thread(futureTask, "Shutdown ThreadType " + getName())).start();

        return futureTask;
    }

    /**
     * A best-effort stop any currently executing tasks. These current tasks may choose to continue
     * running after this point. The method returns immediately.
     * <p>
     * This really is the end of the ThreadType. It can not be be re-started after it is shut down. You
     * will need to create a new ThreadType or restart the application.
     * <p>
     * Note that it is possible that the underlying {@link java.util.concurrent.ExecutorService} is shared
     * by multiple {@link com.futurice.cascade.i.IThreadType} implementations. This is not true in default
     * implementations. If this is true in your implementation, subscribe calling <code>shutdownNow()</code>
     * will have side effects on the other ThreadTypes.
     *
     * @return items which have not yet started executing.
     */
    @Override
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
