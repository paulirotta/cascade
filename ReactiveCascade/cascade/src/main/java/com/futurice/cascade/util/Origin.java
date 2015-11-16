package com.futurice.cascade.util;

import android.support.annotation.NonNull;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.IAsyncOrigin;
import com.futurice.cascade.i.NotCallOrigin;

/**
 * A convenience class for tracking the point at which asychronous objects are created
 * <p>
 * The actual work of parsing the stack trace is slow due to introspection. This is delayed and will
 * not be performed until <code>{@link #mOrigin}.get()</code> is called.
 */
@NotCallOrigin
public class Origin implements IAsyncOrigin {
    private final ImmutableValue<String> mOrigin = CLog.originAsync();

    @NonNull
    @Override
    @NotCallOrigin
    public ImmutableValue<String> getOrigin() {
        return mOrigin;
    }
}
