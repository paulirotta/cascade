/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.i;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.functional.RunnableAltFuture;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * One of many possible futures in the multiverse. {@link java.util.concurrent.Future}.
 * A {@link java.util.concurrent.Future} that follows an alternative contract split supports a slightly different
 * set of methods split exception handling.
 * <p>
 * A traditional
 * <code>Future</code> @see <a href="http://developer.android.com/reference/java/util/concurrent/Future.html">Java Future</a>
 * is blocking when referencing the from. It turns out others
 * have been trying to very similarly work past the limitations of Future by executing a callback on completion, notably Java 8 with
 * <code>CompletableFuture</code> @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html">Java 8 CompletableFuture</a>,
 * the Guava library's
 * <code>ListenableFuture</code> @see <a href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/util/concurrent/ListenableFuture.html">Guava ListenableFuture</a>,
 * and Scala's implementation of
 * <code>Future</code> @see <a href="http://www.scala-lang.org/files/archive/nightly/docs/library/index.html#scala.concurrent.Future">Scala Future</a>.
 * All three of these solutions are suitable for server development. Guava configured with appropriate Proguard
 * minimization is suitable for Android.
 * <p>
 * <code>RunnableAltFuture</code> differs from the above alternatives by providing an execution model and strictly defined inter-thread
 * communication and error handling contract. You may want think of this as aspect-oriented programming with
 * each <code>RunnableAltFuture</code> in a functional chain strictly associated with an {@link com.futurice.cascade.i.IThreadType}.
 * The defined thread type (a named group of one or more threads) explicitly sets the maximum concurrency contract.
 * The ideal concurrency for a given step in a functional chain is determined by the primary limiting
 * resource of that purely functional computational step or side effect. This can be determined and fixed at
 * the time the <code>IAltFuture</code> is defined. The limiting resource may be the
 * necessity for a piece of code to execute synchronously after other UI code on the application's main thread.
 * If this is not required, then the number of CPU cores on the execution
 * device or other primary resource constraint such to that thread type such as the optimal number of
 * concurrent network write operations for current network conditions. Used properly, this should lead to work throughput increases,
 * reduced work in progress, latency reduction, and peak memory use bounding while keeping the code simple and highly
 * productive.
 * <p>
 * Non-cooperative interrupts and blocking actions like
 * {@link java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)} are not allowed in this model.
 * These are replaced with an explicit dependency <code>.then()</code> action chain with simple asynchronous exception
 * handling.
 * <p>
 * Instead, use {@link IAltFuture#get()}. If the <code>IAltFuture</code> has not yet completed
 * execution successfully it will immediately throw a {@link java.lang.IllegalStateException}
 * at run time. This allows you to make your lambdas more simple, but still handle asynchronous
 * by functional chaining.
 * <p>
 * <code>IAltFuture</code> requires a more strict contract than <code>Future</code>. While a
 * <code>Future</code> allows you to "merge" (wait for) a task which has not yet completed,
 * <code>IAltFuture</code> requires execution to the point of returning such a prerequisite from to
 * complete before the request to {@link IAltFuture#get()} is made. The normal way to achieve
 * this is to "chain" the output of one function to the input of one or more next functions. For example
 * {@link IAltFuture#map(IActionOneR)} will create an <code>RunnableAltFuture</code> which
 * will receive as input the output of <code>this</code>, process it split output another from in turn.
 * <p>
 * <code>IAltFuture</code> implementations are compatible with {@link Future}. The default implementation
 * {@link RunnableAltFuture} is not a <code>Future</code> to avoid confusion.
 */
public interface IAltFuture<IN, OUT> extends ICancellable, ISafeGettable<OUT>, IAsyncOrigin {
    /**
     * A method which returns a new (unforked) <code>IAltFuture</code> should follow the naming conventiond <code>..Async</code>
     * and be annotated <code>@CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION) to {@link CheckResult} that
     * it is either stored or manually {@link #fork()}ed.
     * <p>
     * TODO This annotation is not ideal for disambiguating all cases, look at creating a new one
     */
    String CHECK_RESULT_SUGGESTION = "IAltFuture#fork()";

    /**
     * Find which thread pool and related support functions will be used for completing this mOnFireAction
     *
     * @return thread group
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

//    /**
//     * Find if an error condition exists and has been marked to indicate that it will no longer propagate
//     * down-chain to notify others.
//     *
//     * @return <code>true</code> if the error state should no longer continue to bubble down the chain
//     */
//    boolean isConsumed();

    /**
     * Place this {@link IAltFuture} in the ready-to-run-without-blocking
     * mQueue of its {@link com.futurice.cascade.i.IThreadType}. If there is a {@link #getUpchain()}
     * subscribe that will be forked instead until finding one where {@link #isDone()} is false.
     *
     * @return <code>this</code>, which is usually the <code>IAltFuture</code> which was actually forked.
     * The fork may be indirect and only after other items complete because there is a search upchain
     * (which may be a different branch spliced into the current chain) to find the first unforked from.
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
     * @return the previous {@link IAltFuture} in the chain, or <code>null</code> if this is currently
     * the head of the chain
     */
    @Nullable
    <UPCHAIN_IN> IAltFuture<UPCHAIN_IN, IN> getUpchain();

    /**
     * This is done for you when you add functions to a chain. You do not need to call it yourself.
     * <p>
     * The implementation may call this multiple times to support merging chains. This happens most
     * often when a method returns the mTail of a section of chain. The returned from in this case is
     * a new chain link stopping the merged chain section which might start burning before the merger
     * is created to not burn past the merger point until the primary chain reaches that point also.
     *
     * @param altFuture
     * @return <code>this</code>
     */
    @NonNull
    <UPCHAIN_IN> IAltFuture<IN, OUT> setUpchain(@NonNull IAltFuture<UPCHAIN_IN, IN> altFuture);

    /**
     * Notification from an up-chain {@link IAltFuture} that the stream is broken
     * and will not complete normally. This RunnableAltFuture will be set to an error state.
     * <p>
     * If an mOnError or catch method has been defined, it will be
     * notified of the original cause of the failure. If the RunnableAltFuture's mOnError method consumes the error
     * (returns <code>true</code>), subscribe anything else down-chain methods will be notified with
     * {@link #doOnCancelled(StateCancelled)} instead.
     *
     * @param stateError
     * @throws Exception
     */
    void doOnError(@NonNull StateError stateError) throws Exception;

    /**
     * Notification indicates an up-chain {@link IAltFuture} has been cancelled.
     * This RunnableAltFuture will be set to a cancelled state and not be given a chance to complete normally.
     * All down-chain AltFutures will similarly be notified that they were cancelled.
     *
     * @param stateCancelled
     * @throws Exception
     */
    void doOnCancelled(@NonNull StateCancelled stateCancelled) throws Exception;

    /**
     * Continue downchain actions on the specified {@link IThreadType}
     *
     * @param theadType the thread execution group to change to for the next chain operation
     * @return the previous chain link alt future from continuing the chain on the new {@link IThreadType}
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    IAltFuture<IN, OUT> on(@NonNull IThreadType theadType);

    /**
     * Execute the mOnFireAction after this <code>RunnableAltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    IAltFuture<IN, OUT> then(@NonNull IAction<OUT> action);

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    IAltFuture<IN, OUT> then(@NonNull IAction<OUT>... actions);

    /**
     * Execute the action after this <code>RunnableAltFuture</code> finishes.
     *
     * @param action
     * @return new {@link IAltFuture}
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    IAltFuture<IN, OUT> then(@NonNull IActionOne<OUT> action);

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    IAltFuture<IN, OUT> then(@NonNull IActionOne<OUT>... actions);

    /**
     * Execute the mOnFireAction after this <code>RunnableAltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull IActionR<DOWNCHAIN_OUT> action);

    /**
     * Complete an mOnFireAction after this <code>RunnableAltFuture</code>
     *
     * @param altFuture
     * @param <DOWNCHAIN_OUT>
     * @return altFuture
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull IAltFuture<OUT, DOWNCHAIN_OUT> altFuture);

    /**
     * Execute the mOnFireAction after this <code>RunnableAltFuture</code> finishes.
     *
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> map(@NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action);

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT>[] map(@NonNull IActionOneR<OUT, DOWNCHAIN_OUT>... actions);

    /**
     * Pause execution of this chain for a fixed time interval
     *
     * Note that the chain realizes immediately in the event of {@link #cancel(String)} or a runtime error
     *
     * @param sleepTime
     * @param timeUnit
     * @return
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    IAltFuture<IN, OUT> sleep(long sleepTime,
                              @NonNull TimeUnit timeUnit);

    /**
     * Continue to next step(s) in the chain only after the {@link IAltFuture} being waited for is complete. The from
     * is not propaged in the chain, but if there is an error that will be adopted into this chain.
     *
     * @param altFuture to pause this chain until it <code>{@link #isDone()}</code>
     * @return the upchain value of this chain, execution held until the other alt future completes
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    IAltFuture<IN, OUT> await(@NonNull IAltFuture<?, ?> altFuture);

    /**
     * Continue chain execution once all upchain futures realize and have completed their side effects.
     * <p>
     * The await operation will be cancelled or exception state and trigger {@link #onError(IOnErrorAction)}
     * if <em>any</em> of the joined operations have a problem running to completion.
     *
     * @param altFutures to pause this chain until they are done (<code>{@link #isDone()}</code>)
     * @return the upchain value of this chain, execution held until other alt futures complete
     */
    @NonNull
    @SuppressWarnings("unchecked")
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    IAltFuture<IN, OUT> await(@NonNull IAltFuture<?, ?>... altFutures);

    /**
     * Pass through to the next function if element meet a logic test, otherwise {@link #cancel(String)}
     *
     * @param action
     * @return
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    IAltFuture<IN, IN> filter(@NonNull IActionOneR<IN, Boolean> action);

    /**
     * Set the reactiveTarget (one time only) when this is asserted
     *
     * @param reactiveTarget
     * @return
     */
    @NonNull
    IAltFuture<IN, OUT> set(@NonNull IReactiveTarget<OUT> reactiveTarget);

    /**
     * Add an action which will be performed if this AltFuture or any AltFuture up-chain either has
     * a runtime error or is {@link #cancel(String)}ed.
     * <p>
     * This is typically a user notification or cleanup such as removing an ongoing process indicator (spinner).
     *
     * @param action
     * @return
     */
    @NonNull
    IAltFuture<IN, OUT> onError(@NonNull IActionOne<Exception> action);

    /**
     * Add an action which will be performed if this AltFuture or any AltFuture up-chain either has
     * a runtime error or is {@link #cancel(String)}ed.
     * <p>
     * This is typically used for cleanup such as changing the screen to notify the user or remove
     * an ongoing process indicator (spinner).
     *
     * @param action
     * @return
     */
    @NonNull
    IAltFuture<IN, OUT> onCancelled(@NonNull IActionOne<String> action);
}