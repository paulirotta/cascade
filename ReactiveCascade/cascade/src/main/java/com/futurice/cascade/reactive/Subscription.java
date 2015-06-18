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
package com.futurice.cascade.reactive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.action.IActionOneR;
import com.futurice.cascade.i.action.IActionR;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.i.reactive.IReactiveSource;
import com.futurice.cascade.i.reactive.IReactiveTarget;
import com.futurice.cascade.util.AltWeakReference;
import com.futurice.cascade.util.nonnull;
import com.futurice.cascade.util.nullable;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.futurice.cascade.Async.*;

/**
 * This is the default implementation for a reactive active chain link.
 * <p>
 * NOTE: Because there _may_ exist a possibility of multiple fire events racing each other on different
 * threads, it is important that the functions in the reactive chain are idempotent and stateless.
 * <p>
 * <code>Subscription</code>s are both an {@link com.futurice.cascade.i.reactive.IReactiveTarget} and
 * {@link com.futurice.cascade.i.reactive.IReactiveSource}.
 * <p>
 * <p>
 * TODO Add setFireEveryValue(true) option to queue up and fire all states one by one. If inOrderExecutor, this fire will be FIFO sequential, otherwise concurrent
 *
 * @param <OUT>
 * @param <IN>  the type of the second link in the active chain
 */
@NotCallOrigin
public class Subscription<IN, OUT> implements IReactiveTarget<IN>, IReactiveSource<OUT> {
    //FIXME Replace these values with changing lastFireInIsFireNext to be volatile boolean needToQueue to simplify logic
    private static final Object FIRE_ACTION_NOT_QUEUED = new Object(); // A marker state for fireAction to indicate the need to queue on next fire

    @NonNull
    @nonnull
    protected final IThreadType threadType;
    @NonNull
    @nonnull
    private final String name;
    @NonNull
    @nonnull
    protected final IOnErrorAction onError;
    @NonNull
    @nonnull
    private final CopyOnWriteArrayList<IReactiveSource<IN>> reactiveSources = new CopyOnWriteArrayList<>();
    @NonNull
    @nonnull
    protected final CopyOnWriteArrayList<AltWeakReference<IReactiveTarget<OUT>>> reactiveTargets = new CopyOnWriteArrayList<>(); // Holding a strong reference is optional, depending on the binding type
    @NonNull
    @nonnull
    protected final ImmutableValue<String> origin; // Helpful latestFireIn debugOrigin builds to display a log link to the lambda passed into this headFunctionalChainLink when it was created
    @NonNull
    @nonnull
    protected final IActionOneR<IN, OUT> onFireAction;
    @NonNull
    @nonnull
    private final AtomicReference<Object> latestFireIn = new AtomicReference<>(FIRE_ACTION_NOT_QUEUED); // If is FIRE_ACTION_NOT_QUEUED, re-queue fireAction on next fire()
    @NonNull
    @nonnull
    private final AtomicBoolean latestFireInIsFireNext = new AtomicBoolean(true); // Signals high priority re-execution if still processing the previous value
    @NonNull
    @nonnull
    private final Runnable fireRunnable;
    @Nullable
    @nullable
    private final IReactiveSource<IN> upchainReactiveSource; // This is held to keep the chain from being garbage collected until the tail of the chain is de-referenced

    /**
     * Create a new default implementation of a reactive active chain link
     * <p>
     * If there are multiple down-chain targets attached to this node, it will concurrently fire
     * all down-chain branches.
     *
     * @param name                  the descriptive debug name of this subscription
     * @param upchainReactiveSource
     * @param threadType            the default thread group on which this subscription fires
     * @param onFireAction          Because there _may_ exist a possibility of multiple fire events racing each other on different
     *                              threads, it is important that the onFireAction functions in the reactive chain are idempotent and stateless. Further analysis is needed, but be cautious.
     * @param onError
     */
    @SuppressWarnings("unchecked")
    public Subscription(@NonNull @nonnull final String name,
                        @Nullable @nullable final IReactiveSource<IN> upchainReactiveSource,
                        @Nullable @nullable final IThreadType threadType,
                        @NonNull @nonnull final IActionOneR<IN, OUT> onFireAction,
                        @Nullable @nullable final IOnErrorAction onError) {
        this.origin = originAsync();
        this.name = name;
        this.upchainReactiveSource = upchainReactiveSource;
        if (upchainReactiveSource != null) {
            upchainReactiveSource.subscribe(this);
        }
        this.threadType = threadType != null ? threadType : UI;
        this.onFireAction = onFireAction;
        this.onError = onError != null ? onError : e -> ee(getOrigin(), "Problem firing subscription, name=" + getName(), e);

        /*
         * Singleton executor - there is only one which is never queued more than once at any time
         *
         * Fire using the most recently set value. Skip intermediate values when they arrive too
         * fast to process.
          *
          * Re-queue if the input value changes before exiting
         */
        fireRunnable = this.threadType.wrapRunnableAsErrorProtection(() -> {
            final Object latestValueFired = latestFireIn.get();
            doReceiveFire((IN) latestValueFired); // This step may take some time
            if (!latestFireIn.compareAndSet(latestValueFired, FIRE_ACTION_NOT_QUEUED)) {
                if (latestFireInIsFireNext.getAndSet(true)) {
                    this.threadType.runNext(getFireRunnable()); // Input was set again while processing this value- re-queue to fire again after other pending work
                } else {
                    this.threadType.run(getFireRunnable()); // Input was set again while processing this value- re-queue to fire again after other pending work
                }
            }
        });
    }

    @NonNull
    @nonnull
    protected ImmutableValue<String> getOrigin() {
        return this.origin;
    }

    private Runnable getFireRunnable() {
        return fireRunnable;
    }

//================================= Public Utility Methods =======================================

    @Override // INamed
    @NonNull
    @nonnull
    public String getName() {
        return this.name;
    }

    @Override // IReactiveTarget
    public void subscribeSource(@NonNull @nonnull final String reason, @NonNull @nonnull final IReactiveSource<IN> reactiveSource) {
        if (!reactiveSources.addIfAbsent(reactiveSource)) {
            dd(this, origin, "Did you say hello several times or create some other mess? Upchain says hello, but we already have a hello from \"" + reactiveSource.getName() + "\" at \"" + getName() + "\"");
        } else {
            vv(this, origin, reactiveSource.getName() + " says hello: reason=" + reason);
        }
    }

    @Override
    @NotCallOrigin
    public void unsubscribeSource(@NonNull @nonnull final String reason, @NonNull @nonnull final IReactiveSource<IN> reactiveSource) {
        if (reactiveSources.remove(reactiveSource)) {
            vv(this, origin, "Upchain '" + reactiveSource.getName() + "' unsubscribeSource, reason=" + reason);
        } else {
            ii(this, origin, "Upchain '" + reactiveSource.getName() + "' unsubscribeSource, reason=" + reason + "\nWARNING: This source is not current. Probably this is a garbage collection/weak reference side effect");
        }
    }

//================================= Internal Utility Methods =======================================

    /**
     * Do <code>onFireAction</code> to every downstream target that does not have an expired
     * {@link java.lang.ref.WeakReference}
     *
     * @param action something to do for each target- return <code>true</code> if this is the last action
     *               and the loop should abort early (loop is complete)
     * @return <code>true</code> if _any_ of the action(reactive_target) calls returns <code>true</code>
     * @throws Exception
     */
    private boolean forEachReactiveTarget(@NonNull @nonnull final IActionOneR<IReactiveTarget<OUT>, Boolean> action) throws Exception {
        final Iterator<AltWeakReference<IReactiveTarget<OUT>>> iterator = reactiveTargets.iterator();
        boolean result = false;

        while (iterator.hasNext()) {
            final WeakReference<IReactiveTarget<OUT>> weakReference = iterator.next();
            final IReactiveTarget<OUT> reactiveTarget = weakReference.get();

            if (reactiveTarget != null) {
                result |= action.call(reactiveTarget);
            } else {
                vv(this, origin, getName() + " A .subscribe(IReactiveTarget) latestFireIn the reactive chain is an expired WeakReference- that leaf node of this Binding chain has be garbage collected.");
                reactiveTargets.remove(weakReference);
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
    protected final boolean searchReactiveTargets(@NonNull @nonnull final IReactiveTarget<OUT> searchItem, @NonNull @nonnull final IActionOne<IReactiveTarget<OUT>> actionIfFound) throws Exception {
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
    public void fire(@NonNull @nonnull final IN in) {
        vv(this, origin, "fire latestFireIn=" + in);
        latestFireInIsFireNext.set(false);
        /*
         There is a race at this point between latestFireIn and latestFireInIsFireNext.
         By design, if the race is lost, map a normal fire will actually fire next. So we evaluate ahead of
         other pending actions- small loss, and not a problem as there is no dependency upset by this.

         This design is more efficient than the memory thrash at every reactive evaluation step that
         would explicitly atomically couple the signals into a new Pair(in, boolean) structure.
         */
        if (latestFireIn.getAndSet(in) == FIRE_ACTION_NOT_QUEUED) {
            // Only queue for execution if not already queued
            threadType.run(getFireRunnable());
        }
    }

    @NotCallOrigin
    @Override // IReactiveTarget
    public void fireNext(@NonNull @nonnull final IN in) {
        vv(this, origin, "fireNext latestFireIn=" + in);
        if (latestFireIn.getAndSet(in) == FIRE_ACTION_NOT_QUEUED) {
            // Only queue for execution if not already queued
            threadType.runNext(fireRunnable);
        } else {
            // Already queued for execution, but possibly not soon- push it to the top of the stack
            threadType.moveToHeadOfQueue(fireRunnable);
        }
    }

    @NotCallOrigin
    private void doReceiveFire(@NonNull @nonnull final IN in) throws Exception {
        final OUT out = doAction(in);

        try {
            doDownchainActions(in, out);
        } catch (Exception e) {
            String str = "Can not doDownchainActions latestFireIn=" + in;
            ee(this, str, e);
        }
    }

    /**
     * Always called from the headFunctionalChainLink's refire subscribe. By default this executes on ThreadType.WORKER
     */
    @NotCallOrigin
    @NonNull
    @nonnull
    private OUT doAction(@NonNull @nonnull final IN in) throws Exception {
        vv(this, origin, "doReceiveFire \"" + getName() + " value=" + in);
//        visualize(getName(), latestFireIn.toString(), "AbstractBinding");

        return onFireAction.call(in);
    }

    /**
     * Always called from the headFunctionalChainLink's refire subscribe. By default this executes on Async.WORKER
     */
    @NotCallOrigin
    private void doDownchainActions(@NonNull @nonnull final IN in, @NonNull @nonnull final OUT out) throws Exception {
        forEachReactiveTarget(reactiveTarget -> {
            vv(this, origin, "Fire down-chain reactive target " + reactiveTarget.getName() + ", value=" + out);
            reactiveTarget.fireNext(out);
            return false;
        });
        if (reactiveTargets.size() == 0) {
            vv(this, origin, "Fire down-chain reactive targets, but there are zero targets for " + getName() + ", value=" + out);
        }
    }

//=================================.subscribe() Actions ==========================================

    @Override // IReactiveSource
    public boolean unsubscribe(@NonNull @nonnull final String reason, @NonNull @nonnull final IReactiveTarget<OUT> reactiveTarget) {
        try {
            return searchReactiveTargets(reactiveTarget, wr -> {
                        vv(this, origin, "unsubscribeSource(IReactiveTarget) reason=" + reason + " reactiveTarget=" + reactiveTarget);
                        //TODO Annotate to remove the following warning. The action is safe due to AltWeakReference behavior
                        reactiveTargets.remove(reactiveTarget);
                    }
            );
        } catch (Exception e) {
            ee(this, origin, "Can not remove IReactiveTarget reason=" + reason + " reactiveTarget=" + reactiveTarget, e);
            return false;
        }
    }

    @Override // IReactiveTarget
    public void unsubscribeAllSources(@NonNull @nonnull final String reason) {
        final Iterator<IReactiveSource<IN>> iterator = reactiveSources.iterator();

        vv(this, origin, "Unsubscribing all sources, reason=" + reason);
        while (iterator.hasNext()) {
            iterator.next().unsubscribeAll(reason);
        }
    }

    @Override // IReactiveSource
    public void unsubscribeAll(@NonNull @nonnull final String reason) {
        dd(this, origin, "unsubscribeAll() reason=" + reason);

        try {
            forEachReactiveTarget(reactiveTarget -> {
                unsubscribe(reason, reactiveTarget);
                //TODO Annotate to remove the following warning. The action is safe due to AltWeakReference behavior
                reactiveTargets.remove(reactiveTarget);
                return false;
            });
        } catch (Exception e) {
            ee(this, origin, "Can not unsubscribeAll, reason=" + reason, e);
        }
    }

    @Override // IReactiveSource
    public IReactiveSource<OUT> split(@NonNull @nonnull final IReactiveTarget<OUT> reactiveTarget) {
        subscribe(reactiveTarget);

        return this;
    }

    //TODO revisit the use cases for a merge function latestFireIn async (Not the same as RX zip)
//    @Override // IReactiveSource
//    public <UPCHAIN_OUT> IReactiveSource<OUT> merge(IReactiveSource<UPCHAIN_OUT> upchainReactiveSource) {
//        upchainReactiveSource.subscribe(this);
//
//        return this;
//    }

    @Override // IReactiveSource
    @NonNull
    @nonnull
    public IReactiveSource<OUT> subscribe(@NonNull @nonnull final IAction<OUT> action) {
        return subscribe(threadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    @nonnull
    public IReactiveSource<OUT> subscribe(@NonNull @nonnull final IThreadType threadType, @NonNull @nonnull final IAction<OUT> action) {
        return subscribeMap(threadType, out -> {
            action.call();
            return out;
        });
    }

    @Override // IReactiveSource
    @NonNull
    @nonnull
    public IReactiveSource<OUT> subscribe(@NonNull @nonnull final IActionOne<OUT> action) {
        return subscribe(threadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    @nonnull
    public IReactiveSource<OUT> subscribe(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final IActionOne<OUT> action) {
        return subscribeMap(threadType, out -> {
            action.call(out);
            return out;
        });
    }

    @Override // IReactiveSource
    @NonNull
    @nonnull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribeMap(@NonNull @nonnull final IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        return subscribeMap(threadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    @nonnull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribeMap(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        final IReactiveSource<DOWNCHAIN_OUT> subscription = new Subscription<>(getName(), this, threadType, action, onError);
        subscribe((IReactiveTarget<OUT>) subscription); //TODO Suspicious cast here

        return subscription;
    }

    @Override // IReactiveSource
    @NonNull
    @nonnull
    public IReactiveSource<OUT> subscribe(@NonNull @nonnull final IReactiveTarget<OUT> reactiveTarget) {
        if (reactiveTargets.addIfAbsent(new AltWeakReference<>(reactiveTarget))) {
            reactiveTarget.subscribeSource("Reference to keep reactive chain from being garbage collected", this);
            vv(this, origin, "Added WeakReference to down-chain IReactiveTarget \"" + reactiveTarget.getName());
        } else {
            ii(this, origin, "IGNORED duplicate subscribe of down-chain IReactiveTarget \"" + reactiveTarget.getName());
        }

        return this;
    }

    @Override // IReactiveSource
    @NonNull
    @nonnull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribe(@NonNull @nonnull final IActionR<OUT, DOWNCHAIN_OUT> action) {
        return subscribe(threadType, action);
    }

    @Override // IReactiveSource
    @NonNull
    @nonnull
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribe(@NonNull @nonnull final IThreadType threadType, @NonNull @nonnull final IActionR<OUT, DOWNCHAIN_OUT> action) {
        final IReactiveSource<DOWNCHAIN_OUT> subscription = new Subscription<>(
                getName(), this, threadType,
                t -> {
                    return action.call();
                },
                onError);
        subscribe((IReactiveTarget<OUT>) subscription);

        return subscription;
    }
}
