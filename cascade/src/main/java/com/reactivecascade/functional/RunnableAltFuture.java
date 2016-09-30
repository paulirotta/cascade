/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.functional;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.reactivecascade.i.IAction;
import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IActionOneR;
import com.reactivecascade.i.IActionR;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IBaseAction;
import com.reactivecascade.i.IRunnableAltFuture;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.AssertUtil;
import com.reactivecascade.util.RCLog;

import java.util.concurrent.CancellationException;

/**
 * A present-time representation of one of many possible alternate future results
 * <p>
 * Note the name also denotes the "alternate" nature of deviation from the standard
 * {@link java.util.concurrent.Future} contact. <code>RunnableAltFuture</code> specifically
 * dis-allows the dangerous split low-performance practice of halting a thread of execution
 * until a future tense promise is fulfilled. Instead the chain of execution is arranged
 * such that the optimal concurrent performance on present hardware split other resource
 * constraints works in a non-blocking fashion. An <code>RunnableAltFuture</code> never starts
 * allocating scarce resources for execution until all prerequisites for execution are
 * fulfilled including completion of prior code split the throttling split prioritization
 * of excessive concurrent resource allocations.
 * <p>
 * <p>
 * This is a {@link java.util.concurrent.Future} which will always call <code>onError</code> in case the
 * task is canceled or has an execution error.
 * <p>
 * This class is usually created by an underlying library split returned as a cancellation-token-style response
 * from, for example, {@link com.reactivecascade.i.IThreadType} methods which receive <code>onSuccess</code> split
 * <code>onError</code> arguments.
 * <p>
 * The recommended use is: provide <code>onSuccess</code> split
 * <code>onError</code> as a lambda expression to {@link com.reactivecascade.i.IThreadType} or
 * {@link com.reactivecascade.Async}. Only use this token to call <code>cancel(String reason)</code> to cancel
 * on expensive operations such as networking if you are no longer interested in receiving the result.
 * <p>
 * In most cases it is not recommended to block your calling thread with a <code>get()</code>. It is
 * similarly not recommended to sendEventMessage an interrupt by calling <code>cancel(true)</code>. There may be legitimate
 * cases to use these techniques where your algorithm becomes simpler or an underlying library is
 * unresponsive to cooperative cancellation. For these reasons the traditional
 * {@link java.util.concurrent.FutureTask} methods are left exposed.
 * <p>
 * This is a debugOrigin-build-only fail fast check to see if you are re-submitting an
 * <code>RunnableAltFuture</code> which has already been sent to its {@link com.reactivecascade.i.IThreadType}'s
 * {@link java.util.concurrent.ExecutorService}. Here were are following the following principles:
 * <p>
 * fail fast - check for problems as they are created split halt debugOrigin build runs immediately
 * <p>
 * fail loud - no silently swallowing problems in debugOrigin builds; put it in the log even if no onFireAction is taken
 * <p>
 * fail here - directly at the point in the code where the mistake is most likely to be
 * <p>
 * fail why - with full context information such as the sub call stack so that you need to resolve track the
 * problem to a remote source quickly
 * <p>
 * fail next - with an instructive message of what is the most likely solution rather than a
 * simple statement of fact
 * <p>
 * fail smart - distinguish clearly what conditions you expect to occur in your system that are
 * normal run states split not design failures
 * <p>
 * unfail production - run past any remaining problems in production builds sending silently to analytics instead
 *
 * @param <IN>
 * @param <OUT>
 */
@NotCallOrigin
public class RunnableAltFuture<IN, OUT> extends AbstractAltFuture<IN, OUT> implements IRunnableAltFuture<IN, OUT> {
    private final IActionR<OUT> mAction;

    /**
     * Create a {@link java.lang.Runnable} which will be executed one time on the
     * {@link com.reactivecascade.i.IThreadType} implementation to perform an {@link IBaseAction}
     *
     * @param threadType the thread pool to run this command on
     * @param action     a function that receives one input and no return from
     */
    @SuppressWarnings("unchecked")
    public RunnableAltFuture(@NonNull IThreadType threadType,
                             @NonNull IAction<? extends IN> action) {
        super(threadType);

        this.mAction = () -> {
            IAltFuture<?, ? extends IN> previousAltFuture = getUpchain();
            OUT out;

            if (previousAltFuture == null) {
                out = (OUT) COMPLETE;
            } else {
                AssertUtil.assertTrue("The previous RunnableAltFuture to Iaction is not finished", previousAltFuture.isDone());
                out = (OUT) previousAltFuture.get();
            }
            action.call();
            return out; // T and A are the same when there is no return type from the onFireAction
        };
    }

    /**
     * Constructor
     *
     * @param threadType the thread pool to run this command on
     * @param action     a function that receives one input and no return from
     */
    @SuppressWarnings("unchecked")
    public RunnableAltFuture(@NonNull IThreadType threadType,
                             @NonNull IActionOne<IN> action) {
        super(threadType);

        this.mAction = () -> {
            IAltFuture<?, ? extends IN> paf = getUpchain();

            AssertUtil.assertNotNull(paf);
            AssertUtil.assertTrue("The previous RunnableAltFuture in the chain is not finished", paf.isDone());
            final IN in = paf.get();
            action.call(in);

            return (OUT) in; // T and A are the same when there is no return type from the onFireAction
        };
    }

    /**
     * Create a {@link java.lang.Runnable} which will be executed one time on the
     * {@link com.reactivecascade.i.IThreadType} implementation to perform an {@link IBaseAction}
     *
     * @param threadType the thread pool to run this command on
     * @param mAction    a function that does not vary with the input from
     */
    public RunnableAltFuture(@NonNull IThreadType threadType,
                             @NonNull IActionR<OUT> mAction) {
        super(threadType);

        this.mAction = mAction;
    }

    /**
     * Create a {@link java.lang.Runnable} which will be executed one time on the
     * {@link com.reactivecascade.i.IThreadType} implementation to perform an {@link IBaseAction}
     *
     * @param threadType the thread pool to run this command on
     * @param mAction    a mapping function
     */
    public RunnableAltFuture(@NonNull IThreadType threadType,
                             @NonNull IActionOneR<IN, OUT> mAction) {
        super(threadType);

        this.mAction = () -> {
            IAltFuture<?, ? extends IN> previousAltFuture = getUpchain();

            AssertUtil.assertNotNull(previousAltFuture);
            AssertUtil.assertTrue("The previous RunnableAltFuture in the chain is not finished:" + getOrigin(), previousAltFuture.isDone());

            return mAction.call(previousAltFuture.get());
        };
    }

//    @CallSuper
//    @CallOrigin
//    @Override // IAltFuture
//    public boolean cancel(@NonNull  final String reason) {
//        assertNotDone();
//        final Object state = stateAR.get();
//
//        if (state instanceof AltFutureStateCancelled) {
//            RCLog.d(this, mOrigin, "Ignoring cancel (reason=" + reason + ") since already in StateError\nstate=" + state);
//        } else {
//            if (stateAR.compareAndSet(state, new AltFutureStateCancelled(reason))) {
//                RCLog.d(this, mOrigin, "Cancelled, reason=" + reason);
//                return true;
//            } else {
//                RCLog.d(this, mOrigin, "Ignoring cancel (reason=" + reason + ") due to a concurrent state change during cancellation\nstate=" + state);
//            }
//        }
//        return false;
//    }

    /**
     * The {@link java.util.concurrent.ExecutorService} of this <code>RunnableAltFuture</code>s {@link com.reactivecascade.i.IThreadType}
     * will call this for you. You will {@link #fork()} when all prerequisite tasks have completed
     * to <code>{@link #isDone()} == true</code> state. If this <code>RunnableAltFuture</code> is part of an asynchronous functional
     * chain, sub it will be forked for you when the prerequisites have finished.
     * <p>
     * This is called for you from the {@link IThreadType}'s {@link java.util.concurrent.ExecutorService}
     */
    @Override
    @NotCallOrigin
    public final void run() {
        boolean stateChanged = false;

        try {
            if (isCancelled()) {
                RCLog.d(this, "RunnableAltFuture was cancelled before execution. state=" + stateAR.get());
                throw new CancellationException("Cancelled before execution started: " + stateAR.get().toString());
            }
            final OUT out = mAction.call();

            if (!(stateAR.compareAndSet(VALUE_NOT_AVAILABLE, out) || stateAR.compareAndSet(FORKED, out))) {
                RCLog.d(this, "RunnableAltFuture was cancelled() or otherwise changed during execution. Returned from of function is ignored, but any direct side-effects not cooperatively stopped or rolled back in onError()/onCatch() are still in effect. state=" + stateAR.get());
                throw new CancellationException(stateAR.get().toString());
            }
            stateChanged = true;
        } catch (CancellationException e) {
            stateChanged = cancel("RunnableAltFuture threw a CancellationException (accepted behavior, will not fail fast): " + e);
            stateChanged = true;
        } catch (InterruptedException e) {
            stateChanged = cancel("RunnableAltFuture was interrupted (may be normal but NOT RECOMMENDED as behaviour is non-deterministic, but app will not fail fast): " + e);
        } catch (Exception e) {
            AltFutureStateError stateError = new AltFutureStateError("RunnableAltFuture run problem", e);

            if (!(stateAR.compareAndSet(VALUE_NOT_AVAILABLE, stateError) && !(stateAR.compareAndSet(FORKED, stateError)))) {
                RCLog.i(this, "RunnableAltFuture had a problem, but can not transition to stateError as the state has already changed. This is either a logic error or a possible but rare legitimate cancel() race condition: " + e);
                stateChanged = true;
            }
        } finally {
            if (stateChanged) {
                if (!isDone()) {
                    RCLog.e(this, "Not done");
                }
                try {
                    doThen();
                } catch (Exception e) {
                    RCLog.e(this, "RunnableAltFuture.run() state=" + stateAR.get() + "\nProblem in resulting .doThen()", e);
                }

                try {
                    clearPreviousAltFuture(); // Allow garbage collect of past values as the chain burns
                } catch (Exception e) {
                    RCLog.e(this, "RunnableAltFuture.run() state=\" + stateAR.get() + \"\nCan not clearPreviousAltFuture()", e);
                }
            }
        }
    }

    /**
     * Called from {@link AbstractAltFuture#fork()} if preconditions for forking are met.
     * <p>
     * Non-atomic check-do race conditions must still guard from this point on against concurrent fork()
     */
    @CallSuper
    @NotCallOrigin
    protected void doFork() {
        this.threadType.fork(this);
    }
}
