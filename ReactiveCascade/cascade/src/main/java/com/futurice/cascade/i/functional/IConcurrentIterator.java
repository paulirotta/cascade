package com.futurice.cascade.i.functional;

import com.futurice.cascade.functional.ConcurrentIteratorItem;
import com.futurice.cascade.i.ICancellable;

import java.util.NoSuchElementException;

/**
 * Created by Paul Houghton on 3/26/2015.
 */
public interface IConcurrentIterator<T> extends ICancellable {
    /**
     * Get the next element after registering a {@link com.futurice.cascade.i.functional.IConcurrentIterator.StateListener}
     * and receiving <code>HAS_NEXT</code>.
     *
     * If the list is of finite length, the listener notification of transition to state <code>FINISHED</code> will occur
     * after the last value has been fetched. Since this transition may involve multiple threads, a
     * race condition may result in {@link java.util.NoSuchElementException} which can be treated
     * as equivalent to <code>SLEEP</code> which signals that the listener should pause reading items
     * until it receives the next state change notification.
     *
     * @return
     * @throws {@link java.util.NoSuchElementException} if state is anything other than <code>HAS_NEXT</code>.
     */
    ConcurrentIteratorItem<T> next() throws NoSuchElementException;

    void addStateListener(StateListener stateListener);

    enum State {HAS_NEXT, SLEEP, FINISHED, CANCELLED}

    interface StateListener {
        void stateChange(State state);
    }
}
