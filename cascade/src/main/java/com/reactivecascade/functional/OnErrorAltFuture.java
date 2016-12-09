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

import static com.reactivecascade.i.IAltFuture.AltFutureState.ERROR;
import static com.reactivecascade.i.IAltFuture.AltFutureState.FORKED;
import static com.reactivecascade.i.IAltFuture.AltFutureState.PENDING;

/**
 * The on-error action in a chain will be launched asynchronously
 * <p>
 * The error is consumed by this chain link. All downchain items will be notified synchronously
 * as {@link #onCancelled(CharSequence)}
 */
public class OnErrorAltFuture<T> extends SettableAltFuture<T> {
    @NonNull
    private final IActionOne<Exception> onErrorAction;

    /**
     * Constructor
     *
     * @param threadType    the thread pool to run this command on
     * @param onErrorAction a function that receives one input and no return from
     */
    @SuppressWarnings("unchecked")
    public OnErrorAltFuture(@NonNull IThreadType threadType,
                            @NonNull IActionOne<Exception> onErrorAction) {
        super(threadType);

        this.onErrorAction = onErrorAction;
    }

    @NotCallOrigin
    @Override // IAltFuture
    public void onError(@NonNull Exception e) throws Exception {
        RCLog.d(this, "Handling onError(): " + e);

        if (!this.stateAR.compareAndSet(PENDING, ERROR) || (Async.USE_FORKED_STATE && !this.stateAR.compareAndSet(FORKED, ERROR))) {
            RCLog.i(this, "Will not onError() because IAltFuture State is already determined: " + stateAR.get());
            return;
        }

        threadType
                .from(e)
                .then(onErrorAction)
                .fork();

//        StateCancelled stateCancelled = new StateCancelled() {
//            private final ImmutableValue<String> mOrigin = RCLog.originAsync();
//
//            @NonNull
//            @Override
//            public String getReason() {
//                return "Cancelled after onError() notified: " + getStateError();
//            }
//
//            @Nullable
//            @Override
//            public StateError getStateError() {
//                return stateError;
//            }
//
//            @NonNull
//            @Override
//            public ImmutableValue<String> getOrigin() {
//                return mOrigin;
//            }
//        };
        String reason = "Upchain error: " + e;

        Exception e2 = forEachThen(af -> {
            af.onCancelled(reason);
        });

        if (e2 != null) {
            throw e2;
        }
    }
}
