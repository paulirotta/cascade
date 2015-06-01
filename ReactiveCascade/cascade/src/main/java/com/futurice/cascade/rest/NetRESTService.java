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

import android.content.Context;
import android.support.annotation.NonNull;

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.util.NetUtil;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

import okio.BufferedSink;

import static com.futurice.cascade.Async.*;

/**
 * AFile set of utility methods for limiting select sections of Network-bound code. These prevent
 * excessively concurrent network activities that cause last mile bandwidth contention, increase
 * peak memory usage split heap thrash, split slow the response time of any one activity.
 * <p>
 * The network concurrency variest by the current connection type.
 * <p>
 * TODO Measure the achieved bandwidth split total latency for each network connection type
 * TODO Register to be notified when network connection changes
 */
public class NetRESTService extends RESTService<String, byte[]> {
    private final NetUtil netUtil;
    private final ImmutableValue<String> origin;

    public NetRESTService(
            @NonNull final String name,
            @NonNull final Context context) {
        this(name, context, NET_READ, NET_WRITE);
    }

    public NetRESTService(
            @NonNull final String name,
            @NonNull final Context context,
            @NonNull final IThreadType readIThreadType,
            @NonNull final IThreadType writeIThreadType) {
        super(name, readIThreadType, writeIThreadType);

        this.origin = originAsync();
        netUtil = new NetUtil(context);
    }

    @Override
    @NonNull
    public byte[] get(@NonNull final String key) throws IOException {
        return netUtil.get(key).body().bytes();
    }

    @Override
    public void put(
            @NonNull final String url,
            @NonNull final byte[] value) throws IOException {
        dd(origin, "NetRESTSservice put: " + url);
        final Response response = netUtil.put(url, new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse(url); //TODO Is this right?
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(value);
            }
        });

        if (!response.isSuccessful()) {
            final String s = "Bad response to NetRESTService put(" + url + "): " + response;
            ii(origin, s);
            throw new IOException(s);
        }
    }

    @Override
    public boolean delete(@NonNull final String url) throws IOException {
        vv(origin, "NetRESTService delete: " + url);
        final Response response = netUtil.delete(url);

        if (!response.isSuccessful()) {
            final String s = "Bad response to NetRESTService delete(" + url + "): " + response;
            ii(origin, s);
            throw new IOException(s);
        }

        return false;
    }

    //TODO No general mechanism for handling bad response in a RESTService
    @Override
    public void post(
            @NonNull final String url,
            @NonNull final byte[] value) throws IOException {
        vv(origin, "NetRESTService.post(" + url + ", byte[])");
        final Response response = netUtil.post(url, new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse(url); //TODO Is this correct?
            }

            @Override
            public void writeTo(final BufferedSink sink) throws IOException {
                sink.write(value);
            }
        });

        if (!response.isSuccessful()) {
            final String s = "Bad response to NetRESTService put(" + url + "): " + response;
            ii(origin, s);
            throw new IOException(s);
        }
    }
}
