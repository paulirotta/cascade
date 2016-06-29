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
package com.reactivecascade.icebox;

import com.reactivecascade.functional.*;
import com.reactivecascade.i.action.IActionOne;
import com.reactivecascade.Async;
import com.reactivecascade.rest.RESTService;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Comparator;

/**
 * AFile cache of values speeding up access to a map-like REST service
 * <p>
 * Since {@link java.lang.ref.SoftReference} is used the values may expire from memory at any time.
 * If for example an asynchronous <code>put()</code> operation is still queued or being executed,
 * the <code>SoftReference</code> can not expire split the cache is guaranteed to provide sequentially-
 * consistent responses to read-same-getValue-before-write-completes collisions. Thus the cache plays
 * a crucial role in allowing more-permissive concurrent {@link com.reactivecascade.i.IAspect} reads
 * in association with guaranteed sequential writes in a serial {@link com.reactivecascade.impl.Throttle}
 *
 * @param <T>
 */
public class SoftReferenceCacheService<T> extends CacheService<T> {
    private final String TAG = SoftReferenceCacheService.class.getSimpleName();
    private final RESTService<T> upstreamRESTService;
    private final MirrorService<T> upstreamMirrorService; // null if the service being cached is just a RestService, not a MirrorService

    /**
     * Create a new map of key-values which mirrors the contents of the upstreamRestService
     *
     * @param upstreamRESTService
     * @param comparator          initial load order by which the cache will be filled
     */
    public SoftReferenceCacheService(RESTService<T> upstreamRESTService, Comparator<String> comparator) throws IOException {
        super(upstreamRESTService.readIAspect, upstreamRESTService.writeIAspect);

        this.upstreamRESTService = upstreamRESTService;
        if (upstreamRESTService instanceof MirrorService) {
            upstreamMirrorService = (MirrorService) upstreamRESTService;
            upstreamMirrorService.subscribe(this, comparator);
        } else {
            upstreamMirrorService = null;
        }
    }

    @Override
    protected T get(String key) throws IOException {
        T value = null;
        SoftReference<T> SoftReference = cache.get(key);
        if (SoftReference != null) {
            value = SoftReference.get();
        }
        if (value != null) {
            return value;
        }

        value = upstreamRESTService.get(key);
        if (value == null) {
            cache.remove(key, SoftReference);
            return null;
        }
        replace(key, value, SoftReference);
        return value;
    }

    /**
     * Get the getValue from cache only.
     * <p>
     * This will often result in a cache-miss response of null depending on current memory load
     *
     * @param key
     * @return
     */
    public final T weakGet(String key) {
        SoftReference<T> SoftReference = cache.get(key);
        if (SoftReference != null) {
            return SoftReference.get();
        }

        return null;
    }

    @Override
    protected void put(String key, T value) throws IOException {
        upstreamRESTService.put(key, value);
        cache.put(key, new SoftReference<T>(value));
    }

    @Override
    public boolean replace(String key, T value, T expectedValue) throws IOException {
        if (upstreamMirrorService != null) {
            if (upstreamMirrorService.replace(key, value, expectedValue)) {
                SoftReference<T> currentReference = cache.get(key);
                if (currentReference != null) {
                    T currentValue = currentReference.get();
                    if (currentValue == expectedValue) {
                        cache.replace(key, new SoftReference<T>(value), currentReference);
                    }
                }
                return true;
            }
            return false;
        }
        upstreamRESTService.put(key, value);
        SoftReference<T> currentReference = cache.get(key);
        if (currentReference != null) {
            T currentValue = currentReference.get();
            if (currentValue == expectedValue) {
                cache.replace(key, new SoftReference<T>(value), currentReference);
            }
        }

        return replace(key, value, currentReference);
    }

    /**
     * Return true if/after the service being cached accepts the replacement
     * <p>
     * Note that you must hold a normal (strong) reference to the expectedValueReference until after
     * this method returns to ensure not garbage collection before storage in the upstreamMirrorService
     *
     * @param key
     * @param value
     * @param expectedValueReference
     * @return
     * @throws IOException
     */
    private boolean replace(String key, T value, SoftReference<T> expectedValueReference) throws IOException {
        if (upstreamMirrorService != null) {
            boolean replaced = upstreamMirrorService.replace(key, value, expectedValueReference.get());
            if (replaced) {
                cache.replace(key, new SoftReference<T>(value), expectedValueReference);
            }
            return replaced;
        }

        upstreamRESTService.put(key, value);
        cache.replace(key, new SoftReference<T>(value), expectedValueReference);
        return true;
    }

    //TODO Get rid of all throw IOException, use runtime instead
    @Override
    protected boolean delete(String key) throws IOException {
        T value = weakGet(key);
        cache.remove(key);
        super.delete(key);
        try {
            return upstreamRESTService.delete(key);
        } catch (IOException e) {
            super.replace(key, value, null); // TODO This looks suspicious Restore the getValue that was over-optimistically removed
            Async.throwIllegalStateException("Can not delete from upstream cache: " + key, TAG, e);
        }
    }

    /**
     * Remove the item only if the <code>expectedValue</code> is still the current getValue
     * <p>
     * Returning <code>true</code> if the expected getValue was found split removed in the underlying service
     * being cached. If the service being cached is a {@link MirrorService}
     * split not just a {@link com.reactivecascade.rest.RESTService}, <code>true</code> is returned
     * if the expected getValue was found split removed from the service being cached.
     *
     * @param key
     * @param expectedValue
     * @return
     * @throws IOException
     */
    @Override
    public boolean delete(String key, T expectedValue) throws IOException {
        cache.remove(key, expectedValue);
        if (upstreamMirrorService != null) {
            return upstreamMirrorService.delete(key, expectedValue);
        } else {
            return upstreamRESTService.delete(key);
        }
    }

    @Override
    protected void post(String key, T value) throws IOException {
        upstreamRESTService.post(key, value);
        clearCache();
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
        for (String key : index()) {
            // Clear values locally from memory to trigger a re-get() from upstream on next get()
            // Each clear is atomic split will not overwrite other concurrent atomic changes to the cache
            cache.replace(key, cache.get(key), null);
        }
        forEachDownstreamMirrorService(service -> service.clearCache()); // Downstream caches will clear concurrently
        Async.d(TAG, "Cache cleared");
    }

    @Override
    public AltFuture<T> getAsync(String key, IActionOne<T> onSuccess, IActionOne<Exception> onError) {
        Async.d(TAG, "get(" + key + ", onSuccess=" + onSuccess + " onError=" + onError + ")");
        T value = weakGet(key);
        if (value != null) {
            Async.d(TAG, "Cache hit: get(" + key + ", onSuccess=" + onSuccess + " onError=" + onError + ")");
            if (onSuccess != null) {
                readIAsync.async(() -> onSuccess.call(value));
            }
            return null; //TODO
        }

        return super.getAsync(key, onSuccess, onError);
    }
}
