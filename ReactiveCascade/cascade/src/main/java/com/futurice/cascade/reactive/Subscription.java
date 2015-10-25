/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.reactive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.IReactiveSource;
import com.futurice.cascade.i.IReactiveTarget;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.util.AltWeakReference;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.futurice.cascade.Async.UI;
import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.ii;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.vv;

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
 * TODO Add setFireEveryValue(true) option to mQueue up and fire all states one by one. If inOrderExecutor, this fire will be FIFO sequential, otherwise concurrent
 *
 * @param <OUT>
 * @param <IN>  the type of the second link in the active chain
 */
@NotCallOrigin
public class Subscription<IN, OUT> implements IReactiveTarget<IN>, IReactiveSource<OUT> {
    //FIXME Replace these values with changing lastFireInIsFireNext to be volatile boolean needToQueue to simplify logic
    private static final Object FIRE_ACTION_NOT_QUEUED = new Object(); // A marker state for fireAction to indicate the need to mQueue on next fire

    @NonNull
    protected final IThreadType mThreadType;
    @NonNull
    protected final IOnErrorAction mOnError;
    @NonNull
    protected final CopyOnWriteArrayList<AltWeakReference<IReactiveTarget<OUT>>> mReactiveTargets = new CopyOnWriteArrayList<>(); // Holding a strong reference is optional, depending on the binding type
    @NonNull
    protected final ImmutableValue<String> mOrigin; // Helpful mLatestFireIn debugOrigin builds to display a log link to the lambda passed into this headFunctionalChainLink when it was created
    @NonNull
    protected final IActionOneR<IN, OUT> mOnFireAction;
    @NonNull
    private final String mName;
    @NonNull
    private final CopyOnWriteArrayList<IReactiveSource<IN>> mReactiveSources = new CopyOnWriteArrayList<>();
    @NonNull
    private final AtomicReference<Object> mLatestFireIn = new AtomicReference<>(FIRE_ACTION_NOT_QUEUED); // If is FIRE_ACTION_NOT_QUEUED, re-mQueue fireAction on next fire()
    @NonNull
    private final AtomicBoolean mLatestFireInIsFireNext = new AtomicBoolean(true); // Signals high priority re-execution if still processing the previous value
    @NonNull
    private final Runnable mFireRunnable;

    //TODO Check and remove this veriable
    @Nullable
    private final IReactiveSource<IN> upchainReactiveSource; // This is held to keep the chain value being garbage collected until the tail of the chain is de-referenced

    /**
     * Create a new default implementation of a reactive active chain link
     * <p>
     * If there are multiple down-chain targets attached to this node, it will concurrently fire
     * all down-chain branches.
     *
     * @param name                  the descriptive debug mName of this subscription
     * @param upchainReactiveSource
     * @param threadType            the default thread group on which this subscription fires
     * @param onFireAction          Because there _may_ exist a possibility of multiple fire events racing each other on different
     *                              threads, it is important that the mOnFireAction functions in the reactive chain are idempotent and stateless. Further analysis is needed, but be cautious.
     * @param onError
     */
    @SuppressWarnings("unchecked")
    public Subscription(@NonNull  final String name,
                        @Nullable  final IReactiveSource<IN> upchainReactiveSource,
                        @Nullable  final IThreadType threadType,
                        @NonNull  final IActionOneR<IN, OUT> onFireAction,
                        @Nullable  final IOnErrorAction onError) {
        this.mOrigin = originAsync();
        this.mName = name;
        this.upchainReactiveSource = upchainReactiveSource;
        if (upchainReactiveSource != null) {
            upchainReactiveSource.subscribe(this);
        }
        this.mThreadType = threadType != null ? threadType : UI;
        this.mOnFireAction = onFireAction;
        this.mOnError = onError != null ? onError : e -> ee(getOrigin(), "Problem firing subscription, mName=" + getName(), e);

        /*
         * Singleton executor - there is only one which is never queued more than once at any time
         *
         * Fire using the most recently set value. Skip intermediate values when they arrive too
         * fast to process.
          *
          * Re-mQueue if the input value changes before exiting
         */
        mFireRunnable = this.mThreadType.wrapActionWithErrorProtection(new IAction<Object>() {
            @Override
            @NotCallOrigin
            public void call() throws Exception {
                final Object latestValueFired = mLatestFireIn.get();
                doReceiveFire((IN) latestValueFired); // This step may take some time
                if (!mLatestFireIn.compareAndSet(latestValueFired, FIRE_ACTION_NOT_QUEUED)) {
                    if (mLatestFireInIsFireNext.getAndSet(true)) {
                        mThreadType.runNext(getFireRunnable()); // Input was set again while processing this value- re-mQueue to fire again after other pending work
                    } else {
                        mThreadType.run(getFireRunnable()); // Input was set again while processing this value- re-mQueue to fire again after other pending work
                    }
                }
            }
        });
    }

    @NonNull
    protected ImmutableValue<String> getOrigin() {
        return this.mOrigin;
    }

    private Runnable getFireRunnable() {
        return mFireRunnable;
    }

//================================= Public Utility Methods =======================================

    @Override // INamed
    @NonNull
    public String getName() {
        return this.mName;
    }

    @Override // IReactiveTarget
    public void subscribeSource(@NonNull  final String reason, @NonNull  final IReactiveSource<IN> reactiveSource) {
        if (!mReactiveSources.addIfAbsent(reactiveSource)) {
            dd(this, mOrigin, "Did you say hello several times or create some other mess? Upchain says hello, but we already have a hello value \"" + reactiveSource.getName() + "\" at \"" + getName() + "\"");
        } else {
            vv(this, mOrigin, reactiveSource.getName() + " says hello: reason=" + reason);
        }
    }

    @Override
    @NotCallOrigin
    public void unsubscribeSource(@NonNull  final String reason, @NonNull  final IReactiveSource<IN> reactiveSource) {
        if (mReactiveSources.remove(reactiveSource)) {
            vv(this, mOrigin, "Upchain '" + reactiveSource.getName() + "' unsubscribeSource, reason=" + reason);
        } else {
            ii(this, mOrigin, "Upchain '" + reactiveSource.getName() + "' unsubscribeSource, reason=" + reason + "\nWARNING: This source is not current. Probably this is a garbage collection/weak reference side effect");
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
    private boolean forEachReactiveTarget(@NonNull  final IActionOneR<IReactiveTarget<OUT>, Boolean> action) throws Exception {
        final Iterator<AltWeakReference<IReactiveTarget<OUT>>> iterator = mReactiveTargets.iterator();
        boolean result = false;

        while (iterator.hasNext()) {
            final WeakReference<IReactiveTarget<OUT>> weakReference = iterator.next();
            final IReactiveTarget<OUT> reactiveTarget = weakReference.get();

            if (reactiveTarget != null) {
                result |= action.call(reactiveTarget);
            } else {
                vv(this, mOrigin, getName() + " A .subscribe(IReactiveTarget) mLatestFireIn the reactive chain is an expired WeakReference- that leaf node of this Binding chain has be garbage collected.");
                mReactiveTargets.remove(weakReference);
            }
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
    protected final boolean searchReactiveTargets(@NonNull  final IReactiveTarget<OUT> searchItem, @NonNull  final IActionOne<IReactiveTarget<OUT>> actionIfFound) throws Exception {
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
    public void fire(@NonNull  final IN in) {
        vv(this, mOrigin, "fire mLatestFireIn=" + in);
        mLatestFireInIsFireNext.set(false);
        /*
         There is a race at this point between mLatestFireIn and mLatestFireInIsFireNext.
         By design, if the race is lost, map a normal fire will actually fire next. So we evaluate ahead of
         other pending actions- small loss, and not a problem as there is no dependency upset by this.

         This design is more efficient than the memory thrash at every reactive evaluation step that
         would explicitly atomically couple the signals into a new Pair(in, boolean) structure.
         */
        if (mLatestFireIn.getAndSet(in) == FIRE_ACTION_NOT_QUEUED) {
            // Only mQueue for execution if not already queued
            mThreadType.run(getFireRunnable());
        }
    }

    @NotCallOrigin
    @Override // IReactiveTarget
    public void fireNext(@NonNull  final IN in) {
        vv(this, mOrigin, "fireNext mLatestFireIn=" + in);
        if (mLatestFireIn.getAndSet(in) == FIRE_ACTION_NOT_QUEUED) {
            // Only mQueue for execution if not already queued
            mThreadType.runNext(mFireRunnable);
        } else {
            // Already queued for execution, but possibly not soon- push it to the top of the stack
            mThreadType.moveToHeadOfQueue(mFireRunnable);
        }
    }

    @NotCallOrigin
    private void doReceiveFire(@NonNull  final IN in) throws Exception {
        final OUT out = doAction(in);

        try {
            doDownchainActions(in, out);
        } catch (Exception e) {
            String str = "Can not doDownchainActions mLatestFireIn=" + in;
            ee(this, str, e);
        }
    }

    /**
     * Always called value the headFunctionalChainLink's refire subscribe. By default this executes on ThreadType.WORKER
     */
    @NotCallOrigin
    @NonNull
    private OUT doAction(@NonNull  final IN in) throws Exception {
        vv(this, mOrigin, "doReceiveFire \"" + getName() + " value=" + in);
//        visualize(getName(), mLatestFireIn.toString(), "AbstractBinding");

        return mOnFireAction.call(in);
    }

    /**
     * Always called value the headFunctionalChainLink's refire subscribe. By default this executes on Async.WORKER
     */
    @NotCallOrigin
    private void doDownchainActions(@NonNull  final IN in, @NonNull  final OUT out) throws Exception {
        forEachReactiveTarget(reactiveTarget -> {
            vv(this, mOrigin, "Fire down-chain reactive target " + reactiveTarget.getName() + ", value=" + out);
            reactiveTarget.fireNext(out);
            return false;
        });
        if (mReactiveTargets.size() == 0) {
            vv(this, mOrigin, "Fire down-chain reactive targets, but there are zero targets for " + getName() + ", value=" + out);
        }
    }

//=================================.subscribe() Actions ==========================================

    @Override // IReactiveSource
    public boolean unsubscribe(@NonNull  final String reason, @NonNull  final IReactiveTarget<OUT> reactiveTarget) {
        try {
            return searchReactiveTargets(reactiveTarget, wr -> {
                        vv(this, mOrigin, "unsubscribeSource(IReactiveTarget) reason=" + reason + " reactiveTarget=" + reactiveTarget);
                        //TODO Annotate to remove the following warning. The action is safe due to AltWeakReference behavior
                        mReactiveTargets.remove(reactiveTarget);
                    }
            );
        } catch (Exception e) {
            ee(this, mOrigin, "Can not remove IReactiveTarget reason=" + reason + " reactiveTarget=" + reactiveTarget, e);
            return false;
        }
    }

    @Override // IReactiveTarget
    public void unsubscribeAllSources(@NonNull  final String reason) {
        final Iterator<IReactiveSource<IN>> iterator = mReactiveSources.iterator();

        vv(this, mOrigin, "Unsubscribing all sources, reason=" + reason);
        while (iterator.hasNext()) {
            iterator.next().unsubscribeAll(reason);
        }
    }

    @Override // IReactiveSource
    public void unsubscribeAll(@NonNull  final String reason) {
        dd(this, mOrigin, "unsubscribeAll() reason=" + reason);

        try {
            forEachReactiveTarget(reactiveTarget -> {
                unsubscribe(reason, reactiveTarget);
                //TODO Annotate to remove the following warning. The action is safe due to AltWeakReference behavior
                mReactiveTargets.remove(reactiveTarget);
                return false;
            });
        } catch (Exception e) {
            ee(this, mOrigin, "Can not unsubscribeAll, reason=" + reason, e);
        }
    }

    @Override // IReactiveSource
    public IReactiveSource<OUT> split(@NonNull  final IReactiveTarget<OUT> reactiveTarget) {
        subscribe(reactiveTarget);

        return this;
    }

    //TODO revisit the use cases for a merge function mLatestFireIn async (Not the same as RX zip)
//    @Override // IReactiveSource
//    public <UPCHAIN_OUT> IReactiveSource<OUT> merge(IReactiveSource<UPCHAIN_OUT> upchainReactiveSource) {
//        upchainReactiveSource.subscribe(this);
//
//        return this;
//    }

    @Override // IReactiveSource
    @NonNull
    public IReactiveSource<OUT> subscribe(@NonNull  final IAction<OUT> action) {
        return subscribe(mThreadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    public IReactiveSource<OUT> subscribe(@NonNull  final IThreadType threadType, @NonNull  final IAction<OUT> action) {
        return subscribeMap(threadType, out -> {
            action.call();
            return out;
        });
    }

    @Override // IReactiveSource
    @NonNull
    public IReactiveSource<OUT> subscribe(@NonNull  final IActionOne<OUT> action) {
        return subscribe(mThreadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    public IReactiveSource<OUT> subscribe(
            @NonNull  final IThreadType threadType,
            @NonNull  final IActionOne<OUT> action) {
        return subscribeMap(threadType, out -> {
            action.call(out);
            return out;
        });
    }

    @Override // IReactiveSource
    @NonNull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribeMap(@NonNull  final IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        return subscribeMap(mThreadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribeMap(
            @NonNull  final IThreadType threadType,
            @NonNull  IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        final IReactiveSource<DOWNCHAIN_OUT> subscription = new Subscription<>(getName(), this, threadType, action, mOnError);
        subscribe((IReactiveTarget<OUT>) subscription); //TODO Suspicious cast here

        return subscription;
    }

    @Override // IReactiveSource
    @NonNull
    public IReactiveSource<OUT> subscribe(@NonNull  final IReactiveTarget<OUT> reactiveTarget) {
        if (mReactiveTargets.addIfAbsent(new AltWeakReference<>(reactiveTarget))) {
            reactiveTarget.subscribeSource("Reference to keep reactive chain value being garbage collected", this);
            vv(this, mOrigin, "Added WeakReference to down-chain IReactiveTarget \"" + reactiveTarget.getName());
        } else {
            ii(this, mOrigin, "IGNORED duplicate subscribe of down-chain IReactiveTarget \"" + reactiveTarget.getName());
        }

        return this;
    }

    @Override // IReactiveSource
    @NonNull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribe(@NonNull  final IActionR<OUT, DOWNCHAIN_OUT> action) {
        return subscribe(mThreadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribe(@NonNull  final IThreadType threadType, @NonNull  final IActionR<OUT, DOWNCHAIN_OUT> action) {
        final IReactiveSource<DOWNCHAIN_OUT> subscription = new Subscription<>(
                getName(), this, threadType,
                t -> {
                    return action.call();
                },
                mOnError);
        subscribe((IReactiveTarget<OUT>) subscription);

        return subscription;
    }
}
