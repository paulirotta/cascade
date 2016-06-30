/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.functional;

import android.support.annotation.NonNull;

import com.reactivecascade.Async;
import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.RCLog;

/**
 * The on-cancelled action in a chain will be launched asynchonosly
 * <p>
 * Cancelled notifications are not consumed. All downchain items will also receive the
 * {@link #doOnCancelled(StateCancelled)} notification call synchronously.
 * <p>
 * Cancellation may occur from any thread. In the event of concurrent cancellation, {@link #doOnCancelled(StateCancelled)}
 * will be called exactly one time.
 */
public class OnCancelledAltFuture<T> extends SettableAltFuture<T> {
    @NonNull
    private final IActionOne<String> mOnCancelledAction;

    /**
     * Constructor
     *
     * @param threadType the thread pool to run this command on
     * @param action     a function that receives one input and no return from
     */
    @SuppressWarnings("unchecked")
    public OnCancelledAltFuture(@NonNull IThreadType threadType,
                                @NonNull IActionOne<String> action) {
        super(threadType);

        this.mOnCancelledAction = action;
    }

    @NotCallOrigin
    @Override // IAltFuture
    public void doOnCancelled(@NonNull StateCancelled stateCancelled) throws Exception {
        RCLog.d(this, "Handling doOnCancelled(): " + stateCancelled);

        if (!this.stateAR.compareAndSet(VALUE_NOT_AVAILABLE, stateCancelled) || (Async.USE_FORKED_STATE && !this.stateAR.compareAndSet(FORKED, stateCancelled))) {
            RCLog.i(this, "Will not doOnCancelled() because IAltFuture state is already determined: " + stateAR.get());
            return;
        }

        threadType
                .from(stateCancelled.getReason())
                .then(mOnCancelledAction)
                .fork();

        Exception e = forEachThen(af -> {
            af.doOnCancelled(stateCancelled);
        });

        if (e != null) {
            throw e;
        }
    }
}
