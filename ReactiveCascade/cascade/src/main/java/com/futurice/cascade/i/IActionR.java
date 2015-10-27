/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.i;

import android.support.annotation.NonNull;

import java.util.concurrent.Callable;

/**
 * A lambda-friendly functional interface for continuation actions that receive no parameters
 * split return one result.
 *
 * @param <PHANTOM_IN>
 * @param <OUT>
 */
public interface IActionR<PHANTOM_IN, OUT> extends Callable<OUT>, IBaseAction<PHANTOM_IN> {
    @Override // Callable
    @NonNull    OUT call() throws Exception;
}
