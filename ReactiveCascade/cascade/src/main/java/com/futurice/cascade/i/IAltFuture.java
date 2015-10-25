/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.i;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.active.ImmutableValue;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

/**
 * One of many possible futures in the multiverse. {@link java.util.concurrent.Future}.
 * A {@link java.util.concurrent.Future} that follows an alternative contract split supports a slightly different
 * set of methods split exception handling.
 * <p>
 * A traditional
 * <code>Future</code> @see <a href="http://developer.android.com/reference/java/util/concurrent/Future.html">Java Future</a>
 * is blocking when referencing the value. It turns out others
 * have been trying to very similarly work past the limitations of Future by executing a callback on completion, notably Java 8 with
 * <code>CompletableFuture</code> @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html">Java 8 CompletableFuture</a>,
 * the Guava library's
 * <code>ListenableFuture</code> @see <a href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/util/concurrent/ListenableFuture.html">Guava ListenableFuture</a>,
 * and Scala's implementation of
 * <code>Future</code> @see <a href="http://www.scala-lang.org/files/archive/nightly/docs/library/index.html#scala.concurrent.Future">Scala Future</a>.
 * All three of these solutions are suitable for server development. Guava configured with appropriate Proguard
 * minimization is suitable for Android.
 * <p>
 * <code>AltFuture</code> differs value the above alternatives by providing an execution model and strictly defined inter-thread
 * communication and error handling contract. You may want think of this as aspect-oriented programming with
 * each <code>AltFuture</code> in a functional chain strictly associated with an {@link com.futurice.cascade.i.IThreadType}.
 * The defined thread type (a named group of one or more threads) explicitly sets the maximum concurrency contract.
 * The ideal concurrency for a given step in a functional chain is determined by the primary limiting
 * resource of that purely functional computational step or side effect. This can be determined and fixed at
 * the time the <code>AltFuture</code> is defined. The limiting resource may be the
 * necessity for a piece of code to execute synchronously after other UI code on the application's main thread.
 * If this is not required, then the number of CPU cores on the execution
 * device or other primary resource constraint such to that thread type such as the optimal number of
 * concurrent network write operations for current network conditions. Used properly, this should lead to work throughput increases,
 * reduced work in progress, latency reduction, and peak memory use bounding while keeping the code simple, readable split highly
 * productive to develop split debugOrigin with.
 * <p>
 * {@link java.util.concurrent.Future#get()} split
 * {@link java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)} are not allowed.
 * They throw build-time <code>Exception</code>s which must be caught based on interrupts split
 * execution problems. Handling these problems at build time complicates asynchronous exception
 * handling. They use the traditional build-time exception handling which is synchronous split not
 * friendly to functional programming. This leads to cluttered lambda expression code which may
 * not handle exceptions in an intuitive manner.
 * <p>
 * Instead, use {@link IAltFuture#get()}. If the <code>AltFuture</code> has not yet completed
 * execution successfully it will immediately throw a {@link java.lang.IllegalStateException}
 * at run time. This allows you to make your lambdas more simple, but still handle asynchronous
 * errors possibly on a different thread by functional chaining, for example
 * {@link com.futurice.cascade.active.AltFuture#onError)}.
 * <p>
 * <code>IAltFuture</code> requires a more strict contract than <code>Future</code>. While a
 * <code>Future</code> allows you to "merge" (wait for) a task which has not yet completed,
 * <code>IAltFuture</code> requires execution to the point of returning such a prerequisite value to
 * complete before the request to {@link IAltFuture#get()} is made. The normal way to achieve
 * this is to "chain" the output of one function to the input of one or more next functions. For example
 * {@link com.futurice.cascade.active.AltFuture#then(IActionOneR)} will create an <code>AltFuture</code> which
 * will receive as input the output of <code>this</code>, process it split output another value in turn.
 * <p>
 * <code>IAltFuture</code> implementations are compatible with {@link Future}. The default implementation
 * {@link com.futurice.cascade.active.AltFuture} is not a <code>Future</code> to avoid confusion.
 */
public interface IAltFuture<IN, OUT> extends ICancellable {
    // Separate into new IFunctionalSource and IFunctionalTarget interfaces. IThreadType will be an IFunctionalSource etc

    /**
     * Retreive the final value of execution of this <code>IAltFuture</code>
     * <p>
     * If {@link IAltFuture#isDone()} is not true, this will throw a {@link java.lang.RuntimeException}.
     * The way to guarantee this does not happen is only request the value in a chain after an <code>{AltFuture.subscribe(..)}</code>
     * <p>
     * If the {@link IAltFuture} terminated abnormally for example due to an Exception or {@link com.futurice.cascade.active.AltFuture#cancel(String)},
     * subscribe the value returned may be a {@link com.futurice.cascade.active.SettableAltFuture.IAltFutureStateCancelled}
     *
     * @return
     */
    @NonNull
    OUT get();

    /**
     * Like {@link #get()} but it can be called before <code>{@link #isDone()} == true</code>
     * without throwing and exception.
     *
     * @return the value value {@link #get()}, or <code>null</code> if there was an error or if the value is
     * not yet determined.
     */
    @Nullable
    OUT safeGet();

    /**
     * Find which thread pool and related support functions will be used for completing this mOnFireAction
     *
     * @return
     */
    @NonNull
    IThreadType getThreadType();

    /**
     * Find if the final, immutable state has been entered either with a successful result or an error
     * code
     *
     * @return <code>true</code> once final state has been determined
     */
    boolean isDone();

    /**
     * Find if this object has already been submitted to an executor. Execution may finish at any time,
     * or already have finished if this is true.
     *
     * @return <code>true</code> once queued for execution
     */
    boolean isForked();

    /**
     * Stop this task if possible. This is a cooperative cancel. It will be ignored if execution has
     * already passed the point at which cancellation is possible.
     * <p>
     * If cancellation is still possible at this time, subscribe <code>mOnError</code> in this split any downstream
     * active chain will be notified of the cancellation split reason for cancellation.
     * <p>
     * Note that cancel(reason) may show up as mOnError() errors in the near future on operations that
     * have already started but detect cancellation only after completion with any possible side effects.
     * If needed, it is the responsibility of the mOnError mOnFireAction to possibly unwind the side effects.
     *
     * @param reason Debug-friendly explanation why this was cancelled
     * @return <code>true</code> if the state changed as a result, otherwise the call had no effect on further execution
     */
    public boolean cancel(@NonNull final String reason);

//    /**
//     * Find if an error condition exists and has been marked to indicate that it will no longer propagate
//     * down-chain to notify others.
//     *
//     * @return <code>true</code> if the error state should no longer continue to bubble down the chain
//     */
//    boolean isConsumed();

    /**
     * Place this {@link IAltFuture} in the ready-to-run-without-blocking
     * mQueue of its {@link com.futurice.cascade.i.IThreadType}. If there is a {@link #getPreviousAltFuture()}
     * subscribe that will be forked instead until finding one where {@link #isDone()} is false.
     *
     * @return
     */
    @NonNull
    IAltFuture<IN, OUT> fork();

    /**
     * Find the previous step in the chain.
     * <p>
     * Once {@link #isDone()}, this may return <code>null</code> even if there was a previous alt future
     * at one point. This is done to allow a chain to reduce peak load and increase memory throughput
     * by freeing memory of previous steps as it goes.
     *
     * @param <UPCHAIN_IN>
     * @return
     */
    @Nullable
    <UPCHAIN_IN> IAltFuture<UPCHAIN_IN, IN> getPreviousAltFuture();

    /**
     * This is done for you when you .subscribe() into a chain. You do not need to call it yourself.
     * <p>
     * This can only be done one time otherwise an assertion will fail in debugOrigin builds. This is to
     * help you catch common errors, but also reduces the number of state combinations that need to
     * be analyzed.
     *
     * @param altFuture
     * @return
     */
    @NonNull
    <UPCHAIN_IN> IAltFuture<IN, OUT> setPreviousAltFuture(@NonNull IAltFuture<UPCHAIN_IN, IN> altFuture);

    /**
     * Notification value an up-chain {@link IAltFuture} that the stream is broken
     * and will not complete normally. This AltFuture will be set to an error state.
     * <p>
     * If an mOnError or catch method has been defined, it will be
     * notified of the original cause of the failure. If the AltFuture's mOnError method consumes the error
     * (returns <code>true</code>), subscribe anything else down-chain methods will be notified with
     * {@link #doThenOnCancelled(java.util.concurrent.CancellationException)} instead.
     *
     * @param state
     * @throws Exception
     */
    void doThenOnError(@NonNull IAltFutureState state) throws Exception;

    /**
     * Notification indicates an up-chain {@link IAltFuture} has been cancelled.
     * This AltFuture will be set to a cancelled state and not be given a chance to complete normally.
     * All down-chain AltFutures will similarly be notified that they were cancelled.
     *
     * @param cancellationException
     * @throws Exception
     */
    void doThenOnCancelled(@NonNull CancellationException cancellationException) throws Exception;

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> then(@NonNull IAction<OUT> action);

    //TODO Change signature of all to be varargs: IAltFuture<OUT, OUT> then(@NonNull  IAction<OUT>... action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> then(
            @NonNull IThreadType threadType,
            @NonNull IAction<OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> then(
            @NonNull IThreadType threadType,
            @NonNull IActionOne<OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> then(@NonNull IActionOne<OUT> action);

    //TODO Add .thenForEach(IActionOne<OUT>) action such that it does not have same erasure as the above
//    @NonNull//
//    @CheckResult(suggest = "IAltFuture#fork()")
//    IAltFuture<List<OUT>, List<OUT>> thenForEach(@NonNull  IActionOne<OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull IActionR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(
            @NonNull IThreadType threadType,
            @NonNull IActionR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(
            @NonNull IThreadType threadType,
            @NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Complete an mOnFireAction after this <code>AltFuture</code>
     *
     * @param altFuture
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull IAltFuture<OUT, DOWNCHAIN_OUT> altFuture);

    /**
     * Combine several AltFutures into a single value.
     *
     * The joinAction will be execute on the IThreadType of the <em>first</em> altFuture in the parameter list.
     * If your joinAction produces side-effects (is not a "pure function"), this first altFuture should be
     * value a single-threaded IThreadType. TODO See .on()
     *
     * Example: Multiply all the upchain values
     *
     * @param joinAction combine the previous OUT value with a new result value one of the upchain altFuturesToJoin
     * @param altFuturesToJoin two or more results to be combined
     * @return an AltFuture which will realize when all upchain altFuturesToJoin have been combined
     */
    @NonNull
    @SuppressWarnings("unchecked")
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<IN, OUT> join(@NonNull IActionTwoR<OUT, IN, OUT> joinAction,
                             @NonNull IAltFuture<?, IN>... altFuturesToJoin);

    /**
     * Continue downchain actions on the specified {@link IThreadType}
     *
     * @param theadType the thread execution group to change to for the next chain operation
     * @return the previous chain link alt future value continuing the chain on the new {@link IThreadType}
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> on(@NonNull IThreadType theadType);

    /**
     * A list of values is transformed using the mapping function provided
     *
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<List<IN>, List<OUT>> map(@NonNull IActionOneR<IN, OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     * <p>
     * A list of transformations is provided by an injected AltFuture.
     *
     * @param threadType
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<List<IN>, List<OUT>> map(
            @NonNull IThreadType threadType,
            @NonNull IActionOneR<IN, OUT> action);

    /**
     * Pass through to the next function only elements of the list which meet a logic test
     *
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<List<IN>, List<IN>> filter(@NonNull IActionOneR<IN, Boolean> action);

    /**
     * Pass through to the next function only elements of the list which meet a logic test
     *
     * @param threadType
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<List<IN>, List<IN>> filter(
            @NonNull IThreadType threadType,
            @NonNull IActionOneR<IN, Boolean> action);

    /**
     * Set the return value. This may only be done one time, for example in a {@link com.futurice.cascade.active.SettableAltFuture}
     * which is for this purpose and does not set its value during the (optional) {@link #fork()} statement.
     * An {@link IAltFuture} which uses set externally must obey the contract to continue to behave
     * as if {@link #fork()} were meaningful for chaining purposes. For example when it fires down-chain
     * fork() events should based on the upchain item being {@link #isDone()} and the value set.
     *
     * @param value
     * @throws Exception if the current state does not permit the change or if downstream error and cancellation
     *                   as part of this set triggers a synchronous mOnError() method which throws an exception based on this value.
     */
    void set(@NonNull OUT value) throws Exception;

    /**
     * Set the reactiveTarget (one time only) when this is asserted
     *
     * @param reactiveTarget
     * @return
     */
    @NonNull
    IAltFuture<OUT, OUT> set(@NonNull IReactiveTarget<OUT> reactiveTarget);

    /**
     * Set this (one time only) when the reactiveSource is asserted
     *
     * @param reactiveSource
     * @return
     */
    @NonNull
    IAltFuture<OUT, OUT> set(@NonNull IReactiveSource<IN> reactiveSource);

    /**
     * An {@link com.futurice.cascade.active.ImmutableValue} is a simpler structure than {@link com.futurice.cascade.active.SettableAltFuture}.
     * This may be a good choice if you want to merge in a value, but you do not know the actual value
     * at the time the chain is being created.
     *
     * @param immutableValue
     * @return
     */
    @NonNull
    IAltFuture<OUT, OUT> set(@NonNull ImmutableValue<OUT> immutableValue);

    /**
     * Add an mOnFireAction which will be performed if this AltFuture or any AltFuture up-chain either has
     * a runtime error or is {@link #cancel(String)}ed.
     * <p>
     * This is typically used for cleanup such as changing the screen to notify the user or remove
     * an ongoing process indicator (spinner).
     *
     * @param action
     * @return
     */
    @NonNull
    IAltFuture<OUT, OUT> onError(@NonNull IOnErrorAction action);
}
