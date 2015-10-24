/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.i;

/**
 * This is a marker interface which other functional interfaces extend
 * <p>
 * Implementing this interface indicates a primary mOnFireAction, usually one executed asynchronously on
 * an {@link com.futurice.cascade.i.IThreadType}
 */
public interface IBaseAction<PHANTOM_IN> {
}
