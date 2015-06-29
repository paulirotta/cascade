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

/**
 * AFile lambda-friendly continuation mOnFireAction which may be run in the future or on a different thread
 * similar to {@link java.lang.Runnable}. The differences is that an explicit <code>Exception</code>
 * may be thrown which helps facilitate asynchronous exception handling in a lambda-friendly manner.
 *
 */
public interface IAction<PHANTOM_IN> extends IBaseAction<PHANTOM_IN> {
    /**
     * Execute the mOnFireAction
     * <p>
     * If parameters need to be passed in, see for example {@link IActionOne}
     *
     * @throws Exception
     */
    void call() throws Exception;
}
