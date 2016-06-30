/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

/**
 * A future value which is determined externally by a call to {@link #set(Object)}
 * <p>
 * See also {@link IRunnableAltFuture}
 */
public interface ISettableAltFuture<T> extends IAltFuture<T, T>, ISettable<T> {
}
