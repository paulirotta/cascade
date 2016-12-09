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

import static com.reactivecascade.i.IAltFuture.AltFutureState.CANCELLED;
import static com.reactivecascade.i.IAltFuture.AltFutureState.FORKED;
import static com.reactivecascade.i.IAltFuture.AltFutureState.PENDING;

/**
 * The on-cancelled action in a chain will be launched asynchonosly
 * <p>
 * Cancelled notifications are not consumed. All downchain items will also receive the
 * {@link #onCancelled(CharSequence)} notification call synchronously.
 * <p>
 * Cancellation may occur from any thread.
 */
public class OnCancelledAltFuture<T> extends SettableAltFuture<T> {
    @NonNull
    private final IActionOne<CharSequence> onCancelledAction;

    /**
     * Constructor
     *
     * @param threadType the thread pool to run this command on
     * @param action     a function that receives one input and no return from
     */
    @SuppressWarnings("unchecked")
    public OnCancelledAltFuture(@NonNull IThreadType threadType,
                                @NonNull IActionOne<CharSequence> action) {
        super(threadType);

        this.onCancelledAction = action;
    }

    @NotCallOrigin
    @Override // IAltFuture
    public void onCancelled(@NonNull CharSequence reason) throws Exception {
        RCLog.d(this, "Handling onCancelled(): " + reason);

        if (!this.stateAR.compareAndSet(PENDING, CANCELLED) || (Async.USE_FORKED_STATE && !this.stateAR.compareAndSet(FORKED, CANCELLED))) {
            RCLog.i(this, "Will not onCancelled() because IAltFuture State is already determined: " + stateAR.get());
            return;
        }

        threadType
                .from(reason)
                .then(onCancelledAction)
                .fork();

        Exception e = forEachThen(af -> {
            af.onCancelled(reason);
        });

        if (e != null) {
            throw e;
        }
    }
}
