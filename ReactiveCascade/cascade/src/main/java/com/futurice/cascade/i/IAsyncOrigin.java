package com.futurice.cascade.i;

import android.support.annotation.NonNull;

import com.futurice.cascade.Async;
import com.futurice.cascade.active.ImmutableValue;

/**
 * An extendable convenience for debugging asynchronous multithreaded code.
 * <p>
 * During debug builds, a stack trace will be generated and parsed asynchronously at the time this
 * object is created
 */
public interface IAsyncOrigin {
    @NonNull
    ImmutableValue<String> getOrigin();
}
