/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.i;

/**
 * A lambda-friendly continuation mOnFireAction which may be run in the future or on a different thread
 * similar to {@link java.lang.Runnable}. The differences is that an explicit <code>Exception</code>
 * may be thrown which helps facilitate asynchronous exception handling in a lambda-friendly manner.
 */
public interface IAction<PHANTOM_OUT> extends IBaseAction<PHANTOM_OUT> {
    /**
     * Execute the mOnFireAction
     * <p>
     * If parameters need to be passed in, see for example {@link IActionOne}
     *
     * @throws Exception to transition to {@link com.futurice.cascade.i.IAltFuture.StateError}
     * @throws java.util.concurrent.CancellationException to {@link com.futurice.cascade.i.IAltFuture.StateCancelled}
     */
    void call() throws Exception;
}
