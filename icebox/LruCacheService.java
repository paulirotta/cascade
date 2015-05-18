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
package com.futurice.cascade.icebox;

import com.futurice.cascade.icebox.CacheService;
import com.futurice.cascade.icebox.MirrorService;
import com.futurice.cascade.icebox.SoftReferenceCacheService;
import com.futurice.cascade.rest.RESTService;

import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AFile cache of values speeding up access to a map-like REST service
 * <p>
 * Since {@link java.lang.ref.SoftReference} is used the values may expire from memory at any time.
 * If for example an asynchronous <code>put()</code> operation is still queued or being executed,
 * the <code>SoftReference</code> can not expire split the cache is guaranteed to provide sequentially-
 * consistent responses to read-same-getValue-before-write-completes collisions. Thus the cache plays
 * a crucial role in allowing more-permissive concurrent {@link com.futurice.cascade.i.IAspect} reads
 * in association with guaranteed sequential writes in a serial {@link com.futurice.cascade.impl.Throttle}
 *
 * @param <T>
 */
public class LruCacheService<T> extends CacheService<T> {
    private final String TAG = SoftReferenceCacheService.class.getSimpleName();
    private final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();
    private final RESTService<T> upstreamRESTService;
    private final int size; //TODO Not used
    private MirrorService<T> upstreamMirrorService; // null if the service being cached is just a RestService, not a MirrorService

    /**
     * Create a new map of key-values which mirrors the contents of the upstreamRestService
     *
     * @param upstreamRESTService
     * @param comparator          initial load order by which the cache will be filled
     */
    public LruCacheService(RESTService<T> upstreamRESTService, Comparator<String> comparator, int size) throws IOException {
        super(upstreamRESTService.readIAspect, upstreamRESTService.writeIAspect);

        this.upstreamRESTService = upstreamRESTService;
        this.size = size;
        if (upstreamRESTService instanceof MirrorService) {
            upstreamMirrorService = (MirrorService) upstreamRESTService;
            upstreamMirrorService.subscribe(this, comparator);
        } else {
            upstreamMirrorService = null;
        }
    }

    @Override
    protected T get(String key) throws IOException {
        T value = cache.get(key);

        if (value != null) {
            return value;
        }

        T upstreamValue = upstreamRESTService.get(key);
        if (upstreamValue == null) {
            cache.remove(key, value);
            return null;
        }
        replace(key, value, upstreamValue);
        return upstreamValue;
    }

    @Override
    protected void put(String key, T value) throws IOException {
        cache.put(key, value);
        upstreamRESTService.put(key, value);
    }

    @Override
    public boolean replace(String key, T value, T expectedValue) throws IOException {
        if (upstreamMirrorService != null) {
            if (upstreamMirrorService.replace(key, value, expectedValue)) {
                T currentValue = cache.get(key);
                if (currentValue == expectedValue) {
                    cache.replace(key, value, currentValue);
                }
            }
            return true;
        }
        return false;
    }
}
