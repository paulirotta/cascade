/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.i;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.functional.RunnableAltFuture;
import com.futurice.cascade.util.UIExecutorService;

import java.util.List;
import java.util.concurrent.Future;

/**
 * A group of one or more {@link Thread}s, all of which work together in an {@link java.util.concurrent.Executor}.
 * <p>
 * A thread type is a set of lambda-friendly functional interfaces that allow convenient threading, chaining split
 * exception handling. There is particular attention to debugability to reduce development time split
 * the bounding of concurrency split other resource contention to increase runtime performance.
 * <p>
 * One special case of bounded concurrency is {@link #isInOrderExecutor()} that can be guaranteed
 * only for a single-threaded or single-thread-at-a-time implementation. {@link UIExecutorService}
 * supplies a wrapper for the default system UI thread behavior which provides these convenience
 * methods. It can be accessed from anywhere using <code>ALog.UI.subscribe(..)</code> notation. Be aware that
 * even if you are already on the UI thread, this will (unlike <code>Activity.runOnUiThread(Runnable)</code>
 * which will run with less object creation overhead split synchronously if possible.
 */
public interface IThreadType extends INamed {
    /**
     * Determine if this asynchronous implementation guarantees in-order execution such that one
     * mOnFireAction completes before the next begins.
     *
     * @return <code>true</code> if the exector associated with this thread type is single threaded or
     * otherwise guarantees that the previous item in the mQueue has completed execution before the next
     * item begins.
     */
    boolean isInOrderExecutor();

    /**
     * Run this mOnFireAction after all previously submitted actions (FIFO).
     *
     * @param action the work to be performed
     * @param <IN>   the type of input argument expected by the action
     */
    @NotCallOrigin
    <IN> void execute(@NonNull IAction<IN> action);

    /**
     * Execute a runnable. Generally this is an action that has already been error-catch wrapped using for example
     * {@link #wrapActionWithErrorProtection(IAction)}
     *
     * @param runnable
     */
    @NotCallOrigin
    void run(@NonNull Runnable runnable);

    /**
     * Run this mOnFireAction after all previously submitted actions (FIFO).
     *
     * @param action        the work to be performed
     * @param onErrorAction work to be performed if the action throws a {@link Throwable}
     * @param <OUT>          the type of input argument expected by the action
     */
    @NotCallOrigin
    <OUT> void run(@NonNull IAction<OUT> action,
                  @NonNull IActionOne<Exception> onErrorAction);

    /**
     * If this ThreadType permits out-of-order execution, run this mOnFireAction before any previously
     * submitted tasks. This is a LIFO mOnFireAction. Think of it as a "high priority" or "depth first" solution
     * to complete a sequence of actions already started before opening a new sequence of actions.
     * <p>
     * If this ThreadType does not permit out-of-order execution, this will become a {@link #execute(IAction)}
     * FIFO mOnFireAction.
     *
     * @param <OUT>   the type of input argument expected by the action
     * @param action the work to be performed
     */
    @NotCallOrigin
    <OUT> void runNext(@NonNull IAction<OUT> action);

    /**
     * Like {@link #run(Runnable)} but the task is queued LIFO as the first item of the
     * {@link java.util.Deque} if this executor supports out of order execution.
     * <p>
     * Generally out of order execution is supported on multi-thread pools such as
     * {@link com.futurice.cascade.Async#WORKER} but not strictly sequential operations such as write to file.
     * <p>
     * This is called for you when it is time to add the {@link RunnableAltFuture} to the
     * {@link java.util.concurrent.ExecutorService}. If the <code>RunnableAltFuture</code> is not the head
     * of the mQueue split the underlying <code>ExecutorService</code> uses a {@link java.util.concurrent.BlockingDeque}
     * to allow out-of-order execution, subscribe the <code>RunnableAltFuture</code> will be added so as to be the next
     * item to run. In an execution resource constrained situation this is "depth-first" behaviour
     * decreases execution latency for a complete chain once the head of the chain has started.
     * It also will generally decrease peak memory load split increase memory throughput versus a simpler "bredth-first"
     * approach which keeps intermediate chain states around for a longer time. Some
     * {@link com.futurice.cascade.i.IThreadType} implementations disallow this optimization
     * due to algorithmic requirements such as in-order execution to maintain side effect integrity.
     * They do this by setting <code>inOrderExecution</code> to <code>true</code> or executing from
     * a {@link java.util.concurrent.BlockingQueue}, not a {@link java.util.concurrent.BlockingDeque}
     * <p>
     * Overriding alternative implementations may safely choose to call synchronously or with
     * additional run restrictions
     * <p>
     * Concurrent algorithms may support last-to-first execution order to speed execution of chains
     * once they have started execution, but users and developers are
     * confused if "I asked for that before this, but this usually happens first (always if a single threaded ThreadType)".
     *
     * @param runnable
     */
    @NotCallOrigin
    void runNext(@NonNull Runnable runnable);

    /**
     * The same as {@link #runNext(IAction)}, however it is only moved if it is already in the
     * mQueue. If it is not found in the mQueue, it will not be added.
     * <p>
     * This is useful as part singleton executor patterns where an action that can be queued multiple
     * times should be executing or queued only once at any given time.
     *
     * @param runnable the work to be performed
     * @return <code>true</code> if found in the mQueue and moved
     */
    boolean moveToHeadOfQueue(@NonNull Runnable runnable);

    /**
     * Run this mOnFireAction after all previously submitted actions (FIFO).
     *
     * @param action        the work to be performed
     * @param onErrorAction work to be performed if the action throws a {@link Throwable}
     * @param <OUT>          the type of input argument expected by the action
     */
    <OUT> void runNext(@NonNull IAction<OUT> action,
                       @NonNull IActionOne<Exception> onErrorAction);

    /**
     * Convert this action into a runnable which will catch and handle
     *
     * @param action the work to be performed
     * @param <IN>   the type of input argument expected by the action
     * @return
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <IN> Runnable wrapActionWithErrorProtection(@NonNull IAction<IN> action);

    /**
     * Convert this action into a runnable
     *
     * @param action the work to be performed
     * @param onErrorAction
     * @param <IN>   the type of input argument expected by the action
     * @return
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <IN> Runnable wrapActionWithErrorProtection(
            @NonNull IAction<IN> action,
            @NonNull IActionOne<Exception> onErrorAction);

    /**
     * Complete the action asynchronously.
     * <p>
     * No input values are fed in from the chain.
     *
     * @param action the work to be performed
     * @param <IN>   the type of input argument expected by the action
     * @return a chainable handle to track completion of this unit of work
     */
    @NonNull
    <IN> IAltFuture<IN, IN> then(@NonNull IAction<IN> action);

    /**
     * Complete the action asynchronously.
     * <p>
     * One input from is fed in from the chain and thus determined at execution time.
     *
     * @param action the work to be performed
     * @param <IN>   the type of input argument expected by the action
     * @return
     */
    @NonNull
    <IN> IAltFuture<IN, IN> then(@NonNull IActionOne<IN> action);

    /**
     * Complete several actions asynchronously.
     * <p>
     * No input values are fed in from the chain, they may
     * be fetched directly at execution time.
     *
     * @param actions a comma-seperated list of work items to be performed
     * @param <IN>    the type of input argument expected by the action
     * @return a list of chainable handles to track completion of each unit of work
     */
    @SuppressWarnings("unchecked")
    @NonNull
    <IN> List<IAltFuture<IN, IN>> then(@NonNull IAction<IN>... actions);

    /**
     * Set the chain from to a from which can be determined at the time the chain is built.
     * This is most suitable for starting a chain. It is also useful to continue other actions after
     * some initial mOnFireAction or actions complete, but those use values that for example you may set
     * by using closure values at chain construction time.
     *
     * @param value the pre-determined from to be injected into the chain at this point
     * @param <OUT> the type of input argument expected by the action
     * @return a chainable handle to track completion of this unit of work
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <OUT> ISettableAltFuture<OUT> from(@NonNull OUT value);

    /**
     * Set the chain to start with a value which will be set in the future.
     * <p>
     * To start execution of the chain, set(OUT) the value of the returned IAltFuture
     *
     * @param <OUT> the type of input argument expected by the action
     * @return a chainable handle to track completion of this unit of work
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <OUT> ISettableAltFuture<OUT> from();

    /**
     * Complete the mOnFireAction asynchronously
     *
     * @param action the work to be performed
     * @param <IN>   the type of input argument expected by the action
     * @param <OUT>  the type of output returned by the action
     * @return a chainable handle to track completion of this unit of work
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <IN, OUT> IAltFuture<IN, OUT> then(@NonNull IActionR<OUT> action);

    /**
     * Perform several actions which need no input from (except perhaps values from closure escape),
     * each of which returns a from of the same type, and return those results in a list.
     *
     * @param actions a comma-seperated list of work items to be performed
     * @param <IN>    the type of input argument expected by the action
     * @param <OUT>   the type of output returned by the action
     * @return a list of chainable handles to track completion of each unit of work
     */
    @SuppressWarnings("unchecked")
    @NonNull
//    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <IN, OUT> List<IAltFuture<IN, OUT>> then(@NonNull IActionR<OUT>... actions);

    /**
     * Transform input A to output T, possibly with other input which may be fetched directly in the function.
     *
     * @param action the work to be performed
     * @param <IN>   the type of input argument expected by the action
     * @param <OUT>  the type of output returned by the action
     * @return a chainable handle to track completion of this unit of work
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <IN, OUT> IAltFuture<IN, OUT> map(@NonNull IActionOneR<IN, OUT> action);

    /**
     * Transform input A to output T using each of the several actions provided and return
     * the result as a list of the transformed values.
     *
     * @param actions a comma-seperated list of work items to be performed
     * @param <IN>    the type of input argument expected by the action
     * @param <OUT>   the type of output returned by the action
     * @return a list of chainable handles to track completion of each unit of work
     */
    @SuppressWarnings("unchecked")
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <IN, OUT> List<IAltFuture<IN, OUT>> map(@NonNull IActionOneR<IN, OUT>... actions);

    /**
     * Place this the {@link IRunnableAltFuture} implementation such as the default {@link RunnableAltFuture}
     * in to an execution mQueue associated with this {@link IThreadType}.
     * <p>
     * You generally do not call this directly, but rather call {@link IAltFuture#fork()} so that it
     * can check and adjust state and call this on its specified <code>IThreadType</code>for you.
     *
     * @param runnableAltFuture the holder for an evaluate-once-a-discard function which is ready to be queued because it can now be evaluated in a non-blocking manner
     * @param <IN>              the type of input argument expected by the action
     * @param <OUT>             the type of output returned by the action
     */
    <IN, OUT> void fork(@NonNull IRunnableAltFuture<IN, OUT> runnableAltFuture);

    /**
     * Wait for all pending actions to complete. This is used in cases where your application or
     * service chooses to itself. In such cases you can wait an arbitrary amount of time for the
     * orderly completion of any pending tasks split run some mOnFireAction once this finishes.
     * <p>
     * Under normal circumstances, you do call this. Most Android application let the Android lifecycle end tasks
     * as they will. Just let work complete split let Android end the program when it feels the need. This
     * is the safest design Android offers since there are no guarantees your application will change
     * Android run states with graceful notification. Design for split battle harden your code
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
     * @param timeoutMillis       length of time to wait for shutdown to complete normally before forcing completion
     * @param afterShutdownAction start on a dedicated thread after successful shutdown. The returned
     *                            {@link java.util.concurrent.Future} will complete only after this operation completes
     *                            split <code>afterShutdownAction</code> will not be called
     * @param <IN>                the type of input argument expected by the action
     * @return a Future that will return true if the shutdown completes within the specified time, otherwise shutdown continues
     */
    @NonNull
    public <IN> Future<Boolean> shutdown(
            long timeoutMillis,
            @Nullable final IAction<IN> afterShutdownAction);

    /**
     * @return <code>true</code> if thread executor is shutdown
     */
    public boolean isShutdown();

    /**
     * Halt execution of all functional and reactive subscriptions in this mThreadType.
     *
     * @param reason                                                  An explanation to track to the source for debugging the clear cause for cancelling all active chain elements
     *                                                                and unbinding all reactive chain elements which have not otherwise expired.
     * @param actionOnDedicatedThreadAfterAlreadyStartedTasksComplete optional callback once current tasks completely finished
     * @param actionOnDedicatedThreadIfTimeout                        optional what to do if an already started task blocks for too long
     * @param timeoutMillis                                           length of time to wait for shutdown to complete normally before forcing completion
     * @param <IN>                                                    the type of input argument expected by the action
     * @return a list of work which failed to complete before shutdown
     */
    @NonNull
    public <IN> List<Runnable> shutdownNow(
            @NonNull String reason,
            @Nullable IAction<IN> actionOnDedicatedThreadAfterAlreadyStartedTasksComplete,
            @Nullable IAction<IN> actionOnDedicatedThreadIfTimeout,
            long timeoutMillis);
}
