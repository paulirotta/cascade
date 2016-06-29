package com.reactivecascade.functional;

import android.util.Pair;

/**
 * Created by Paul Houghton on 3/28/2015.
 */
public final class ConcurrentIteratorItem<T> extends Pair<Integer, T> {
    /**
     * Constructor for a Pair.
     *
     * @param index  the first object in the Pair
     * @param value the second object in the pair
     */
    public ConcurrentIteratorItem(Integer index, T value) {
        super(index, value);
    }
}
