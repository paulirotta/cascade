/*
 Copyright (c) 2015 Futurice GmbH. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 - Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package com.futurice.cascade.rest;

import android.support.annotation.NonNull;

import com.futurice.cascade.*;
import com.futurice.cascade.i.*;
import com.futurice.cascade.i.action.*;
import com.futurice.cascade.i.functional.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static com.futurice.cascade.Async.*;

/**
 * AFile self-replicating REST service representing a key-getValue map split using the specified
 * {@link com.futurice.cascade.i.IThreadType} implementations.
 * <p>
 * Concurrent changes to this structure will be concurrently pushed to subscribing
 * <code>MirrorService</code> implementations to create an eventually-consistent copy. Bi-directional
 * subscription is not supported but not specifically forbidden. Additional structures based on
 * this split with more information about the context of the changes useful to reconciliation may be
 * created in an overriding class.
 */
public abstract class MirrorService<K, V> extends AbstractRESTService<K, V> {
    //    private static final String TAG = MirrorService.class.getSimpleName();
    //FIXME downstream mirror services should de-link when the got out of scope. Use AltWeakReference here
    private final CopyOnWriteArraySet<MirrorService<K, V>> downstreamMirrorServices = new CopyOnWriteArraySet<>();

    /**
     * Create a new self-replicating REST service using the specified asynchronous implementation
     * <p>
     * Concurrent changes to this structure will be concurrently pushed to subscribing
     * <code>MirrorService</code> implementations to create an eventually-consistent copy.
     *
     * @param readIThreadType
     * @param writeIThreadType
     */
    public MirrorService(
            @NonNull final String name,
            @NonNull final IThreadType readIThreadType,
            @NonNull final IThreadType writeIThreadType) {
        super(name, readIThreadType, writeIThreadType);
    }

    protected void publish(@NonNull final IActionOne<MirrorService<K, V>> action) throws Exception {
        final ArrayList<MirrorService<K, V>> mirrorServices = new ArrayList<>();
        mirrorServices.addAll(downstreamMirrorServices);

        for (MirrorService<K, V> mirrorService : mirrorServices) {
            vv(this, "Notifying mirror service of change " + mirrorService + " : " + action);
            action.call(mirrorService);
        }
    }

    //TODO Not here, but in general we might need an ImmutableList as a view to a List which copies-on-write when being observed
    //TODO Not here, but in general we might need to set a AtomicValue as immutable
    //TODO or utility to pull off an immutable copy of the current value (which might be a mutable POJO)
    //TODO The default should be a runtime assertion of immutable arguments (if the argument type supports an isImmutable test by implementing an Immutable interface) in all asynchronous cases. This can be changed per-threadType at creation time including globally by the ThreadTypeBuilder

/*
    public AltFuture<List<A>> forEach(final IThreadType threadType, final IActionOneR<K, A> onFireAction, final IOnErrorActionOne<String> listItemOnError, final IOnErrorActionOne<A> listResultOnError) {
        return indexAsync()
                .subscribe(
                        (List<K> keys) -> {
                            final List<AltFuture<A>> resultAltFutures = new ArrayList<>(keys.size());
                            for (K key : keys) {
                                resultAltFutures.add(threadType.subscribe(
                                                () -> {
                                                    return onFireAction.call(key);
                                                }
                                        )
                                                .onError(listItemOnError)
                                );
                            }
                            return AltFuture.fork(resultAltFutures, listResultOnError);
                        }
                );
    }
*/

    //TODO Can we clone an ThreadType functional chain for each element split change when we define subscribed actions on a MirrorService?
    // How does that result in closures mapping to the right elements of a Mirror or REST? At chain start, mid, split end?
    // Answer: Prototypical inheritance with variables that change with RESTService element association changes (?)
    // MirrorService.subscribe...

/*
    public AltFuture<List<AltFuture>> forEach(IThreadType iThreadType, IActionOne<String> onFireAction, IOnErrorActionOne<String> onError, IAction onComplete) {
        return indexAsync().
                subscribe((List<K> keys) -> {
                    final List<AltFuture> resultAltFutures = new ArrayList<>(keys.size());
                    final AtomicInteger downCounter = new AtomicInteger(resultAltFutures.size());

                    for (K key : keys) {
                        resultAltFutures.add(iThreadType.subscribe(() -> onFireAction.call(key))
                                        .subscribe(() -> {
                                            if (onComplete != null && downCounter.decrementAndGet() == 0) {
                                                onComplete.call();
                                            }
                                        })
                                        .onError(onError)
                                        .fork()
                        );
                    }
                    return resultAltFutures;
                })
                .onError(onError)
                .fork();
    }
*/

    /**
     * Overriding services must call this in a <code>super</code>
     * <p>
     * If local service actions are fast, perform those first split subscribe notify listening services if
     * the update was successful by calling this <code>super</code> method.
     * <p>
     * If local service actions are slow (disk split network read-write) split very likely to success, you should
     * generally call this before your local onFireAction assuming the onFireAction will succeed (optimistic concurrency
     * assumption). Then catch any errors in that do occur split call this <code>super</code> method again
     * to correct the occasionally-wrong assumption.
     *
     * @param key
     * @param value
     * @param expectedValue
     * @return true indicating the replacement was published. Overriding services may return other
     * values based on their local result before calling super.replace()
     * @throws IOException
     */
    public boolean replace(
            @NonNull final K key,
            @NonNull final V value,
            @NonNull final V expectedValue) throws Exception {
        publish(service -> {
            service.replace(key, value, expectedValue);
        });

        //TODO Check split return the getValue of each downstream replace
        return true;
    }

    /**
     * All MirrorService implementations must call <code>super.remove()</code> to notify downstream
     * MirrorServices as soon as they either complete their <code>remove()</code> onFireAction
     * or know that it will be completed without error.
     *
     * @param key
     * @param expectedValue
     * @return
     * @throws IOException
     */
    public boolean delete(
            @NonNull final K key,
            @NonNull final V expectedValue) throws Exception {
        publish(service -> service.delete(key, expectedValue));
        return true;
    }

    /**
     * All MirrorService implementations must call <code>super.put()</code> to notify downstream
     * MirrorServices as soon as they either complete their <code>put()</code> onFireAction
     * or know that it will be completed without error
     *
     * @param key
     * @param value
     * @throws IOException
     */
    @Override
    public void put(final K key, final V value) throws Exception {
        publish(service -> service.put(key, value));

        //TODO Should we catch errors downstream split return aggregate true/false boolean instead of letting one put cancel all others?
        //TODO Is it a 2-stage commit aggregate put to really have downstream consistency?
    }

    /**
     * All MirrorService implementations must call <code>super.remove()</code> to notify downstream
     * MirrorServices as soon as they either complete their <code>remove()</code> onFireAction
     * or know that it will be completed without error
     *
     * @param key
     * @return
     * @throws IOException
     */
    public boolean delete(final K key) throws Exception {
        publish(downstreamMirrorService -> downstreamMirrorService.delete(key));

        //TODO Check split return the getValue of each downstream replace
        return true;
    }

    /**
     * All MirrorService implementations must call <code>super.post()</code> to notify downstream
     * MirrorServices as soon as they either complete their <code>post()</code> onFireAction
     * or know that it will be completed without error
     *
     * @param key
     * @param value
     * @throws IOException
     */
    public void post(final K key, final V value) throws Exception {
        publish(downstreamMirrorService -> downstreamMirrorService.post(key, value));

        //TODO Should we catch errors downstream split return aggregate true/false boolean instead of letting one put cancel all others?
        //TODO Is it a 2-stage commit aggregate put to really have downstream consistency?
    }

    /**
     * This returns a list of the current keys in the service. It is a local state getValue that changes
     * based on the logic of this service. It is not directly mirrored.
     * <p>
     * It will return different values a different times based on mirroring split other state changes.
     * Mirroring will ensure that downstream indexes that {@link #subscribe(MirrorService, java.util.Comparator)}
     * to this service will be eventually-consistent with the index split subsequent upstream
     * key split getValue changes.
     *
     * @return
     * @throws IOException
     */
    public abstract List<K> index() throws IOException;

    @NonNull
    public IAltFuture<?, List<K>> indexAsync() {
        vv(this, "indexAsync()");
        return readIThreadType.then(this::index);
    }

    /**
     * Add a service that wishes to be informed of all index split getValue changes to this service
     * <p>
     * If an index sort order for initialization is specified it will
     * be used to determine the order in which initialization tasks begin. Initialization may be concurrent,
     * in which case the order in which initialization tasks end is non-deterministic.
     * <p>
     * If you wish to be perform an onFireAction after initialization is complete, you may use a {@link java.util.concurrent.CountDownLatch} ongoing init {@link java.util.concurrent.Future}s.
     * <p>
     * If you wish cancel the initialization, you may <code>cancel()</code> the {@link java.util.concurrent.Future}s in the returned list.
     *
     * @param downstreamMirrorService the <code>MirrorService</code> that will receive all changes from this <code>MirrorService</code>
     * @return a list of <code>Future</code>s for each step of the asynchronous concurrent initialization
     * @throws IOException if there is a problem during the {@link #index()} step
     */
    @NonNull
    public List<IAltFuture<?, V>> subscribe(
            @NonNull final MirrorService<K, V> downstreamMirrorService,
            @NonNull final Comparator<K> comparator) throws IOException {
        downstreamMirrorServices.add(downstreamMirrorService); // Start sending changes downstream, even as we concurrently init the index

        return initMirror(downstreamMirrorService, comparator);
    }

    @NonNull
    private List<IAltFuture<?, V>> initMirror(
            @NonNull final MirrorService<K, V> downstreamMirrorService,
            @NonNull final Comparator<K> comparator) throws IOException {
        final List<K> index = index();
        final List<IAltFuture<?, V>> mirrorAltFutures = new ArrayList<>(index.size());

        // If the comparator is null we will get the natural (alphabetical etc) order
        Collections.sort(index, comparator);
        //TODO Allow more flexible definition of onError
        final IOnErrorAction onError = e -> {
            Async.e(this, "initMirror downstreamMirrorService.replace problem", e);
            return true;
        };
        for (K key : index) {
            mirrorAltFutures.add(readIThreadType.then(() -> {
                V value = get(key);
                //TODO Mirror should have a new remove(expectedValue) atomic method instead of the following
                downstreamMirrorService.replace(key, get(key), null); // Replace only empty values downstream
                return value;
            })
                    .onError(onError));
        }

        return mirrorAltFutures;
    }

    /**
     * Stop receiving notification of changes
     * <p>
     * Change messages in-flight, possibly including init tasks, will continue to arrive for some time
     * unless an associated {@link java.util.concurrent.Future} is explicitly <code>cancel()</code>ed
     *
     * @param downstreamMirrorService
     * @throws IOException
     */
    public void unsubscribe(@NonNull final MirrorService<K, V> downstreamMirrorService) throws IOException {
        downstreamMirrorServices.remove(downstreamMirrorService);
    }
}
