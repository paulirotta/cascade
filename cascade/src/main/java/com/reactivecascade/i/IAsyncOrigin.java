/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

import android.support.annotation.NonNull;

import com.reactivecascade.functional.ImmutableValue;

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
