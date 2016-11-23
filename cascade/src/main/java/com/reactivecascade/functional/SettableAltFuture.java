/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.functional;

import android.support.annotation.NonNull;

import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.ISettableAltFuture;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.RCLog;

/**
 * An {@link IAltFuture} on which you can {@link SettableAltFuture#set(Object)}
 * one time to change State
 * <p>
 * Note that a <code>SettableAltFuture</code> is not itself {@link java.lang.Runnable}. You explicity {@link #set(Object)}
 * when the from is determined, and this changes the State to done. Therefore concepts like {@link IAltFuture#fork()}
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
 * TODO Would it be helpful for debugging to store and pass forward a reference to the object which originally detected the problem? It might help with filtering what onFireAction you want to do onError
 */
@NotCallOrigin
public class SettableAltFuture<T> extends AbstractAltFuture<T, T> implements ISettableAltFuture<T> {
    /**
     * Create, from is not yet determined
     *
     * @param threadType on which downchain actions continue
     */
    public SettableAltFuture(@NonNull IThreadType threadType) {
        super(threadType);
    }

    /**
     * Create, immutable from is set and will immediately fork() downchain actions
     *
     * @param threadType on which downchain actions continue
     * @param value
     */
    public SettableAltFuture(@NonNull IThreadType threadType,
                             @NonNull T value) {
        this(threadType);
        set(value);
    }

    @Override // ISettable
    public void set(@NonNull T value) {
        if (stateAR.compareAndSet(VALUE_NOT_AVAILABLE, value) || stateAR.compareAndSet(FORKED, value)) {
            // Previous State was FORKED, so set completes the onFireAction and continues the chain
            RCLog.v(this, "SettableAltFuture set, from= " + value);
            doFork();
            clearPreviousAltFuture();
            return;
        }

        // Already set, cancelled or error State
        RCLog.throwIllegalArgumentException(this, "Attempted to set " + this + " to from=" + value + ", but the from can only be set once and was already set to from=" + get());
    }

    protected void doFork() {
        // This is not an IRunnableAltFuture, so nothing to fork() or run(). But RunnableAltFuture overrides this and does more
    }

    //FIXME Remove this after testing
    @Override
    protected boolean isForked(@NonNull Object state) {
        return state != VALUE_NOT_AVAILABLE;
    }
}
