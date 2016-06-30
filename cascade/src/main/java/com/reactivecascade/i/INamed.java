/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

import android.support.annotation.NonNull;

public interface INamed {
    /**
     * A descriptive name to assist the developer with debugging
     *
     * @return object name suitable for easy debugging
     */
    @NonNull
    String getName();
}
