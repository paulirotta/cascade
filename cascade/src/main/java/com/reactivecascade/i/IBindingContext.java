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
public interface IBindingContext<T> {
    /**
     * Check if closed
     *
     * @return <code>true</code> if {@link #closeBindingContext(Object)} has not been called
     */
    boolean isOpen();

    /**
     * Trigger start of all binding context actions
     */
    void openBindingContext(T t);

    /**
     * Trigger end of all binding context actions
     */
    void closeBindingContext(T t);

    /**
     * Add an action to be performed synchronously before the binding context open finishes
     *
     * @param action
     * @return
     */
    void onOpen(@NonNull IActionOne<T> action);

    /**
     * Add an action to be performed synchronously before the binding context close finishes
     *
     * @param action
     * @return
     */
    void onClose(@NonNull IActionOne<T> action);
}
