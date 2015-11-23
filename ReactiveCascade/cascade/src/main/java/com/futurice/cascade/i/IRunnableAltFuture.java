/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.i;

import com.futurice.cascade.functional.RunnableAltFuture;

/**
 * A future value which will execute an {@link IAction} before the value is determined. The output
 * of the action may determine the value, or the {@link #getUpchain()} value is used if no value is
 * output by the action.
 *
 * See also {@link ISettableAltFuture}
 */
public interface IRunnableAltFuture<IN, OUT> extends IAltFuture<IN, OUT>, Runnable {
}
