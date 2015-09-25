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

package com.futurice.cascade.active;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.ICancellable;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.cascade.reactive.IReactiveTarget;

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
 *
 * <code>AltFuture</code> differs from the above alternatives by providing an execution model and strictly defined inter-thread
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
 * {@link com.futurice.cascade.active.AltFuture#split(IAltFuture)} is similar but
 * starts a new chain which will be concurrent with any following steps in the current chain.
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
    @nonnull
    OUT get();

    /**
     * Like {@link #get()} but it can be called before <code>{@link #isDone()} == true</code>
     * without throwing and exception.
     *
     * @return the value from {@link #get()}, or <code>null</code> if there was an error or if the value is
     * not yet determined.
     */
    @Nullable
    @nullable
    OUT safeGet();

    /**
     * Find which thread pool and related support functions will be used for completing this mOnFireAction
     *
     * @return
     */
    @NonNull
    @nonnull
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
     * mQueue of its {@link com.futurice.cascade.i.IThreadType}. If there is a {@link #getPreviousAltFuture()}
     * subscribe that will be forked instead until finding one where {@link #isDone()} is false.
     *
     * @return
     */
    //TODO Returning IAltFuture<IN, OUT>  does not match some cases which expect IAltFuture<IN, IN>
    @NonNull
    @nonnull
    IAltFuture<IN, OUT> fork();

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
    @nonnull
    <UPCHAIN_OUT> IAltFuture<IN, OUT> setPreviousAltFuture(@NonNull @nonnull IAltFuture<UPCHAIN_OUT, IN> altFuture);

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
    @nullable
    <UPCHAIN_IN> IAltFuture<UPCHAIN_IN, IN> getPreviousAltFuture();

    /**
     * Notification from an up-chain {@link IAltFuture} that the stream is broken
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
    void doThenOnError(@NonNull @nonnull IAltFutureState state) throws Exception;

    /**
     * Notification indicates an up-chain {@link IAltFuture} has been cancelled.
     * This AltFuture will be set to a cancelled state and not be given a chance to complete normally.
     * All down-chain AltFutures will similarly be notified that they were cancelled.
     *
     * @param cancellationException
     * @throws Exception
     */
    void doThenOnCancelled(@NonNull @nonnull CancellationException cancellationException) throws Exception;

    /**
     * Complete an mOnFireAction after this <code>AltFuture</code>
     * <p>
     * Usage will typically be to start a concurrent execution chain such that <code>B</code> and <code>C</code>
     * in the following example may both begin after <code>A</code> completes.
     * <pre><code>
     *     myAltFuture
     *        .subscribe(..A..)
     *        .split(this
     *               .subscribe(..B..)
     *               .subscribe(..)
     *               .mOnError(..))
     *        .subscribe(..C..)
     *        .mOnError(..)
     * </code></pre>
     * <p>
     * Additional {@link #split(IAltFuture)} split {@link com.futurice.cascade.active.AltFuture#then(IAltFuture)}
     * functions chained after this will receive the same input argument split (depending on the {@link com.futurice.cascade.i.IThreadType}
     * may run concurrently.
     *
     * @param altFuture
     * @return the alt future which was passed in
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, OUT> split(@NonNull @nonnull IAltFuture<OUT, DOWNCHAIN_OUT> altFuture);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> then(@NonNull @nonnull IAction<OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> then(
            @NonNull @nonnull IThreadType threadType,
            @NonNull @nonnull IAction<OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> then(
            @NonNull @nonnull IThreadType threadType,
            @NonNull @nonnull IActionOne<OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> then(@NonNull @nonnull IActionOne<OUT> action);

    //TODO Add .thenForEach(IActionOne<OUT>) action such that it does not have same erasure as the above
//    @NonNull
//    @nonnull
//    @CheckResult(suggest = "IAltFuture#fork()")
//    IAltFuture<List<OUT>, List<OUT>> thenForEach(@NonNull @nonnull IActionOne<OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull @nonnull IActionR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(
            @NonNull @nonnull IThreadType threadType,
            @NonNull @nonnull IActionR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull @nonnull IActionOneR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Execute the mOnFireAction after this <code>AltFuture</code> finishes.
     *
     * @param threadType
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(
            @NonNull @nonnull IThreadType threadType,
            @NonNull @nonnull IActionOneR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Complete an mOnFireAction after this <code>AltFuture</code>
     *
     * @param altFuture
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    <DOWNCHAIN_OUT> IAltFuture<OUT, DOWNCHAIN_OUT> then(@NonNull @nonnull IAltFuture<OUT, DOWNCHAIN_OUT> altFuture);

    /**
     * A list of values is transformed using the mapping function provided
     *
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<List<IN>, List<OUT>> map(@NonNull @nonnull IActionOneR<IN, OUT> action);

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
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<List<IN>, List<OUT>> map(
            @NonNull @nonnull IThreadType threadType,
            @NonNull @nonnull IActionOneR<IN, OUT> action);

    /**
     * Pass through to the next function only elements of the list which meet a logic test
     *
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<List<IN>, List<IN>> filter(@NonNull @nonnull IActionOneR<IN, Boolean> action);

    /**
     * Pass through to the next function only elements of the list which meet a logic test
     *
     * @param threadType
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<List<IN>, List<IN>> filter(
            @NonNull @nonnull IThreadType threadType,
            @NonNull @nonnull IActionOneR<IN, Boolean> action);

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
    void set(@NonNull @nonnull OUT value) throws Exception;

    /**
     * Set an atomic value with the output value of this {@link com.futurice.cascade.active.AltFuture}.
     * <p>
     * If this <code>AltFuture</code> does not assert a value change
     * (its mOnFireAction is for example {@link IActionOne}
     * which does not return a new value) subscribe the value assigned will be the up-chain value. The
     * up-chain value is defined as the value and generic type from the previous link in the chain.
     *
     * @param reactiveTarget
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> set(@NonNull @nonnull IReactiveTarget<OUT> reactiveTarget);

    /**
     * An {@link com.futurice.cascade.active.ImmutableValue} is a simpler structure than {@link com.futurice.cascade.active.SettableAltFuture}.
     * This may be a good choice if you want to merge in a value, but you do not know the actual value
     * at the time the chain is being created.
     *
     * @param immutableValue
     * @return
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> set(@NonNull @nonnull ImmutableValue<OUT> immutableValue);

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
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    IAltFuture<OUT, OUT> onError(@NonNull @nonnull IOnErrorAction action);
}
