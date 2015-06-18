/*
 * Copyright (c) 2015 Futurice GmbH. All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.futurice.cascade.i.reactive;

import com.futurice.cascade.i.*;

/**
 * An object which can receive repeated value assertions from a reactive active chain.
 * <p>
 * Commonly used default implementations are {@link com.futurice.cascade.reactive.Subscription} and
 * {@link com.futurice.cascade.reactive.ReactiveValue}.
 * <p>
 * Note that classes which are functional but not reactive only receive a value one time before
 * becoming immutable value object. These are thus _not_ <code>IReactiveTarget</code>s because the
 * 2nd etc firing would not be meaningful. You will see example of these in the package
 * {@link com.futurice.cascade.i.active}
 *
 * @param <IN>     the type of value expected as input to this node. If this is the head of the reactive chain,
 *                 it is the same type as <code>SOURCE</code>.
 */
public interface IReactiveTarget<IN> extends INamed {
    /**
     * An upchain {@link com.futurice.cascade.i.reactive.IReactiveSource} is notifying us
     * of a new input value to process.
     *
     * You don't usually call the fire methods directly. They are
     * called for you when this is an input or state change, for example by
     * {@link com.futurice.cascade.reactive.ReactiveValue#set(Object)}
     *
     * <p>
     * Nodes within a reactive chain may choose to be stateless such as
     * {@link com.futurice.cascade.reactive.Subscription}.
     * <p>
     * Nodes at the leaf of a reactive chain will typically retain this value as state, for example
     * ({@link com.futurice.cascade.reactive.ReactiveValue}).
     * <p>
     * They may also choose to otherwise create a stateful side effect with the value received. The
     * recommended practice is to not create such side effects except in the leaf node(s) at the
     * end of chain branches. If you violate this rule, be warned you tend to end up with a
     * hairy memory management and partially-completed-stateful-logic-before-exception error
     * correction mess.
     * <p>
     * If you would like to unlink this object when the fire operation results in an error, you must
     * do so in an <code>.onError()</code> statement lower down in the chain you wish to trigger
     * such an action.
     *
     * @param in               The value from the previous link in the reactive chain.
     */
    void fire(IN in);

    /**
     * Same as {@link #fire(Object)} however this is queued LIFO for more immediate execution.
     *
     * You do not generally call this directly. Chains call this for you. It is called instead of {@link #fire(Object)} mid-chain to reduce
     * work in progress and complete the task tree is an depth-first manner. This reduces the latency of
     * individual chains once execution starts. It also decreases the peak memory load by more rapidly
     * flushing intermediate values to the leaf node side effects of the chain.
     *
     * @param in
     */
    void fireNext(IN in);

    /**
     * Notification that an {@link com.futurice.cascade.i.reactive.IReactiveSource}  will start sending updates
     *
     * This allows the target which is responsible for holding a strong reference to the source to
     * prevent it from being garbage collected until all targets of a given source go out of scope and
     * are themselves garbage collected.
     *
     * You may manually speed this process by calling {@link #unsubscribeSource(String, IReactiveSource)},
     * however if you choose not to or forget to do so, it will be taken care of for you fairly soon.
     *
     * @param reactiveSource
     */
    void subscribeSource(String reason, IReactiveSource<IN> reactiveSource);

    /**
     * Notification that an {@link com.futurice.cascade.i.reactive.IReactiveSource}  will no longer send updates
     *
     * Since this target is responsible for holding a reference to the source to keep it from
     * being garbage collected, this target can not forget the source and, if not used elsewhere, it can be garbage collected.
     *
     * @param reason
     * @param reactiveSource
     */
    void unsubscribeSource(String reason, IReactiveSource<IN> reactiveSource);

    /**
     * Remove all up-chain branches from this node of the reactive function tree
     */
    void unsubscribeAllSources(String reason);
}
