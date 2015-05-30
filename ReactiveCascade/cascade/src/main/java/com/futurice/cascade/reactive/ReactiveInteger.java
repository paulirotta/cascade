package com.futurice.cascade.reactive;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.action.IOnErrorAction;
import static com.futurice.cascade.Async.*;

/**
 * An integer which can be updated in an atomic, thread-safe manner.
 *
 * This is similar to an {@link java.util.concurrent.atomic.AtomicInteger} with reactive bindings to
 * get and set the value in reactive chains (function sequences that can fire multiple times).
 *
 * Created by phou on 30-04-2015.
 */
public class ReactiveInteger extends ReactiveValue<Integer> {
    public ReactiveInteger(IThreadType threadType, String name, int initialValue) {
        super(threadType, name, initialValue);
    }

    public ReactiveInteger(
            @NonNull final IThreadType threadType,
            @NonNull final String name,
            final int initialValue,
            @NonNull final IOnErrorAction onError) {
        super(threadType, name, initialValue, onError);
    }

    public ReactiveInteger(IThreadType threadType, String name) {
        super(threadType, name);
    }

    public ReactiveInteger(IThreadType threadType, String name, IOnErrorAction onError) {
        super(threadType, name, onError);
    }

    public int incrementAndGet() {
        int currentValue;
        boolean firstFail = true;

        while (true) {
            currentValue = get();
            if (compareAndSet(currentValue, ++currentValue)) {
                break;
            }
            if (firstFail) {
                firstFail = false;
                ii(this, origin, "Failed concurrent increment, will try again: " + currentValue);
            }
        }

        return currentValue;
    }

    public int decrementAndGet() {
        int currentValue;

        while (true) {
            currentValue = get();
            if (compareAndSet(currentValue, --currentValue)) {
                break;
            }
            dd(this, origin, "Failed concurrent decrement, will try again: " + currentValue);
        }

        return currentValue;
    }
}
