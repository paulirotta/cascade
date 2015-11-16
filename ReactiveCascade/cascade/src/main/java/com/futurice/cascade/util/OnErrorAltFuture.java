package com.futurice.cascade.util;

import android.support.annotation.NonNull;

import com.futurice.cascade.active.RunnableAltFuture;
import com.futurice.cascade.i.IActionOne;
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
    @Override // IAltFuture
    public void doOnError(@NonNull final StateError stateError) throws Exception {
        CLog.d(this, "Handling doOnError(): " + stateError);

        if (!this.mStateAR.compareAndSet(ZEN, stateError) && !this.mStateAR.compareAndSet(FORKED, stateError)) {
            CLog.i(this, "Can not doOnError because IAltFuture state is already determined: " + mStateAR.get());
            return;
        }

        //FIXME Continue here
//        mThreadType
//                .from(stateError.getException())
//                .then(this);
//
//        forEachThen(altFuture -> {
//            altFuture.doOnCancelled(stateError);
//        });
    }
}
