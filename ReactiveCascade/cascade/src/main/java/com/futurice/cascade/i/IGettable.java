/*
The MIT License (MIT)

Copyright (c) 2015 Futurice Oy and individual contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.futurice.cascade.i;

import android.support.annotation.Nullable;

/**
 * Create the actual URL at the last minute, just before the request is processed.
 * <p>
 * This may be useful for load balancing between servers or late-prioritizing parameters
 * based on current conditions. For example use this in association with a Collection to
 * prioritize if/which-next at that moment based on current user interface state.
 * <p>
 * Your implementation must be thread safe since multiple WORKER threads may attempt to start
 * network connections simultaneously. The simplest way to do this is mark the method synchronized
 * <p>
 * Return <code>null</code> if no URL should be loaded at this time. Depending on your use case
 * this may signal the end of a collection of URLs to be downloaded.
 *
 * @param <T>
 */
public interface IGettable<T> {
    /**
     * Get the current value of a variable or the next value in a list.
     *
     * @return the current value, next value in the series, or <code>null</code> of the series has ended
     */
    @Nullable
    public T get();
}
