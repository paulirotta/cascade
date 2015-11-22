package com.futurice.cascade.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.Async;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.active.RunnableAltFuture;
import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;

/**
 * The on-error action
 */
public class OnErrorAltFuture<IN, OUT> extends RunnableAltFuture<IN, OUT> {
    /**
     * Constructor
     *
     * @param threadType the thread pool to run this command on
     * @param action     a function that receives one input and no return from
     */
    @SuppressWarnings("unchecked")
    public OnErrorAltFuture(@NonNull IThreadType threadType,
                            @NonNull IActionOne<Exception> action) {
        super(threadType, (IActionOne<IN>) action);
    }

    @NotCallOrigin
    @Override // AbstractAltFuture
    public void doOnError(@NonNull final StateError stateError) throws Exception {
        RCLog.d(this, "Handling doOnError(): " + stateError);

        if (!this.mStateAR.compareAndSet(ZEN, stateError) || (Async.USE_FORKED_STATE && !this.mStateAR.compareAndSet(FORKED, stateError))) {
            RCLog.i(this, "Will not repeat doOnError() because IAltFuture state is already determined: " + mStateAR.get());
            return;
        }

        @SuppressWarnings("unchecked")
        final IAltFuture<?, Exception> altFuture = mThreadType
                .from(stateError.getException())
                .then((IAction<Exception>) this);

        final StateCancelled stateCancelled = new StateCancelled() {
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

        final Exception e = forEachThen(af -> {
            af.doOnCancelled(stateCancelled);
        });

        if (e != null) {
            throw e;
        }
    }
}
