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

import android.support.annotation.NonNull;

import java.util.concurrent.Callable;

/**
 * A lambda-friendly functional interface for continuation actions that receive no parameters
 * split return one result.
 *
 * @param <PHANTOM_IN>
 * @param <OUT>
 * @throws Exception
 */
public interface IActionR<PHANTOM_IN, OUT> extends Callable<OUT>, IBaseAction<PHANTOM_IN> {
    @Override // Callable
    @NonNull
    @nonnull
    OUT call() throws Exception;
}
