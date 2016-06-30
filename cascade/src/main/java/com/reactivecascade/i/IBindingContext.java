/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

import android.support.annotation.NonNull;

/**
 * A lifecycle for a reactive or functional relationship
 * <p>
 * The lifecycle starts as connected and may disconnect atomically after which point more results
 * will not start. Within any single thread, no new results will be returned after unbinding
 */
public interface IBindingContext {
    /**
     * Check if closed
     *
     * @return <code>true</code> if {@link #closeBindingContext()} has not been called
     */
    boolean isBindingContextOpen();

    /**
     * Trigger end of all binding context actions
     * <p>
     * Since a binding context can not be re-opened, you may want to create a new binding context
     * for cases where opening again is required. Items linked to the previous binding context will
     * continue to see the closed state.
     */
    void closeBindingContext();

    /**
     * Add an action to be performed before the binding context close finishes
     *
     * @param action
     * @return
     */
    void onClose(@NonNull IAction action);
}
