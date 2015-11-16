/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.i;

import android.support.annotation.NonNull;

/**
 * A function to run in the event of an {@link java.lang.Exception} or similar irregular termination
 * such as {@link com.futurice.cascade.i.ICancellable#cancel(String)}
 * <p>
 * Perform some cleanup or notification mOnFireAction to bring this object into a rest state after
 * irregular termination.
 * @throws Exception
 */
public interface IOnErrorAction extends IActionOne<Exception> {
}
