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
package com.futurice.cascade.reactive;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IActionOneR;
import com.futurice.cascade.i.exception.IOnErrorAction;
import com.futurice.cascade.i.reactive.IReactiveSource;

/**
 * This is the default implementation for a reactive functional chain link.
 * <p>
 * NOTE: Because there _may_ exist a possibility of multiple fire events racing each other on different
 * threads, it is important that the functions in the reactive chain are idempotent and stateless.
 * <p>
 * <code>Subscription</code>s are both an {@link com.futurice.cascade.i.reactive.IReactiveTarget} and
 * {@link com.futurice.cascade.i.reactive.IReactiveSource}.
 * <p>
 *
 * @param <OUT>
 * @param <IN>  the type of the second link in the functional chain
 */
@NotCallOrigin
public class Subscription<IN, OUT> extends AbstractSubscription<IN, OUT> {
    private final IReactiveSource<IN> upchainReactiveSource; // This is held to keep the chain from being garbage collected until the tail of the chain is de-referenced

    /**
     * Create a new default implementation of a reactive functional chain link
     * <p>
     * If there are multiple down-chain targets attached to this node, it will concurrently fire
     * all down-chain branches.
     *
     * @param threadType
     * @param name
     * @param upchainReactiveSource
     * @param action                Because there _may_ exist a possibility of multiple fire events racing each other on different
     *                              threads, it is important that the action functions in the reactive chain are idempotent and stateless. Further analysis is needed, but be cautious.
     * @param onError
     */
    public Subscription(@NonNull IThreadType threadType,
                        @NonNull String name,
                        @NonNull IReactiveSource<IN> upchainReactiveSource,
                        @NonNull IActionOneR<IN, OUT> action,
                        @NonNull IOnErrorAction onError) {
        super(threadType, name, onError, action);

        this.upchainReactiveSource = upchainReactiveSource;
        upchainReactiveSource.subscribe(this);
    }
}
