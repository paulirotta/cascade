package com.futurice.cascade;

import android.support.annotation.NonNull;

import com.futurice.cascade.functional.AltFutureFuture;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.functional.IAltFuture;

import java.util.concurrent.TimeUnit;

import static com.futurice.cascade.Async.*;

/**
 * Created by phou on 6/2/2015.
 */
public class TestUtil {
    private static final String TAG = TestUtil.class.getSimpleName();
    public static final long TEST_TIMEOUT = 1000; //ms

    private final IThreadType threadType;

    public TestUtil(@NonNull final IThreadType threadType) {
        this.threadType = threadType;
    }

    public <IN, OUT> OUT awaitDone(@NonNull final IAltFuture<IN, OUT> altFuture) throws Exception {
        return new AltFutureFuture<>(altFuture).get(TEST_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * TODO This works for simple cases, but not when additional steps are spawned which generate the error
     *
     * @param action
     * @param <IN>
     */
    public <IN> void hideIntentionalErrorStackTraces(
            @NonNull final IAction<IN> action) {
        SHOW_ERROR_STACK_TRACES = false;
        try {
            action.call();
        } catch (Exception e) {
            v(TAG, "hideIntentionalErrorStackTraces() from (probably intentional) test exception: " + e);
        } finally {
            // Turn error stack traces back on only after any pending async tasks already in the queue (usually this is good enough for masking intentional exception tests on a single-threaded test setup)
            threadType.then(() -> {
                SHOW_ERROR_STACK_TRACES = true;
            });
        }
    }
}
