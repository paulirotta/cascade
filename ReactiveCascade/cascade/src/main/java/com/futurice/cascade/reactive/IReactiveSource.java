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
package com.futurice.cascade.reactive;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.nonnull;

/**
 * An object you can bind _from_ to receive updates each time this object decides to emit them.
 * <p>
 * A common implementation is {@link com.futurice.cascade.reactive.ReactiveValue} which will concurrently emit
 * to all its down-chain bindings each time it changes.
 * <p>
 * See also {@link IReactiveTarget}
 * <p>
 * Unlike the single-use objects in {@link com.futurice.cascade.i.active}, the chain created is
 * suitable for multiple firings. Chains of {@link IReactiveSource}
 * functions are usually held with a weak reference at their head. They will automatically collapse
 * and clean up the binding at the same time the leaf node is garbage collected. Any firing which started
 * before garbage collection may lead to a value which is discarded. The same is true even if you manually
 * {@link com.futurice.cascade.reactive.ReactiveValue#unsubscribe(String, IReactiveTarget)} so the automatic cleanup is equivalent.
 * Still, some people are paranoid, and others just like changing diapers and doing dirty plumbing work.
 * Feel free to do it manually. If you want to be controversial, do so in the {@link Object#finalize()}
 * method of the leaf node. It could be argued that if <code>.finalize()</code> was not called for
 * a long time, you didn't have a problem or leak in the first place so why worry.
 * <p>
 * If your reactive chain branches, be sure to prune just the branch of interest. A reactive chain is
 * said to "branch" when there are multiple <code>.subscribe()</code> statements
 * from a single <code>IReactiveSource</code>). The end node of a reactive chain is called the
 * "leaf node". The leaf node is "live" if it is strongly referenced and not free for garbage
 * collection. If the reactive chain branches, only that section
 * of the reactive function free which is no longer needed by a live leaf node will
 * collapse and be garbage collected to prevent useless firing. Check the logs for notification when this occurs.
 * <p>
 * If your reactive chain is strongly referenced in some other way, automatic cleanup will be defeated.
 * A typical way to do this is use an anonymous inner class instead of a lambda expression. Anonymous
 * inner classes from within the leaf node or its non-persistent child objects are not a problem.
 * <p>
 * A second even more sneaky way to defeat the automatic memory cleanup of unused reactive chains is: include
 * a closure reference in your lambda expressions to some object that will not be garbage collected
 * when the leaf nod of the reactive function chain is garbage collected. So you can freely
 * incorporate closures into your lambda and not manually call {@link com.futurice.cascade.reactive.ReactiveValue#unsubscribe(String, IReactiveTarget)}
 * <em>only</em> if the closures reference objects that will not hang around past the end of the
 * useful life of the reactive chain.
 *
 * @param <OUT>
 */
public interface IReactiveSource<OUT> extends INamed {
    //TODO Add .subscribe(onFireAction..) list versions for convenience

//    /**
//     * NOTE: Firing is always asynchronous if the down-chain IThreadType is not the same as this.threadType
//     */
//    public static enum FireMode {
//        /**
//         * Default behaviour
//         * <p>
//         * <p>
//         * Concurrent if there are multiple down-chain {@link com.futurice.cascade.reactive.IReactiveTarget}s
//         * <p>
//         * If there is only one down-chain target and the {@link com.futurice.cascade.i.IThreadType} is the
//         * same, this will continue on the same worker without context switching overhead. In such as case,
//         * previous chain steps will not be garbage collectable until the fire completes or reaches
//         * a down-chain branch.
//         */
//        ASYNCHRONOUS,
//        /**
//         * Synchronous sequential fire even if there are multiple targets with this same IThreadType.
//         * <p>
//         * The calling sequence is the same as the order in which targets were attached to this node.
//         */
//        SYNCHRONOUS
//    }
//
//    ;

    /**
     * Remove a down-chain branch from the reactive function tree at this node
     *
     * @param reason
     * @param reactiveTarget
     * @return <code>true</code> if the branch was found and removed
     */
    boolean unsubscribe(String reason, IReactiveTarget<OUT> reactiveTarget);

    /**
     * Remove all down-chain branches from this node of the reactive function tree
     */
    void unsubscribeAll(String reason);

    /**
     * Attach a downstream <code>.subscribe()</code> like {@link #split(IReactiveTarget)}
     * as a {@link java.lang.ref.WeakReference}
     * for automatic down-chain {@link #unsubscribe(String, IReactiveTarget)} on garbage collect of reactive chain leaf node.
     *
     * @param reactiveTarget
     * @return
     */
    IReactiveSource<OUT> split(IReactiveTarget<OUT> reactiveTarget);

    //TODO revisit the use cases for a merge function in async (Not the same as RX zip)
//    /**
//     * Inject the reactive data stream like {@link #merge(IReactiveSource)} as a {@link java.lang.ref.WeakReference}
//     * for automatic down-chain {@link #unsubscribe(String, IReactiveTarget)} on garbage collect of reactive chain leaf node.
//     *
//     * @param upchainReactiveSource
//     * @param <UPCHAIN_OUT>
//     * @return
//     */
//    <UPCHAIN_OUT> IReactiveSource<OUT> merge(IReactiveSource<UPCHAIN_OUT> upchainReactiveSource);

    /**
     * Add an onFireAction as a new branch down-chain from this node.
     * <p>
     * It will run on the same {@link com.futurice.cascade.i.IThreadType} as this node and may be called synchronously.
     * <p>
     * If this <code>onFireAction</code> is a lambda with closure references to a surrounding {@link java.lang.Object}
     * context, subscribe the onFireAction will automatically {@link #unsubscribe(String, IReactiveTarget)} when the surrounding
     * context is garbage collected <em>and</em> any down-chain {@link IReactiveTarget}
     * is garbage collected.
     *
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    IReactiveSource<OUT> subscribe(@NonNull @nonnull IAction<OUT> action);

    /**
     * Add an onFireAction as a new branch down-chain from this node.
     * <p>
     * It will run on the same {@link com.futurice.cascade.i.IThreadType} as this node and may be called synchronously.
     * <p>
     * If this <code>onFireAction</code> is a lambda with closure references to a surrounding {@link java.lang.Object}
     * context, subscribe the onFireAction will automatically {@link #unsubscribe(String, IReactiveTarget)} when the surrounding
     * context is garbage collected <em>and</em> any down-chain {@link IReactiveTarget}
     * is garbage collected.
     *
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    IReactiveSource<OUT> subscribe(@NonNull @nonnull IActionOne<OUT> action);

    /**
     * Add an onFireAction as a new branch down-chain from this node.
     * <p>
     * It will run on the same {@link com.futurice.cascade.i.IThreadType} as this node and may be called synchronously.
     * <p>
     * If this <code>onFireAction</code> is a lambda with closure references to a surrounding {@link java.lang.Object}
     * context, subscribe the onFireAction will automatically {@link #unsubscribe(String, IReactiveTarget)} when the surrounding
     * context is garbage collected <em>and</em> any down-chain {@link IReactiveTarget}
     * is garbage collected.
     *
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @nonnull
    <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribeMap(@NonNull @nonnull IActionOneR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Add an onFireAction as a new branch down-chain from this node.
     *
     * @param threadType The onFireAction will be called asynchronously unless the this is same {@link com.futurice.cascade.i.IThreadType} as this (upchain) node in the reactive function tree.
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    IReactiveSource<OUT> subscribe(@NonNull @nonnull IThreadType threadType, @NonNull @nonnull IAction<OUT> action);

    /**
     * Add an onFireAction as a new branch down-chain from this node.
     *
     * @param threadType The onFireAction will be called asynchronously unless the this is same {@link com.futurice.cascade.i.IThreadType} as this (upchain) node in the reactive function tree.
     * @param action
     * @return
     */
    @NonNull
    @nonnull
    IReactiveSource<OUT> subscribe(@NonNull @nonnull IThreadType threadType, @NonNull @nonnull IActionOne<OUT> action);

    /**
     * Add an onFireAction as a new branch down-chain from this node.
     *
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @nonnull
    <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribe(@NonNull @nonnull IActionR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Add an onFireAction as a new branch down-chain from this node.
     *
     * @param threadType
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @nonnull
    <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribe(@NonNull @nonnull IThreadType threadType, @NonNull @nonnull IActionR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Add an onFireAction as a new branch down-chain from this node.
     *
     * @param threadType      The onFireAction will be called asynchronously unless the this is same {@link com.futurice.cascade.i.IThreadType} as this (upchain) node in the reactive function tree.
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    @nonnull
    <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subscribeMap(@NonNull @nonnull IThreadType threadType, @NonNull @nonnull IActionOneR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Add an onFireAction as a new branch down-chain from this node.
     * <p>
     * It will run on the same {@link com.futurice.cascade.i.IThreadType} as this node and may be called synchronously.
     *
     * @param reactiveTarget
     * @return
     */
    @NonNull
    @nonnull
    IReactiveSource<OUT> subscribe(@NonNull @nonnull IReactiveTarget<OUT> reactiveTarget);
}
