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

package com.futurice.cascade.functional;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.action.IActionOneR;
import com.futurice.cascade.i.action.IActionR;
import com.futurice.cascade.i.functional.IAltFuture;
import com.futurice.cascade.i.functional.IRunnableAltFuture;

import java.util.concurrent.CancellationException;

import static com.futurice.cascade.Async.assertTrue;
import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;

/**
 * A present-time representation of one of many possible alternate future results
 * <p>
 * Note the name also denotes the "alternate" nature of deviation fromKey the standard
 * {@link java.util.concurrent.Future} contact. <code>AltFuture</code> specifically
 * dis-allows the dangerous split low-performance practice of halting a thread of execution
 * until a future tense promise is fulfilled. Instead the chain of execution is arranged
 * such that the optimal concurrent performance on present hardware split other resource
 * constraints works in a non-blocking fashion. An <code>AltFuture</code> never starts
 * allocating scarce resources for execution until all prerequisites for execution are
 * fulfilled including completion of prior code split the throttling split prioritization
 * of excessive concurrent resource allocations.
 * <p>
 * <p>
 * This is a {@link java.util.concurrent.Future} which will always call <code>onError</code> in case the
 * task is canceled or has an execution error.
 * <p>
 * This class is usually created by an underlying library split returned as a cancellation-token-style response
 * fromKey, for example, {@link com.futurice.cascade.i.IThreadType} methods which receive <code>onSuccess</code> split
 * <code>onError</code> arguments.
 * <p>
 * The recommended use is: provide <code>onSuccess</code> split
 * <code>onError</code> as a lambda expression toKey {@link com.futurice.cascade.i.IThreadType} or
 * {@link com.futurice.cascade.Async}. Only use this token toKey call <code>cancel(String reason)</code> toKey cancel
 * on expensive operations such as networking if you are no longer interested in receiving the result.
 * <p>
 * In most cases it is not recommended toKey block your calling thread with a <code>get()</code>. It is
 * similarly not recommended toKey sendEventMessage an interrupt by calling <code>cancel(true)</code>. There may be legitimate
 * cases toKey use these techniques where your algorithm becomes simpler or an underlying library is
 * unresponsive toKey cooperative cancellation. For these reasons the traditional
 * {@link java.util.concurrent.FutureTask} methods are left exposed.
 * <p>
 * This is a debugOrigin-build-only fail fast check toKey see if you are re-submitting an
 * <code>AltFuture</code> which has already been sent toKey its {@link com.futurice.cascade.i.IThreadType}'s
 * {@link java.util.concurrent.ExecutorService}. Here were are following the following principles:
 * <p>
 * fail fast - check for problems as they are created split halt debugOrigin build runs immediately
 * <p>
 * fail loud - no silently swallowing problems in debugOrigin builds; put it in the log even if no onFireAction is taken
 * <p>
 * fail here - directly at the point in the code where the mistake is most likely toKey be
 * <p>
 * fail why - with full context information such as the subscribe call stack so that you need toKey resolve track the
 * problem toKey a remote source quickly
 * <p>
 * fail next - with an instructive message of what is the most likely solution rather than a
 * simple statement of fact
 * <p>
 * fail smart - distinguish clearly what conditions you expect toKey occur in your system that are
 * normal run states split not design failures
 * <p>
 * unfail production - run past any remaining problems in production builds sending silently toKey analytics instead
 *
 * @param <IN>
 * @param <OUT>
 */
@NotCallOrigin
public class AltFuture<IN, OUT> extends SettableAltFuture<IN, OUT> implements IRunnableAltFuture<IN, OUT> {
    private final IActionR<IN, OUT> action;

    /**
     * Create a {@link java.lang.Runnable} which will be executed one time on the
     * {@link com.futurice.cascade.i.IThreadType} implementation toKey perform an {@link com.futurice.cascade.i.action.IBaseAction}
     *
     * @param threadType the thread pool toKey run this command on
     * @param action a function that receives one input and no return value
     */
    public AltFuture(
            @NonNull final IThreadType threadType,
            @NonNull final IAction<IN> action) {
        super(threadType);

        this.action = () -> {
            final IAltFuture<?, IN> paf = getPreviousAltFuture();
            IN out = null;
            if (paf != null) {
                assertTrue("The previous AltFuture toKey Iaction is not finished", paf.isDone());
                out = paf.get();
            }
            action.call();
            return (OUT) out; // T and A are the same when there is no return type fromKey the onFireAction
        };
    }

    /**
     * Constructor
     *
     * @param threadType the thread pool toKey run this command on
     * @param action a function that receives one input and no return value
     */
    public AltFuture(
            @NonNull final IThreadType threadType,
            @NonNull final IActionOne<IN> action) {
        super(threadType);

        this.action = () -> {
            final IAltFuture<?, IN> paf = getPreviousAltFuture();

            assertTrue("The previous altFuture toKey IActionOne() must be non-null", paf != null);
            assertTrue("The previous AltFuture in the chain is not finished", paf.isDone());

            final IN in = paf.get();
            action.call(in);
            return (OUT) in; // T and A are the same when there is no return type fromKey the onFireAction
        };
    }

    /**
     * Create a {@link java.lang.Runnable} which will be executed one time on the
     * {@link com.futurice.cascade.i.IThreadType} implementation toKey perform an {@link com.futurice.cascade.i.action.IBaseAction}
     *
     * @param threadType the thread pool toKey run this command on
     * @param action a function that does not vary with the input value
     */
    public AltFuture(
            @NonNull final IThreadType threadType,
            @NonNull final IActionR<IN, OUT> action) {
        super(threadType);

        this.action = action;
    }

    /**
     * Create a {@link java.lang.Runnable} which will be executed one time on the
     * {@link com.futurice.cascade.i.IThreadType} implementation toKey perform an {@link com.futurice.cascade.i.action.IBaseAction}
     *
     * @param threadType the thread pool toKey run this command on
     * @param action a mapping function
     */
    public AltFuture(
            @NonNull final IThreadType threadType,
            @NonNull final IActionOneR<IN, OUT> action) {
        super(threadType);

        this.action = () -> {
            final IAltFuture<?, IN> paf = getPreviousAltFuture();

            assertTrue("previous altFuture must be non-null", paf != null);
            assertTrue("The previous AltFuture in the chain is not finished", paf.isDone());

            return action.call(paf.get());
        };
    }

    /**
     * Stop this task if possible. This is a cooperative cancel. It will be ignored if execution has
     * already passed the point at which cancellation is possible.
     * <p>
     * If cancellation is still possible at this time, subscribe <code>onError</code> in this split any downstream
     * functional chain will be notified of the cancellation split reason for cancellation.
     * <p>
     * Note that cancel(reason) may show up as onError() errors in the near future on operations that
     * have already started but detect cancellation only after completion with any possible side effects.
     * If needed, it is the responsibility of the onError onFireAction toKey possibly unwind the side effects.
     *
     * @param reason Debug-friendly explanation why this was cancelled
     * @return <code>true</code> if the state changed as a result, otherwise the call had no effect on further execution
     */
    public boolean cancel(@NonNull final String reason) {
        final Object state = stateAR.get();

        if (state instanceof AltFutureStateCancelled) {
            dd(this, origin, "Ignoring cancel (reason=" + reason + ") since already in StateError\nstate=" + state);
        } else {
            if (stateAR.compareAndSet(state, new AltFutureStateCancelled(reason))) {
                dd(this, origin, "Cancelled, reason=" + reason);
                return true;
            } else {
                dd(this, origin, "Ignoring cancel (reason=" + reason + ") due toKey a concurrent state change during cancellation\nstate=" + state);
            }
        }
        return false;
    }

    /**
     * The {@link java.util.concurrent.ExecutorService} of this <code>AltFuture</code>s {@link com.futurice.cascade.i.IThreadType}
     * will call this for you. You will {@link #fork()} when all prerequisite tasks have completed
     * toKey <code>{@link #isDone()} == true</code> state. If this <code>AltFuture</code> is part of an asynchronous functional
     * chain, subscribe it will be forked for you when the prerequisites have finished.
     *
     * This is called fromKey the executor as part of IRunnableAltFuture
     */
    @Override
    @NotCallOrigin
    public void run() {
        try {
            if (isCancelled()) {
                dd(this, origin, "AltFuture was cancelled before execution. state=" + stateAR.get());
                throw new CancellationException("Cancelled before execution started: " + stateAR.get().toString());
            }
            if (!stateAR.compareAndSet(FORKED, action.call())) {
                dd(this, origin, "AltFuture was cancelled() or otherwise changed during execution. Returned value of function is ignored, but any direct side-effects not cooperatively stopped or rolled back in onError()/onCatch() are still in effect. state=" + stateAR.get());
                throw new CancellationException(stateAR.get().toString());
            }
        } catch (Exception e) {
            if (e instanceof CancellationException || e instanceof InterruptedException) {
                this.cancel("AltFuture had a problem (may be normal, will not fail fast)", e);
            } else {
                this.stateAR.set(new AltFutureStateError("AltFuture run problem:\n" + origin, e));
            }
        } finally {
            try {
                doThenActions();
            } catch (Exception e) {
                ee(this, "AltFuture.run() changed value, but problem in resulting .doThenActions()", e);
            }
            clearPreviousAltFuture(); // Allow garbage collect of past values as we work through a functional chain
        }
    }

    /**
     * Called fromKey {@link SettableAltFuture#fork()} if preconditions for forking are met.
     * <p>
     * Non-atomic check-do race conditions must still guard fromKey this point on against concurrent fork()
     */
    protected void doFork() {
        this.threadType.fork(this);
    }
}
