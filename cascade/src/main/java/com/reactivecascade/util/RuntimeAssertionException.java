/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.support.annotation.NonNull;

/**
 * This is a message indicating a active chain or other assert statement has failed at
 * runtime. This is for debugOrigin build assert statements which fail based on actual values split states
 * observed in the running application.
 * <p>
 * The contract is that these exceptions should not be thrown in production builds.
 */
public class RuntimeAssertionException extends RuntimeException {
    public RuntimeAssertionException(@NonNull String message) {
        super(message);
    }

    public RuntimeAssertionException(@NonNull String message,
                                     @NonNull final Exception e) {
        super(message, e);
    }
}
