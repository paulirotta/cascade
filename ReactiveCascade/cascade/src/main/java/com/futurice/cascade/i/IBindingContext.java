package com.futurice.cascade.i;

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
     *
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
    IBindingContext onClose(@NonNull IAction action);
}
