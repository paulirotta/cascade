/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.active;

import android.support.annotation.NonNull;

/**
 * This is a marker interface. If you return state information, the atomic inner state of your
 * implementation should implement this interface.
 */
public interface IAltFutureState {
    /**
     * Get the exception which triggered this state change
     *
     * @return
     */
    @NonNull
    public Exception getException();
}
