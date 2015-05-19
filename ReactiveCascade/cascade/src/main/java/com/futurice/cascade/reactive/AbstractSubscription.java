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

import com.futurice.cascade.AltWeakReference;
import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.action.IActionOneR;
import com.futurice.cascade.i.action.IActionR;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.i.reactive.IReactiveSource;
import com.futurice.cascade.i.reactive.IReactiveTarget;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.futurice.cascade.Async.*;

/**
 * Both a reactive target and a reactive subscription
 *
 * @param <IN>
 * @param <OUT>
 */
// TODO Use delegates that can be reused latestFireIn other classes
@NotCallOrigin
public abstract class AbstractSubscription<IN, OUT> implements IReactiveTarget<IN>, IReactiveSource<OUT> {
    private static final Object FIRE_ACTION_NOT_QUEUED = new Object(); // A marker state for fireAction to indicate the need to queue on next fire
    private static final Object FIRE_ACTION_EXECUTING = new Object(); // A marker state for fireAction to indicate that it is now running on a thread

    protected final IThreadType threadType;
    private final String name;
    protected final IOnErrorAction onError;
    private final CopyOnWriteArrayList<IReactiveSource<IN>> reactiveSources = new CopyOnWriteArrayList<>();
    protected final CopyOnWriteArrayList<AltWeakReference<IReactiveTarget<OUT>>> reactiveTargets = new CopyOnWriteArrayList<>(); // Holding a strong reference is optional, depending on the binding type
    protected final ImmutableValue<String> origin; // Helpful latestFireIn debugOrigin builds to display a log link to the lambda passed into this headFunctionalChainLink when it was created
    protected final IActionOneR<IN, OUT> onFireAction;
    private final AtomicReference<Object> latestFireIn = new AtomicReference<>(FIRE_ACTION_NOT_QUEUED); // If is FIRE_ACTION_NOT_QUEUED, re-queue fireAction on next fire()
    private final AtomicBoolean latestFireInIsFireNext = new AtomicBoolean(true); // Signals high priority re-execution if still processing the previous value
    private final Runnable fireRunnable;

    //TODO Update onError to be composable based on the current value latestFireIn the chain like latestFireIn the functional chain (currently is set only by the chain head and with that SOURCE value only)
    public AbstractSubscription(@NonNull IThreadType threadType, @NonNull String name, @NonNull IOnErrorAction onError, @NonNull IActionOneR<IN, OUT> onFireAction) { //TODO Reorder onFireAction before onErrorAction with the app in scope
        this.threadType = threadType;
        this.name = name;
        this.onError = onError;
        this.onFireAction = onFireAction;
        this.origin = originAsync();

        /*
         * Singleton executor - there is only one which is never queued more than once at any time
         *
         * Fire using the most recently set value. Skip intermediate values when they arrive too
         * fast to process.
          *
          * Re-queue if the input value changes before exiting
         */
        fireRunnable = threadType.wrapRunnableAsErrorProtection(() -> {
            final Object latestValueFired = latestFireIn.get();
            doReceiveFire((IN) latestValueFired); // This step may take some time
            if (!latestFireIn.compareAndSet(latestValueFired, FIRE_ACTION_NOT_QUEUED)) {
                if (latestFireInIsFireNext.getAndSet(true)) {
                    threadType.executeNext(getFireRunnable()); // Input was set again while processing this value- re-queue to fire again after other pending work
                } else {
                    threadType.execute(getFireRunnable()); // Input was set again while processing this value- re-queue to fire again after other pending work
                }
            }
        });
    }

    private Runnable getFireRunnable() {
        return fireRunnable;
    }

//================================= Public Utility Methods =======================================

    @Override // INamed
    public String getName() {
        return this.name;
    }

    @Override // IReactiveTarget
    public void subscribeSource(@NonNull String reason, @NonNull IReactiveSource<IN> reactiveSource) {
        if (!reactiveSources.addIfAbsent(reactiveSource)) {
            dd(this, origin, "Did you say hello several times or create some other mess? Upchain says hello, but we already have a hello from \"" + reactiveSource.getName() + "\" at \"" + getName() + "\"");
        } else {
            vv(this, origin, reactiveSource.getName() + " says hello: reason=" + reason);
        }
    }

    @Override
    @NotCallOrigin
    public void unsubscribeSource(@NonNull String reason, @NonNull IReactiveSource<IN> reactiveSource) {
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
    private boolean forEachReactiveTarget(@NonNull IActionOneR<IReactiveTarget<OUT>, Boolean> action) throws Exception {
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
     * @param searchItem
     * @param actionIfFound
     * @return
     * @throws Exception
     */
    protected final boolean searchReactiveTargets(@NonNull final IReactiveTarget<OUT> searchItem, @NonNull final IActionOne<IReactiveTarget<OUT>> actionIfFound) throws Exception {
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
    public void fire(@NonNull final IN in) {
        vv(this, origin, "fire latestFireIn=" + in);
        latestFireInIsFireNext.set(false);
        /*
         There is a race at this point between latestFireIn and latestFireInIsFireNext.
         By design, if the race is lost, then a normal fire will actually fire next. So we evaluate ahead of
         other pending actions- small loss, and not a problem as there is no dependency upset by this.

         This design is more efficient than the memory thrash at every reactive evaluation step that
         would explicitly atomically couple the signals into a new Pair(in, boolean) structure.
         */
        if (latestFireIn.getAndSet(in) == FIRE_ACTION_NOT_QUEUED) {
            // Only queue for execution if not already queued
            threadType.execute(fireRunnable);
        }
    }

    @NotCallOrigin
    @Override // IReactiveTarget
    public void fireNext(@NonNull final IN in) {
        vv(this, origin, "fireNext latestFireIn=" + in);
        if (latestFireIn.getAndSet(in) == FIRE_ACTION_NOT_QUEUED) {
            // Only queue for execution if not already queued
            threadType.executeNext(fireRunnable);
        } else {
            // Already queued for execution, but possibly not soon- push it to the top of the stack
            threadType.moveToHeadOfQueue(fireRunnable);
        }
    }

    @NotCallOrigin
    private void doReceiveFire(@NonNull final IN in) throws Exception {
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
    private OUT doAction(@NonNull final IN in) throws Exception {
        vv(this, origin, "doReceiveFire \"" + getName() + " value=" + in);
//        visualize(getName(), latestFireIn.toString(), "AbstractBinding");

        return onFireAction.call(in);
    }

    /**
     * Always called from the headFunctionalChainLink's refire subscribe. By default this executes on Async.WORKER
     */
    @NotCallOrigin
    private void doDownchainActions(@NonNull IN in, @NonNull OUT out) throws Exception {
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
    public boolean unsubscribe(@NonNull String reason, @NonNull IReactiveTarget<OUT> reactiveTarget) {
        try {
            return searchReactiveTargets(reactiveTarget, wr -> {
                        vv(this, origin, "unsubscribeSource(IReactiveTarget) reason=" + reason + " reactiveTarget=" + reactiveTarget);
                        reactiveTargets.remove(reactiveTarget);
                    }
            );
        } catch (Exception e) {
            ee(this, origin, "Can not remove IReactiveTarget reason=" + reason + " reactiveTarget=" + reactiveTarget, e);
            return false;
        }
    }

    @Override // IReactiveTarget
    public void unsubscribeAllSources(@NonNull String reason) {
        final Iterator<IReactiveSource<IN>> iterator = reactiveSources.iterator();

        vv(this, origin, "Unsubscribing all sources, reason=" + reason);
        while (iterator.hasNext()) {
            iterator.next().unsubscribeAll(reason);
        }
    }

    @Override // IReactiveSource
    public void unsubscribeAll(@NonNull String reason) {
        dd(this, origin, "unsubscribeAll() reason=" + reason);

        try {
            forEachReactiveTarget(reactiveTarget -> {
                unsubscribe(reason, reactiveTarget);
                reactiveTargets.remove(reactiveTarget);
                return false;
            });
        } catch (Exception e) {
            ee(this, origin, "Can not unsubscribeAll, reason=" + reason, e);
        }
    }

    @Override // IReactiveSource
    public IReactiveSource<OUT> split(@NonNull IReactiveTarget<OUT> reactiveTarget) {
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
    public IReactiveSource<OUT> subscribe(@NonNull IAction<OUT> action) {
        return subscribe(threadType, action);
    }

    @Override // IReactiveSource
    public IReactiveSource<OUT> subscribe(@NonNull IThreadType threadType, @NonNull IAction<OUT> action) {
        return subscribe(threadType, out -> {
            action.call();
            return out;
        });
    }

    @Override // IReactiveSource
    public IReactiveSource<OUT> subscribe(@NonNull IActionOne<OUT> action) {
        return subscribe(threadType, action);
    }

    @Override // IReactiveSource
    public IReactiveSource<OUT> subscribe(@NonNull IThreadType threadType, @NonNull IActionOne<OUT> action) {
        return subscribe(threadType, out -> {
            action.call(out);
            return out;
        });
    }

    @Override // IReactiveSource
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribe(@NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        return subscribe(threadType, action);
    }

    @Override // IReactiveSource
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribe(@NonNull IThreadType threadType, @NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action) {
        IReactiveSource<DOWNCHAIN_OUT> subscription = new Subscription(threadType, getName(), this, action, onError);
        subscribe((IReactiveTarget<OUT>) subscription);

        return subscription;
    }

    @Override // IReactiveSource
    public IReactiveSource<OUT> subscribe(@NonNull IReactiveTarget<OUT> reactiveTarget) {
        if (reactiveTargets.addIfAbsent(new AltWeakReference<>(reactiveTarget))) {
            vv(this, origin, "Added WeakReference to down-chain IReactiveTarget \"" + reactiveTarget.getName());
        } else {
            ii(this, origin, "IGNORED duplicate subscribe of down-chain IReactiveTarget \"" + reactiveTarget.getName());
        }

        return this;
    }

    @Override // IReactiveSource
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribe(@NonNull IActionR<OUT, DOWNCHAIN_OUT> action) {
        return subscribe(threadType, action);
    }

    @Override // IReactiveSource
    public <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribe(@NonNull IThreadType threadType, @NonNull IActionR<OUT, DOWNCHAIN_OUT> action) {
        //TODO AbstractSubscription should probably be merged with and named "Subscription" to avoid referencing a child class in the abstract parent
        IReactiveSource<DOWNCHAIN_OUT> subscription = new Subscription(
                threadType,
                getName(),
                this,
                t -> {
                    return action.call();
                },
                onError);
        subscribe((IReactiveTarget<OUT>) subscription);

        return subscription;
    }
}
