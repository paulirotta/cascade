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

package com.futurice.cascade.i;

import android.support.annotation.NonNull;

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOneR;
import com.futurice.cascade.i.action.IActionR;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.i.functional.IAltFuture;
import com.futurice.cascade.i.functional.IRunnableAltFuture;

import java.util.List;

/**
 * A group of one or more {@link Thread}s, all of which work together in an {@link java.util.concurrent.Executor}.
 * <p>
 * A thread type is a set of lambda-friendly functional interfaces that allow convenient threading, chaining split
 * exception handling. There is particular attention to debugability to reduce development time split
 * the bounding of concurrency split other resource contention to increase runtime performance.
 * <p>
 * One special case of bounded concurrency is {@link #isInOrderExecutor()} that can be guaranteed
 * only for a single-threaded or single-thread-at-a-time implementation. {@link com.futurice.cascade.UIExecutorService}
 * supplies a wrapper for the default system UI thread behavior which provides these convenience
 * methods. It can be accessed from anywhere using <code>ALog.UI.subscribe(..)</code> notation. Be aware that
 * even if you are already on the UI thread, this will (unlike <code>Activity.runOnUiThread(Runnable)</code>
 * which will execute with less object creation overhead split synchronously if possible.
 */
public interface IThreadType extends INamed {
    /**
     * Determine if this asynchronous implementation guarantees in-order execution such that one
     * onFireAction completes before the next begins.
     *
     * @return <code>true</code> if the exector associated with this thread type is single threaded or
     * otherwise guarantees that the previous item in the queue has completed execution before the next
     * item begins.
     */
    boolean isInOrderExecutor();

    /**
     * Run this onFireAction after all previously submitted actions (FIFO).
     *
     * @param action the work to be performed
     * @param <IN>   the type of input argument expected by the action
     */
    <IN> void execute(IAction<IN> action);

    /**
     * Execute a runnable. Generally this is an action that has already been error-catch wrapped using for example
     * {@link #wrapRunnableAsErrorProtection(IAction)}
     *
     * @param runnable
     */
    void execute(Runnable runnable);

    /**
     * Run this onFireAction after all previously submitted actions (FIFO).
     *
     * @param action        the work to be performed
     * @param onErrorAction work to be performed if the action throws a {@link Throwable}
     * @param <IN>          the type of input argument expected by the action
     */
    <IN> void execute(IAction<IN> action, IOnErrorAction onErrorAction);

    /**
     * If this ThreadType permits out-of-order execution, execute this onFireAction before any previously
     * submitted tasks. This is a LIFO onFireAction. Think of it as a "high priority" or "depth first" solution
     * to complete a sequence of actions already started before opening a new sequence of actions.
     * <p>
     * If this ThreadType does not permit out-of-order execution, this will become a {@link #execute(IAction)}
     * FIFO onFireAction.
     *
     * @param <IN>   the type of input argument expected by the action
     * @param action the work to be performed
     */
    <IN> void executeNext(IAction<IN> action);

    /**
     * Like {@link #execute(Runnable)} but the task is queued LIFO as the first item of the
     * {@link java.util.Deque} if this executor supports out of order execution.
     *
     * Generally out of order execution is supported on multi-thread pools such as
     * {@link com.futurice.cascade.Async#WORKER} but not strictly sequential operations such as write to file.
     *
     * @param runnable
     */
    void executeNext(Runnable runnable);

    /**
     * The same as {@link #executeNext(IAction)}, however it is only moved if it is already in the
     * queue. If it is not found in the queue, it will not be added.
     * <p>
     * This is useful as part singleton executor patterns where an action that can be queued multiple
     * times should be executing or queued only once at any given time.
     *
     * @param runnable the work to be performed
     * @param <IN>     the type of input argument expected by the action
     * @return <code>true</code> if found in the queue and moved
     */
    <IN> boolean moveToHeadOfQueue(@NonNull Runnable runnable);

    /**
     * Run this onFireAction after all previously submitted actions (FIFO).
     *
     * @param action        the work to be performed
     * @param onErrorAction work to be performed if the action throws a {@link Throwable}
     * @param <IN>          the type of input argument expected by the action
     */
    <IN> void executeNext(IAction<IN> action, IOnErrorAction onErrorAction);

    /**
     * Convert this action into a runnable
     *
     * @param action
     * @param <IN>
     * @return
     */
    <IN> Runnable wrapRunnableAsErrorProtection(@NonNull IAction<IN> action);

    /**
     * Convert this action into a runnable
     *
     * @param action
     * @param onErrorAction
     * @param <IN>
     * @return
     */
    <IN> Runnable wrapRunnableAsErrorProtection(@NonNull IAction<IN> action, @NonNull IOnErrorAction onErrorAction);

    /**
     * Complete the onFireAction asynchronously.
     * <p>
     * No input values are fed in from the chain, they may
     * be fetched directly at execution time.
     *
     * @param action the work to be performed
     * @param <IN>   the type of input argument expected by the action
     * @return a chainable handle to track completion of this unit of work
     */
    <IN> IAltFuture<IN, IN> then(IAction<IN> action);

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
    <IN> List<IAltFuture<IN, IN>> then(IAction<IN>... actions);

    /**
     * Set the chain value to a value which can be determined at the time the chain is built.
     * This is most suitable for starting a chain. It is also useful to continue other actions after
     * some initial onFireAction or actions complete, but those use values that for example you may set
     * by using closure values at chain construction time.
     *
     * @param value the pre-determined value to be injected into the chain at this point
     * @param <IN>  the type of input argument expected by the action
     * @param <OUT> the type of output returned by the action
     * @return a chainable handle to track completion of this unit of work
     * @throws Exception
     */
    <IN, OUT> IAltFuture<IN, OUT> then(OUT value) throws Exception;

    /**
     * Set the chain to a value not yet determined, but which may be determined in a non-blocking
     * manner either before or at the time this point in the chain executes.
     *
     * @param value the value returned by a pre-determined function and then injected into the chain at this point
     * @param <IN>  the type of input argument expected by the action
     * @param <OUT> the type of output returned by the action
     * @return a chainable handle to track completion of this unit of work
     * @throws Exception
     */
    <IN, OUT> IAltFuture<IN, OUT> then(ImmutableValue<OUT> value) throws Exception;

    /**
     * Complete the onFireAction asynchronously
     *
     * @param action the work to be performed
     * @param <IN>   the type of input argument expected by the action
     * @param <OUT>  the type of output returned by the action
     * @return a chainable handle to track completion of this unit of work
     */
    <IN, OUT> IAltFuture<IN, OUT> then(IActionR<IN, OUT> action);

    /**
     * Perform several actions which need no input value (except perhaps values from closure escape),
     * each of which returns a value of the same type, and return those results in a list.
     *
     * @param actions a comma-seperated list of work items to be performed
     * @param <IN>    the type of input argument expected by the action
     * @param <OUT>   the type of output returned by the action
     * @return a list of chainable handles to track completion of each unit of work
     */
    <IN, OUT> List<IAltFuture<IN, OUT>> then(IActionR<IN, OUT>... actions);

    /**
     * Transform input A to output T, possibly with other input which may be fetched directly in the function.
     *
     * @param action the work to be performed
     * @param <IN>   the type of input argument expected by the action
     * @param <OUT>  the type of output returned by the action
     * @return a chainable handle to track completion of this unit of work
     */
    <IN, OUT> IAltFuture<IN, OUT> then(IActionOneR<IN, OUT> action);

    /**
     * Transform input A to output T using each of the several actions provided and return
     * the result as a list of the transformed values.
     *
     * @param actions a comma-seperated list of work items to be performed
     * @param <IN>    the type of input argument expected by the action
     * @param <OUT>   the type of output returned by the action
     * @return a list of chainable handles to track completion of each unit of work
     */
    <IN, OUT> List<IAltFuture<IN, OUT>> then(IActionOneR<IN, OUT>... actions);

    /**
     * Place this the {@link com.futurice.cascade.i.functional.IRunnableAltFuture} implementation such as the default {@link com.futurice.cascade.functional.AltFuture}
     * in to an execution queue associated with this {@link IThreadType}.
     * <p>
     * You generally do not call this directly, but rather call {@link com.futurice.cascade.i.functional.IAltFuture#fork()} so that it
     * can check and adjust state and call this on its specified <code>IThreadType</code>for you.
     *
     * @param runnableAltFuture the holder for an evaluate-once-a-discard function which is ready to be queued because it can now be evaluated in a non-blocking manner
     * @param <IN>              the type of input argument expected by the action
     * @param <OUT>             the type of output returned by the action
     */
    <IN, OUT> void fork(IRunnableAltFuture<IN, OUT> runnableAltFuture);

    /**
     * Halt execution of all functional and reactive subscriptions in this threadType.
     *
     * @param reason                                                  An explanation to track to the source for debugging the clear cause for cancelling all functional chain elements
     *                                                                and unbinding all reactive chain elements which have not otherwise expired.
     * @param actionOnDedicatedThreadAfterAlreadyStartedTasksComplete optional callback once current tasks completely finished
     * @param actionOnDedicatedThreadIfTimeout                        optional what to do if an already started task blocks for too long
     * @param timeoutMillis                                           length of time to wait for shutdown to complete normally before forcing completion
     * @param <IN>                                                    the type of input argument expected by the action
     * @return a list of work which failed to complete before shutdown
     */
    <IN> List<Runnable> shutdownNow(String reason, IAction<IN> actionOnDedicatedThreadAfterAlreadyStartedTasksComplete, IAction<IN> actionOnDedicatedThreadIfTimeout, long timeoutMillis);
}