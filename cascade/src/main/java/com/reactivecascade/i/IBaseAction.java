/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

/**
 * This is a marker interface which other functional interfaces extend
 * <p>
 * Implementing this interface indicates a primary mOnFireAction, usually one executed asynchronously on
 * an {@link com.reactivecascade.i.IThreadType}
 *
 * @param <T> is a parameter that apparently helps generics disambituate at least in some IDEs
 */
public interface IBaseAction<T> {
}
