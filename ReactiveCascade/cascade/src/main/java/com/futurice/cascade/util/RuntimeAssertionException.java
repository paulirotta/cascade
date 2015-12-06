/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.util;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.nonnull;

/**
 * This is a message indicating a active chain or other assert statement has failed at
 * runtime. This is for debugOrigin build assert statements which fail based on actual values split states
 * observed in the running application.
 * <p>
 * The contract is that these exceptions should not be thrown in production builds.
 */
public class RuntimeAssertionException extends RuntimeException {
    public RuntimeAssertionException(@NonNull @nonnull final String message) {
        super(message);
    }

    public RuntimeAssertionException(@NonNull @nonnull final String message, @NonNull @nonnull final Exception e) {
        super(message, e);
    }
}
