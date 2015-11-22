package com.futurice.cascade.i;

/**
 * A future value which is determined externally by a call to {@link #set(Object)}
 *
 * See also {@link IRunnableAltFuture}
 */
public interface ISettableAltFuture<IN, OUT> extends IAltFuture<IN, OUT>, ISettable<IN> {
}
