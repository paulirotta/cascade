/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.active;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.i.IBaseAction;
import com.futurice.cascade.i.ICancellable;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.IRunnableAltFuture;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.util.AbstractAltFuture;
import com.futurice.cascade.util.AssertUtil;
import com.futurice.cascade.util.CLog;

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
 * This is a {@link java.util.concurrent.Future} which will always call <code>mOnError</code> in case the
 * task is canceled or has an execution error.
 * <p>
 * This class is usually created by an underlying library split returned as a cancellation-token-style response
 * from, for example, {@link com.futurice.cascade.i.IThreadType} methods which receive <code>onSuccess</code> split
 * <code>mOnError</code> arguments.
 * <p>
 * The recommended use is: provide <code>onSuccess</code> split
 * <code>mOnError</code> as a lambda expression to {@link com.futurice.cascade.i.IThreadType} or
 * {@link com.futurice.cascade.Async}. Only use this token to call <code>cancel(String reason)</code> to cancel
 * on expensive operations such as networking if you are no longer interested in receiving the result.
 * <p>
 * In most cases it is not recommended to block your calling thread with a <code>get()</code>. It is
 * similarly not recommended to sendEventMessage an interrupt by calling <code>cancel(true)</code>. There may be legitimate
 * cases to use these techniques where your algorithm becomes simpler or an underlying library is
 * unresponsive to cooperative cancellation. For these reasons the traditional
 * {@link java.util.concurrent.FutureTask} methods are left exposed.
 * <p>
 * This is a debugOrigin-build-only fail fast check to see if you are re-submitting an
 * <code>RunnableAltFuture</code> which has already been sent to its {@link com.futurice.cascade.i.IThreadType}'s
 * {@link java.util.concurrent.ExecutorService}. Here were are following the following principles:
 * <p>
 * fail fast - check for problems as they are created split halt debugOrigin build runs immediately
 * <p>
 * fail loud - no silently swallowing problems in debugOrigin builds; put it in the log even if no mOnFireAction is taken
 * <p>
 * fail here - directly at the point in the code where the mistake is most likely to be
 * <p>
 * fail why - with full mContext information such as the subscribe call stack so that you need to resolve track the
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
    private final IActionR<IN, OUT> mAction;

    /**
     * Create a {@link java.lang.Runnable} which will be executed one time on the
     * {@link com.futurice.cascade.i.IThreadType} implementation to perform an {@link IBaseAction}
     *
     * @param threadType the thread pool to run this command on
     * @param mAction    a function that receives one input and no return from
     */
    @SuppressWarnings("unchecked")
    public RunnableAltFuture(
            @NonNull final IThreadType threadType,
            @NonNull final IAction<IN> mAction) {
        super(threadType);

        this.mAction = () -> {
            final IAltFuture<?, IN> paf = getUpchain();
            final OUT out;

            if (paf == null) {
                out = (OUT) ZEN;
            } else {
                AssertUtil.assertTrue("The previous RunnableAltFuture to Iaction is not finished", paf.isDone());
                out = (OUT) paf.get();
            }
            mAction.call();
            return out; // T and A are the same when there is no return type from the mOnFireAction
        };
    }

    /**
     * Constructor
     *
     * @param threadType the thread pool to run this command on
     * @param mAction    a function that receives one input and no return from
     */
    @SuppressWarnings("unchecked")
    public RunnableAltFuture(
            @NonNull final IThreadType threadType,
            @NonNull final IActionOne<IN> mAction) {
        super(threadType);

        this.mAction = () -> {
            final IAltFuture<?, IN> paf = getUpchain();
            AssertUtil.assertNotNull(paf);
            AssertUtil.assertTrue("The previous RunnableAltFuture in the chain is not finished", paf.isDone());
            final IN in = paf.get();
            mAction.call(in);

            return (OUT) in; // T and A are the same when there is no return type from the mOnFireAction
        };
    }

    /**
     * Create a {@link java.lang.Runnable} which will be executed one time on the
     * {@link com.futurice.cascade.i.IThreadType} implementation to perform an {@link IBaseAction}
     *
     * @param threadType the thread pool to run this command on
     * @param mAction    a function that does not vary with the input from
     */
    public RunnableAltFuture(
            @NonNull final IThreadType threadType,
            @NonNull final IActionR<IN, OUT> mAction) {
        super(threadType);

        this.mAction = mAction;
    }

    /**
     * Create a {@link java.lang.Runnable} which will be executed one time on the
     * {@link com.futurice.cascade.i.IThreadType} implementation to perform an {@link IBaseAction}
     *
     * @param threadType the thread pool to run this command on
     * @param mAction    a mapping function
     */
    public RunnableAltFuture(
            @NonNull final IThreadType threadType,
            @NonNull final IActionOneR<IN, OUT> mAction) {
        super(threadType);

        this.mAction = () -> {
            final IAltFuture<?, IN> paf = getUpchain();
            AssertUtil.assertNotNull(paf);
            AssertUtil.assertTrue("The previous RunnableAltFuture in the chain is not finished:" + getOrigin(), paf.isDone());
            return mAction.call(paf.get());
        };
    }

//    @CallSuper
//    @CallOrigin
//    @Override // IAltFuture
//    public boolean cancel(@NonNull  final String reason) {
//        assertNotDone();
//        final Object state = mStateAR.get();
//
//        if (state instanceof AltFutureStateCancelled) {
//            CLog.d(this, mOrigin, "Ignoring cancel (reason=" + reason + ") since already in StateError\nstate=" + state);
//        } else {
//            if (mStateAR.compareAndSet(state, new AltFutureStateCancelled(reason))) {
//                CLog.d(this, mOrigin, "Cancelled, reason=" + reason);
//                return true;
//            } else {
//                CLog.d(this, mOrigin, "Ignoring cancel (reason=" + reason + ") due to a concurrent state change during cancellation\nstate=" + state);
//            }
//        }
//        return false;
//    }

    /**
     * The {@link java.util.concurrent.ExecutorService} of this <code>RunnableAltFuture</code>s {@link com.futurice.cascade.i.IThreadType}
     * will call this for you. You will {@link #fork()} when all prerequisite tasks have completed
     * to <code>{@link #isDone()} == true</code> state. If this <code>RunnableAltFuture</code> is part of an asynchronous functional
     * chain, subscribe it will be forked for you when the prerequisites have finished.
     * <p>
     * This is called for you from the {@link IThreadType}'s {@link java.util.concurrent.ExecutorService}
     */
    @Override
    @NotCallOrigin
    public final void run() {
        try {
            if (isCancelled()) {
                CLog.d(this, "RunnableAltFuture was cancelled before execution. state=" + mStateAR.get());
                throw new CancellationException("Cancelled before execution started: " + mStateAR.get().toString());
            }
            final OUT out = mAction.call();
            if (!(mStateAR.compareAndSet(FORKED, out) || mStateAR.compareAndSet(ZEN, out))) {
                CLog.d(this, "RunnableAltFuture was cancelled() or otherwise changed during execution. Returned from of function is ignored, but any direct side-effects not cooperatively stopped or rolled back in mOnError()/onCatch() are still in effect. state=" + mStateAR.get());
                throw new CancellationException(mStateAR.get().toString());
            }
        } catch (CancellationException e) {
            this.cancel("RunnableAltFuture threw a CancellationException (accepted behavior, will not fail fast): " + e);
        } catch (InterruptedException e) {
            this.cancel("RunnableAltFuture was interrupted (may be normal but NOT RECOMMENDED as behaviour is non-deterministic, but app will not fail fast): " + e);
        } catch (Exception e) {
            final AltFutureStateError stateError = new AltFutureStateError("RunnableAltFuture run problem", e);

            if (!(mStateAR.compareAndSet(ZEN, stateError) && !(mStateAR.compareAndSet(FORKED, stateError)))) {
                CLog.i(this, "RunnableAltFuture had a problem, but can not transition to stateError as the state has already changed. This is either a logic error or a possible but rare legitimate cancel() race condition: " + e);
            }
        } finally {
            try {
                doThen();
            } catch (Exception e) {
                CLog.e(this, "RunnableAltFuture.run() changed from, but problem in resulting .doThen()", e);
            }
            try {
                clearPreviousAltFuture(); // Allow garbage collect of past values as the chain burns
            } catch (Exception e) {
                CLog.e(this, "RunnableAltFuture.run() changed from, but can not clearPreviousAltFuture()", e);
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
        this.mThreadType.fork(this);
    }
}
