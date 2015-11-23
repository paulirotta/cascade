/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.functional;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.i.ICancellable;
import com.futurice.cascade.i.ISettableAltFuture;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.functional.AbstractAltFuture;
import com.futurice.cascade.util.RCLog;

/**
 * An {@link IAltFuture} on which you can {@link SettableAltFuture#set(Object)}
 * one time to change state
 * <p>
 * Note that a <code>SettableAltFuture</code> is not itself {@link java.lang.Runnable}. You explicity {@link #set(Object)}
 * when the from is determined, and this changes the state to done. Therefore concepts like {@link IAltFuture#fork()}
 * and {@link IAltFuture#isForked()} do not have their traditional meanings.
 * <p>
 * {@link RunnableAltFuture} overrides this class.
 * TODO You may also use a {@link SettableAltFuture} to inject data where the from is determined from entirely outside of the current chain hierarchy.
 * This is currently an experimental feature so be warned, your results and chain behaviour may vary. Additional
 * testing is on the long list.
 * <p>
 * You may prefer to use {@link ImmutableValue} that a similar need in some cases. That is a
 * slightly faster, simpler implementation than {@link SettableAltFuture}.
 * <p>
 * TODO Would it be helpful for debugging to store and pass forward a reference to the object which originally detected the problem? It might help with filtering what mOnFireAction you want to do mOnError
 */
@NotCallOrigin
public class SettableAltFuture<IN, OUT> extends AbstractAltFuture<IN, OUT> implements ISettableAltFuture<IN, OUT> {
    /**
     * Create, from is not yet determined
     *
     * @param threadType on which downchain actions continue
     */
    public SettableAltFuture(@NonNull final IThreadType threadType) {
        super(threadType);
    }

    /**
     * Create, immutable from is set and will immediately fork() downchain actions
     *
     * @param threadType on which downchain actions continue
     * @param value
     */
    public SettableAltFuture(@NonNull final IThreadType threadType,
                             @NonNull final IN value) {
        this(threadType);
        set(value);
    }

    @Override // ISettable
    public void set(@NonNull final IN value) {
        if (mStateAR.compareAndSet(ZEN, value) || mStateAR.compareAndSet(FORKED, value)) {
            // Previous state was FORKED, so set completes the mOnFireAction and continues the chain
            RCLog.v(this, "SettableAltFuture set, from= " + value);
            doFork();
            clearPreviousAltFuture();
            return;
        }

        // Already set, cancelled or error state
        RCLog.throwIllegalArgumentException(this, "Attempted to set " + this + " to from=" + value + ", but the from can only be set once and was already set to from=" + get());
    }

    protected void doFork() {
        // This is not an IRunnableAltFuture, so nothing to run(). But RunnableAltFuture overrides this and does more
        try {
            doThen();
        } catch (Exception e) {
            RCLog.e(this, "Can not doFork()", e);
        }
    }
}
