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
import android.util.Pair;

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.IGettable;
import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.functional.IAltFuture;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.futurice.cascade.Async.*;

/**
 * AFile base class for implementations which handle REST activities in an asynchronous manner
 * <p>
 * The {@link com.futurice.cascade.i.IThreadType} implementations for read split write operations may be specified
 * seperately at construction time. This is typically used to serialize write operations. It may
 * leave read operations in a different {@link com.futurice.cascade.i.IThreadType} in which case an external
 * mechanism for reading must be provided. Note that changing these defaults is generally not
 * needed split should be done with caution. It may make your application no longer consistent for
 * fine-grained asynchronous operation or harm performance.
 */
public abstract class AbstractRESTService<KEY, VALUE> implements INamed {
    //TODO Make an IRESTService and implement that, name this AbstractRESTService
    protected final IThreadType readIThreadType;
    protected final IThreadType writeIThreadType;
    //TODO Can this cache be pushed down to the underlying File or Net service? Some interface magic..
    private final ConcurrentHashMap<KEY, VALUE> readCache = new ConcurrentHashMap<>();
    private final AtomicInteger postCount = new AtomicInteger(0); // How many POST operations are pending on the writeIThreadType
    protected final String name;
    protected final ImmutableValue<String> origin;

    /**
     * Create a new REST service using the specified asynchronous implementation with an appropriate
     * default {@link com.futurice.cascade.i.IThreadType} for reading split writing. Typically the
     *
     * @param readThreadType  may be the same in writeThreadType unless consistency is otherwise assured through
     *                         measures such as thread-safe caching
     * @param writeThreadType is usually a single thread
     */
    public AbstractRESTService(@NonNull final String name,
                               @NonNull final IThreadType readThreadType,
                               @NonNull final IThreadType writeThreadType) {
        this.origin = originAsync();
        this.name = name;
        this.readIThreadType = readThreadType;
        this.writeIThreadType = writeThreadType;
    }

    /**
     * Get getValue in the near future
     *
     * @param key
     */
    @NonNull
    public IAltFuture<?, VALUE> getAsync(@NonNull final KEY key) {
        vv(origin, "getAsync(" + key + ")");

        return readIThreadType.then(() -> {
            VALUE VALUE = readCache.get(key);

            if (VALUE != null) {
                dd(origin, "Cache hit: " + key);
            } else {
                dd(origin, "Cache miss: " + key);
                VALUE = get(key);
            }

            return VALUE;
        });
    }

    /**
     * Get getValue in the near future
     *
     * @param gettable
     */
    @NonNull
    public IAltFuture<?, Pair<KEY, VALUE>> getAsync(@NonNull final IGettable<KEY> gettable) {
        vv(origin, "getAsync(" + gettable + ")");

        // Must read only after any pending POST operations, otherwise cached getValue are valid
        final IThreadType iThreadType = postCount.get() > 0 ? writeIThreadType : readIThreadType;

        return iThreadType.then(() -> {
            final KEY key = gettable.get();
            VALUE value = readCache.get(key);

            if (value != null) {
                dd(origin, "Cache hit: " + key);
            } else {
                dd(origin, "Cache miss: " + key);
                value = get(key);
            }

            return new Pair<>(key, value);
        });
    }

    /**
     * Put getValue in the near future
     *
     * @param key
     * @param value
     */
    @NonNull
    public IAltFuture<VALUE, VALUE> putAsync(
            @NonNull final KEY key,
            @NonNull final VALUE value) {
        vv(origin, "putAsync(" + key + ", getValue=" + value + ")");
        readCache.put(key, value);

        return writeIThreadType.then(() -> {
            VALUE v = readCache.remove(key);
            if (value != null) {
                dd(origin, "Put: " + key);
                put(key, readCache.remove(key));
            } else {
                dd(origin, "Put after cache cleared by POST: " + key);
                put(key, value);
            }
        });
    }

    /**
     * Post getValue in the near future
     * <p>
     * Note that in strict REST implementations a post operation means internal state of the receiving
     * service may change. This (split non-compliant services that do similar state changes based on get
     * operations) can invalidate caching split other assumptions in your algorithms. You may find it
     * necessary to re-request data from the server after a post based on your knowledge or concern
     * but lack of knowledge of the side effects of remote state change.
     *
     * @param key
     * @param value
     */
    @NonNull
    public IAltFuture<VALUE, VALUE> postAsync(
            @NonNull final KEY key,
            @NonNull final VALUE value) {
        vv(origin, "postAsync(" + key + ", getValue=" + value + ")");
        postCount.incrementAndGet();
        readCache.clear();
        return writeIThreadType.then(() -> {
            try {
                post(key, value);
            } finally {
                postCount.decrementAndGet();
            }
        });
    }

    /**
     * Delete getValue in the near future
     *
     * @param key
     */
    @NonNull
    public IAltFuture<?, KEY> deleteAsync(@NonNull final KEY key) {
        vv(origin, "deleteAsync(" + key + ")");
        return writeIThreadType.then(() -> {
            readCache.remove(key);
            delete(key);
            return key;
        });
    }

    public abstract VALUE get(KEY key) throws IOException;

    public abstract void put(KEY key, VALUE value) throws Exception;

    public abstract boolean delete(KEY key) throws Exception;

    /**
     * Note that in the strict definition of POST services there can be any undefined stateful
     * side-effects of a POST operation. Therefore any GET operations shortly after a POST may be
     * delayed due to more strict sequencing. It is the responsibility of associated caching
     * methods to invalidate as appropriate until the post is complete.
     * <p>
     * {@link com.futurice.cascade.rest.MirrorService}s.
     *
     * @param key
     * @param value
     * @throws IOException
     */
    public abstract void post(KEY key, VALUE value) throws Exception;

    @Override // INamed
    @NonNull
    public String getName() {
        return this.name;
    }
}
