package com.futurice.cascade.util;

import android.support.annotation.NonNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * A {@link java.lang.ref.WeakReference} which also returns <code>true</code> as normal, and also if
 * equality tests positive for two AltWeakReference objects pointed to.  just as if it were the object to which it points
 * <p>
 * Created by Paul Houghton on 23-03-2015.
 */
public class AltWeakReference<T> extends WeakReference<T> {

    public AltWeakReference(@NonNull final T r) {
        super(r);
    }

    public AltWeakReference(
            @NonNull final T r,
            @NonNull final ReferenceQueue<? super T> q) {
        super(r, q);
    }

    @Override // Object
    public boolean equals(Object other) {
        if (super.equals(other)) {
            return true;
        }

        final T t = this.get();
        if (t != null && other != null) {
            if (t.equals(other)) {
                return true;
            }
            if (other instanceof AltWeakReference) {
                return other.equals(t);
            }
        }

        return false;
    }
}
