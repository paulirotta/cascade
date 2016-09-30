/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.reactive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.reactivecascade.i.IAction;
import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IActionOneR;
import com.reactivecascade.i.IActionR;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IReactiveSource;
import com.reactivecascade.i.IReactiveTarget;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.Origin;
import com.reactivecascade.util.RCLog;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.reactivecascade.Async.UI;

/**
 * This is the default implementation for a reactive active chain link.
 * <p>
 * NOTE: Because there _may_ exist a possibility of multiple fire events racing each other on different
 * threads, it is important that the functions in the reactive chain are idempotent and stateless.
 * <p>
 * <code>Subscription</code>s are both an {@link IReactiveTarget} and
 * {@link IReactiveSource}.
 * <p>
 * <p>
 * TODO Add setFireEveryValue(true) option to queue up and fire all states one by one. If inOrderExecutor, this fire will be FIFO sequential, otherwise concurrent
 *
 * @param <OUT>
 * @param <IN>  the type of the second link in the active chain
 */
@NotCallOrigin
public class Subscription<IN, OUT> extends Origin implements IReactiveTarget<IN>, IReactiveSource<OUT> {
    //FIXME Replace these values with changing lastFireInIsFireNext to be volatile boolean needToQueue to simplify logic
    private static final Object FIRE_ACTION_NOT_QUEUED = new Object(); // A marker state for fireAction to indicate the need to queue on next fire

    @NonNull
    protected final IThreadType mThreadType;
    @NonNull
    protected final IActionOne<Exception> mOnError;
    @NonNull
    protected final CopyOnWriteArraySet<IReactiveTarget<OUT>> reactiveTargets = new CopyOnWriteArraySet<>(); // Holding a strong reference is optional, depending on the binding type
    @NonNull
    protected final IActionOneR<IN, OUT> mOnFireAction;
    @NonNull
    private final String name;
    @NonNull
    private final CopyOnWriteArraySet<IReactiveSource<IN>> reactiveSources = new CopyOnWriteArraySet<>();
    @NonNull
    private final AtomicReference<Object> latestFireInAR = new AtomicReference<>(FIRE_ACTION_NOT_QUEUED); // If is FIRE_ACTION_NOT_QUEUED, re-queue fireAction on next fire()
    @NonNull
    private final AtomicBoolean mLatestFireInIsFireNext = new AtomicBoolean(false); // Signals high priority re-execution if still processing the previous from
    @NonNull
    private final Runnable mFireRunnable;

    //TODO Use to unsubcribe from tail when IBindingContext is implemented
    @Nullable
    private final IReactiveSource<IN> upchainReactiveSource; // This is held to keep the chain from being garbage collected until the tail of the chain is de-referenced

    /**
     * Create a new default implementation of a reactive active chain link
     * <p>
     * If there are multiple down-chain targets attached to this node, it will concurrently fire
     * all down-chain branches.
     *  @param name                  the descriptive debug name of this subscription
     * @param threadType            the default thread group on which this subscription fires
     * @param upchainReactiveSource
     * @param onFireAction          Because there _may_ exist a possibility of multiple fire events racing each other on different
*                              threads, it is important that the mOnFireAction functions in the reactive chain are idempotent and stateless. Further analysis is needed, but be cautious.
     * @param onError
     */
    @SuppressWarnings("unchecked")
    public Subscription(@NonNull String name,
                        @Nullable IThreadType threadType, @Nullable IReactiveSource<IN> upchainReactiveSource,
                        @NonNull IActionOneR<IN, OUT> onFireAction,
                        @Nullable IActionOne<Exception> onError) {
        this.name = name;
        this.upchainReactiveSource = upchainReactiveSource;
        if (upchainReactiveSource != null) {
            upchainReactiveSource.sub(this);
        }
        this.mThreadType = threadType != null ? threadType : UI;
        this.mOnFireAction = onFireAction;
        this.mOnError = onError != null ? onError : e ->
                RCLog.e(this, "Problem firing subscription, name=" + getName(), e);

        /*
         * Singleton executor - there is only one which is never queued more than once at any time
         *
         * Fire using the most recently set from. Skip intermediate values when they arrive too
         * fast to process.
          *
          * Re-queue if the input from changes before exiting
         */
        mFireRunnable = this.mThreadType.wrapActionWithErrorProtection(new IAction<Object>() {
            @Override
            @NotCallOrigin
            public void call() throws Exception {
                Object latestValueFired = latestFireInAR.get();

                doReceiveFire((IN) latestValueFired); // This step may take some time
                if (!latestFireInAR.compareAndSet(latestValueFired, FIRE_ACTION_NOT_QUEUED)) {
                    if (mLatestFireInIsFireNext.getAndSet(true)) {
                        mThreadType.runNext(getFireRunnable()); // Input was set again while processing this from- re-queue to fire again after other pending work
                    } else {
                        mThreadType.run(getFireRunnable()); // Input was set again while processing this from- re-queue to fire again after other pending work
                    }
                }
            }
        });
    }

    private Runnable getFireRunnable() {
        return mFireRunnable;
    }

//================================= Public Utility Methods =======================================

    @Override // INamed
    @NonNull
    public String getName() {
        return this.name;
    }

    @Override // IReactiveTarget
    public void subSource(@NonNull String reason,
                          @NonNull IReactiveSource<IN> reactiveSource) {
        if (reactiveSources.add(reactiveSource)) {
            RCLog.v(this, reactiveSource.getName() + " subscribesSource, reason=" + reason);
        } else {
            RCLog.d(this, "Did you subSource several times or create some other mess? Upchain says hello, but we already have a hello from \"" + reactiveSource.getName() + "\" at \"" + getName() + "\"");
        }
    }

    @Override
    @NotCallOrigin
    public void unsubSource(@NonNull String reason,
                            @NonNull IReactiveSource<IN> reactiveSource) {
        if (reactiveSources.remove(reactiveSource)) {
            RCLog.v(this, "Upchain '" + reactiveSource.getName() + "' unsubSource, reason=" + reason);
        } else {
            RCLog.i(this, "Upchain '" + reactiveSource.getName() + "' unsubSource, reason=" + reason + "\nWARNING: This source is not current. Probably this is a garbage collection/weak reference side effect");
        }
    }

//================================= Internal Utility Methods =======================================

    /**
     * Do <code>mOnFireAction</code> to every downstream target that does not have an expired
     * {@link java.lang.ref.WeakReference}
     *
     * @param action something to do for each target- return <code>true</code> if this is the last action
     *               and the loop should abort early (loop is complete)
     * @return <code>true</code> if _any_ of the action(reactive_target) calls returns <code>true</code>
     * @throws Exception
     */
    private boolean forEachReactiveTarget(@NonNull IActionOneR<IReactiveTarget<OUT>, Boolean> action) throws Exception {
        boolean result = false;

        for (IReactiveTarget<OUT> target : reactiveTargets) {
            result |= action.call(target);
        }

        return result;
    }

    /**
     * Search downstream targets and do <code>actionIfFound</code> if you find the one specified
     *
     * @param searchItem    the target to be located in the subscribed targets list
     * @param actionIfFound an action to perform on that target if found
     * @return <code>true</code> if found and action performed
     * @throws Exception
     */
    protected final boolean searchReactiveTargets(@NonNull IReactiveTarget<OUT> searchItem,
                                                  @NonNull IActionOne<IReactiveTarget<OUT>> actionIfFound) throws Exception {
        return forEachReactiveTarget(reactiveTarget -> {
            final boolean equal = searchItem.equals(reactiveTarget);

            if (equal) {
                actionIfFound.call(reactiveTarget);
            }

            return equal;
        });
    }

// ================================ .fire() Actions =========================================

    @NotCallOrigin
    @Override // IReactiveTarget
    public void fire(@NonNull IN in) {
        RCLog.v(this, "fire latestFireInAR=" + in);
        mLatestFireInIsFireNext.set(false);
        /*
         There is a race at this point between latestFireInAR and mLatestFireInIsFireNext.
         By design, if the race is lost, map a normal fire will actually fire next. So we evaluate ahead of
         other pending actions- small loss, and not a problem as there is no dependency upset by this.

         This design is more efficient than the memory thrash at every reactive evaluation step that
         would explicitly atomically couple the signals into a new Pair(in, boolean) structure.
         */
        if (latestFireInAR.getAndSet(in) == FIRE_ACTION_NOT_QUEUED && in != IAltFuture.VALUE_NOT_AVAILABLE) {
            // Only queue for execution if not already queued
            mThreadType.run(getFireRunnable());
        }
    }

    @NotCallOrigin
    @Override // IReactiveTarget
    //TODO This looks a mess- can we clean up to eliminate this method entirely?
    public void fireNext(@NonNull IN in) {
        RCLog.v(this, "fireNext latestFireInAR=" + in);
        if (latestFireInAR.getAndSet(in) == FIRE_ACTION_NOT_QUEUED) {
            // Only queue for execution if not already queued
            mThreadType.runNext(mFireRunnable);
        } else {
            // Already queued for execution, but possibly not soon- push it to the top of the stack
            mThreadType.moveToHeadOfQueue(mFireRunnable);
        }
    }

    @NotCallOrigin
    private void doReceiveFire(@NonNull IN in) throws Exception {
        final OUT out = doAction(in);

        try {
            doDownchainActions(in, out);
        } catch (Exception e) {
            RCLog.e(this, "Can not doDownchainActions latestFireInAR=" + in, e);
        }
    }

    /**
     * Always called from the headFunctionalChainLink's refire sub. By default this executes on ThreadType.WORKER
     */
    @NotCallOrigin
    @NonNull
    private OUT doAction(@NonNull final IN in) throws Exception {
        RCLog.v(this, "doReceiveFire \"" + getName() + " from=" + in);
//        visualize(getName(), latestFireInAR.toString(), "AbstractBinding");

        return mOnFireAction.call(in);
    }

    /**
     * Always called from the headFunctionalChainLink's refire sub. By default this executes on Async.WORKER
     */
    @NotCallOrigin
    private void doDownchainActions(@NonNull IN in,
                                    @NonNull OUT out) throws Exception {
        forEachReactiveTarget(reactiveTarget -> {
            RCLog.v(this, "Fire down-chain reactive target " + reactiveTarget.getName() + ", from=" + out);
            reactiveTarget.fireNext(out);
            return false;
        });
        if (reactiveTargets.size() == 0) {
            RCLog.v(this, "Fire down-chain reactive targets, but there are zero targets for " + getName() + ", from=" + out);
        }
    }

//=================================.sub() Actions ==========================================

    @Override // IReactiveSource
    public boolean unsubscribe(@NonNull String reason,
                               @NonNull IReactiveTarget<OUT> reactiveTarget) {
        try {
            return searchReactiveTargets(reactiveTarget, wr -> {
                        RCLog.v(this, "unsubSource(IReactiveTarget) reason=" + reason + " reactiveTarget=" + reactiveTarget);
                        //TODO Annotate to remove the following warning. The action is safe due to AltWeakReference behavior
                        reactiveTargets.remove(reactiveTarget);
                    }
            );
        } catch (Exception e) {
            RCLog.e(this, "Can not remove IReactiveTarget reason=" + reason + " reactiveTarget=" + reactiveTarget, e);
            return false;
        }
    }

    @Override // IReactiveTarget
    public void unsubAllSources(@NonNull String reason) {
        RCLog.v(this, "Unsubscribing all sources, reason=" + reason);
        for (IReactiveSource<IN> source : reactiveSources) {
            source.unsubscribeAll(reason);
        }
    }

    @Override // IReactiveSource
    public void unsubscribeAll(@NonNull String reason) {
        RCLog.d(this, "unsubscribeAll() reason=" + reason);

        try {
            forEachReactiveTarget(reactiveTarget -> {
                unsubscribe(reason, reactiveTarget);
                //TODO Annotate to remove the following warning. The action is safe due to AltWeakReference behavior
                reactiveTargets.remove(reactiveTarget);
                return false;
            });
        } catch (Exception e) {
            RCLog.e(this, "Can not unsubscribeAll, reason=" + reason, e);
        }
    }

    @Override // IReactiveSource
    public IReactiveSource<OUT> split(@NonNull IReactiveTarget<OUT> reactiveTarget) {
        sub(reactiveTarget);

        return this;
    }

    //TODO revisit the use cases for a merge function latestFireInAR async (Not the same as RX zip)
//    @Override // IReactiveSource
//    public <UPCHAIN_OUT> IReactiveSource<OUT> merge(IReactiveSource<UPCHAIN_OUT> upchainReactiveSource) {
//        upchainReactiveSource.sub(this);
//
//        return this;
//    }

    @Override // IReactiveSource
    @NonNull
    public IReactiveSource<OUT> sub(@NonNull IAction<OUT> action) {
        return sub(mThreadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    public IReactiveSource<OUT> sub(@NonNull IThreadType threadType,
                                    @NonNull IAction<OUT> action) {
        return subMap(threadType, out -> {
            action.call();
            return out;
        });
    }

    @Override // IReactiveSource
    @NonNull
    public IReactiveSource<OUT> sub(@NonNull IActionOne<OUT> action) {
        return sub(mThreadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    public IReactiveSource<OUT> sub(
            @NonNull IThreadType threadType,
            @NonNull IActionOne<OUT> action) {
        return subMap(threadType, out -> {
            action.call(out);
            return out;
        });
    }

    @Override // IReactiveSource
    @NonNull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subMap(@NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        return subMap(mThreadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subMap(
            @NonNull IThreadType threadType,
            @NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        final IReactiveSource<DOWNCHAIN_OUT> subscription = new Subscription<>(getName(), threadType, this, action, mOnError);
        sub((IReactiveTarget<OUT>) subscription); //TODO Suspicious cast here

        return subscription;
    }

    @Override // IReactiveSource
    @NonNull
    public IReactiveSource<OUT> sub(@NonNull IReactiveTarget<OUT> reactiveTarget) {
        if (reactiveTargets.add(reactiveTarget)) {
            reactiveTarget.subSource("Reference to keep reactive chain from being garbage collected", this);
            RCLog.v(this, "Added WeakReference to down-chain IReactiveTarget \"" + reactiveTarget.getName());
        } else {
            RCLog.i(this, "IGNORED duplicate sub of down-chain IReactiveTarget \"" + reactiveTarget.getName());
        }

        return this;
    }

    @Override // IReactiveSource
    @NonNull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> sub(@NonNull IActionR<DOWNCHAIN_OUT> action) {
        return sub(mThreadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> sub(@NonNull IThreadType threadType,
                                                              @NonNull IActionR<DOWNCHAIN_OUT> action) {
        final IReactiveSource<DOWNCHAIN_OUT> subscription = new Subscription<>(
                getName(), threadType, this,
                t -> {
                    return action.call();
                },
                mOnError);
        sub((IReactiveTarget<OUT>) subscription);

        return subscription;
    }
}
