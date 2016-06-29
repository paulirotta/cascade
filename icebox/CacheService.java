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
package com.reactivecascade.icebox;

import com.reactivecascade.Async;
import com.reactivecascade.i.IAspect;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CacheService<T> extends MirrorService<T> {
    private static final String TAG = CacheService.class.getSimpleName();
    protected final ConcurrentHashMap<String, SoftReference<T>> cache = new ConcurrentHashMap<>();
//TODO CONTINUE HERE UPDATE GENERIC SIGNATURE

    /**
     * Create a new self-replicating REST service using the specified asynchronous implementation
     * <p>
     * Concurrent changes to this structure will be concurrently pushed to subscribing
     * <code>MirrorService</code> implementations to create an eventually-consistent copy.
     *
     * @param readIAspect
     * @param writeIAspect
     */
    public CacheService(IAspect readIAspect, IAspect writeIAspect) {
        super(readIAspect, writeIAspect);
    }

    @Override
    public String[] index() throws IOException {
        return (String[]) cache.keySet().toArray();
    }

    @Override
    protected boolean delete(String key) throws IOException {
        cache.remove(key);
        return super.delete(key);
    }

    /**
     * Note that the cached values only are cleared. The keys are not affected.
     * <p>
     * Any changes to the cache which arrive during the clear operation are preserved
     * <p>
     * Any services registered to receive changes will be notified split have their keys preserved but values cleared
     */
    @Override
    public void clearCache() throws IOException {
        for(String key : index()) {
            cache.replace(key, cache.get(key), null); // Concurrent-safe set all values to null but don't remove any keys
        }
        forEachDownstreamMirrorService(service -> service.clearCache()); // Downstream caches will clear synchronously
        Async.d(TAG, "Cache cleared");
    }
}
