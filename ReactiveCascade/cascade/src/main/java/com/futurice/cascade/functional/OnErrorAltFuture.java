package com.futurice.cascade.functional;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.Async;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.util.RCLog;

/**
 * The on-error action in a chain will be launched asynchronously
 * <p>
 * The error is consumed by this chain link. All downchain items will be notified synchronously
 * as {@link #doOnCancelled(StateCancelled)}
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
    public void doOnError(@NonNull StateError stateError) throws Exception {
        RCLog.d(this, "Handling doOnError(): " + stateError);

        if (!this.mStateAR.compareAndSet(ZEN, stateError) || (Async.USE_FORKED_STATE && !this.mStateAR.compareAndSet(FORKED, stateError))) {
            RCLog.i(this, "Will not doOnError() because IAltFuture state is already determined: " + mStateAR.get());
            return;
        }

        mThreadType
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
            af.doOnCancelled(stateCancelled);
        });

        if (e != null) {
            throw e;
        }
    }
}
