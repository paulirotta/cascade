/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

import android.support.annotation.NonNull;

/**
 * An object you can bind _from_ to receive updates each time this object decides to emit them.
 * <p>
 * A common implementation is {@link com.reactivecascade.reactive.ReactiveValue} which will concurrently emit
 * to all its down-chain bindings each time it changes.
 * <p>
 * See also {@link IReactiveTarget}
 * <p>
 * Unlike the single-use functional (as opposed to "reactive") objects, the chain created is
 * suitable for multiple firings. Chains of {@link IReactiveSource}
 * functions are usually held with a weak reference at their head. They will automatically collapse
 * and clean up the binding at the same time the leaf node is garbage collected. Any firing which started
 * before garbage collection may lead to a from which is discarded. The same is true even if you manually
 * {@link com.reactivecascade.reactive.ReactiveValue#unsubscribe(String, IReactiveTarget)} so the automatic cleanup is equivalent.
 * Still, some people are paranoid, and others just like changing diapers and doing dirty plumbing work.
 * Feel free to do it manually. If you want to be controversial, do so in the {@link Object#finalize()}
 * method of the leaf node. It could be argued that if <code>.finalize()</code> was not called for
 * a long time, you didn't have a problem or leak in the first place so why worry.
 * <p>
 * If your reactive chain branches, be sure to prune just the branch of interest. A reactive chain is
 * said to "branch" when there are multiple <code>.sub()</code> statements
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
 * incorporate closures into your lambda and not manually call {@link com.reactivecascade.reactive.ReactiveValue#unsubscribe(String, IReactiveTarget)}
 * <em>only</em> if the closures reference objects that will not hang around past the end of the
 * useful life of the reactive chain.
 *
 * @param <OUT>
 */
public interface IReactiveSource<OUT> extends INamed {
    //TODO Add .sub(mOnFireAction..) list versions for convenience

    /**
     * Remove a down-chain branch from the reactive function tree at this node
     *
     * @param reason
     * @param reactiveTarget
     * @return <code>true</code> if the branch was found and removed
     */
    boolean unsubscribe(@NonNull String reason, @NonNull IReactiveTarget<OUT> reactiveTarget);

    /**
     * Remove all down-chain branches from this node of the reactive function tree
     */
    void unsubscribeAll(@NonNull String reason);

    /**
     * Attach a downstream <code>.sub()</code> like {@link #split(IReactiveTarget)}
     * as a {@link java.lang.ref.WeakReference}
     * for automatic down-chain {@link #unsubscribe(String, IReactiveTarget)} on garbage collect of reactive chain leaf node.
     *
     * @param reactiveTarget
     * @return
     */
    IReactiveSource<OUT> split(@NonNull IReactiveTarget<OUT> reactiveTarget);

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
     * Add an mOnFireAction as a new branch down-chain from this node.
     * <p>
     * It will run on the same {@link com.reactivecascade.i.IThreadType} as this node and may be called synchronously.
     * <p>
     * If this <code>mOnFireAction</code> is a lambda with closure references to a surrounding {@link java.lang.Object}
     * context, sub the mOnFireAction will automatically {@link #unsubscribe(String, IReactiveTarget)} when the surrounding
     * context is garbage collected <em>and</em> any down-chain {@link IReactiveTarget}
     * is garbage collected.
     *
     * @param action
     * @return
     */
    @NonNull
    IReactiveSource<OUT> sub(@NonNull IAction<OUT> action);

    /**
     * Add an mOnFireAction as a new branch down-chain from this node.
     * <p>
     * It will run on the same {@link com.reactivecascade.i.IThreadType} as this node and may be called synchronously.
     * <p>
     * If this <code>mOnFireAction</code> is a lambda with closure references to a surrounding {@link java.lang.Object}
     * context, sub the mOnFireAction will automatically {@link #unsubscribe(String, IReactiveTarget)} when the surrounding
     * context is garbage collected <em>and</em> any down-chain {@link IReactiveTarget}
     * is garbage collected.
     *
     * @param action
     * @return
     */
    @NonNull
    IReactiveSource<OUT> sub(@NonNull IActionOne<OUT> action);

    /**
     * Add an mOnFireAction as a new branch down-chain from this node.
     * <p>
     * It will run on the same {@link com.reactivecascade.i.IThreadType} as this node and may be called synchronously.
     * <p>
     * If this <code>mOnFireAction</code> is a lambda with closure references to a surrounding {@link java.lang.Object}
     * context, sub the mOnFireAction will automatically {@link #unsubscribe(String, IReactiveTarget)} when the surrounding
     * context is garbage collected <em>and</em> any down-chain {@link IReactiveTarget}
     * is garbage collected.
     *
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subMap(@NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Add an mOnFireAction as a new branch down-chain from this node.
     *
     * @param threadType The mOnFireAction will be called asynchronously unless the this is same {@link com.reactivecascade.i.IThreadType} as this (upchain) node in the reactive function tree.
     * @param action
     * @return
     */
    @NonNull
    IReactiveSource<OUT> sub(@NonNull IThreadType threadType,
                             @NonNull IAction<OUT> action);

    /**
     * Add an mOnFireAction as a new branch down-chain from this node.
     *
     * @param threadType The mOnFireAction will be called asynchronously unless the this is same {@link com.reactivecascade.i.IThreadType} as this (upchain) node in the reactive function tree.
     * @param action
     * @return
     */
    @NonNull
    IReactiveSource<OUT> sub(@NonNull IThreadType threadType,
                             @NonNull IActionOne<OUT> action);

    /**
     * Add an mOnFireAction as a new branch down-chain from this node.
     *
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> sub(@NonNull IActionR<DOWNCHAIN_OUT> action);

    /**
     * Add an mOnFireAction as a new branch down-chain from this node.
     *
     * @param threadType
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> sub(@NonNull IThreadType threadType,
                                                       @NonNull IActionR<DOWNCHAIN_OUT> action);

    /**
     * Add an mOnFireAction as a new branch down-chain from this node.
     *
     * @param threadType      The mOnFireAction will be called asynchronously unless the this is same {@link com.reactivecascade.i.IThreadType} as this (upchain) node in the reactive function tree.
     * @param action
     * @param <DOWNCHAIN_OUT>
     * @return
     */
    @NonNull
    <DOWNCHAIN_OUT> IReactiveSource<DOWNCHAIN_OUT> subMap(@NonNull IThreadType threadType,
                                                          @NonNull IActionOneR<OUT, DOWNCHAIN_OUT> action);

    /**
     * Add an mOnFireAction as a new branch down-chain from this node.
     * <p>
     * It will run on the same {@link com.reactivecascade.i.IThreadType} as this node and may be called synchronously.
     *
     * @param reactiveTarget
     * @return
     */
    @NonNull
    IReactiveSource<OUT> sub(@NonNull IReactiveTarget<OUT> reactiveTarget);
}
