/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.support.annotation.NonNull;

import com.reactivecascade.functional.ImmutableValue;
import com.reactivecascade.i.IAsyncOrigin;
import com.reactivecascade.i.NotCallOrigin;

/**
 * A convenience class for tracking the point at which asychronous objects are created
 * <p>
 * The actual work of parsing the stack trace is slow due to introspection. This is delayed and will
 * not be performed until <code>{@link #mOrigin}.get()</code> is called.
 */
@NotCallOrigin
public abstract class Origin implements IAsyncOrigin {
    private final ImmutableValue<String> mOrigin = RCLog.originAsync();

    @NonNull
    @Override // IAsyncOrigin
    @NotCallOrigin
    public ImmutableValue<String> getOrigin() {
        return mOrigin;
    }

    @NonNull
    @Override // Object
    public String toString() {
        return mOrigin.get();
    }
}
