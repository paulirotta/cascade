/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.functional;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.reactivecascade.Async;
import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.RCLog;

/**
 * The on-error action in a chain will be launched asynchronously
 * <p>
 * The error is consumed by this chain link. All downchain items will be notified synchronously
 * as {@link #onCancelled(StateCancelled)}
 */
public class OnErrorAltFuture<T> extends SettableAltFuture<T> {
    @NonNull
    private final IActionOne<Exception> mOnErrorAction;

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

        this.mOnErrorAction = onErrorAction;
    }

    @NotCallOrigin
    @Override // IAltFuture
    public void onError(@NonNull StateError stateError) throws Exception {
        RCLog.d(this, "Handling onError(): " + stateError);

        if (!this.stateAR.compareAndSet(VALUE_NOT_AVAILABLE, stateError) || (Async.USE_FORKED_STATE && !this.stateAR.compareAndSet(FORKED, stateError))) {
            RCLog.i(this, "Will not onError() because IAltFuture state is already determined: " + stateAR.get());
            return;
        }

        threadType
                .from(stateError.getException())
                .then(mOnErrorAction)
                .fork();

        StateCancelled stateCancelled = new StateCancelled() {
            private final ImmutableValue<String> mOrigin = RCLog.originAsync();

            @NonNull
            @Override
            public String getReason() {
                return "Cancelled after onError() notified: " + getStateError();
            }

            @Nullable
            @Override
            public StateError getStateError() {
                return stateError;
            }

            @NonNull
            @Override
            public ImmutableValue<String> getOrigin() {
                return mOrigin;
            }
        };

        Exception e = forEachThen(af -> {
            af.onCancelled(stateCancelled);
        });

        if (e != null) {
            throw e;
        }
    }
}
