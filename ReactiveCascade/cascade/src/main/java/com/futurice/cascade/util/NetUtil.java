package com.futurice.cascade.util;

import com.futurice.cascade.Async;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.spdy.Header;

import java.io.IOException;
import java.util.Collection;

/**
 * OkHttp convenience wrapper classes
 */
public final class NetUtil {
    final static OkHttpClient client = new OkHttpClient();
    private static final String TAG = NetUtil.class.getSimpleName();

    public static Response get(String url) throws IOException {
        return get(url, null);
    }

    public static Response get(String url, Collection<Header> headers) throws IOException {
        Async.d(TAG, "get " + url);
        Call call = setupCall(url, builder -> addHeaders(builder, headers));

        return execute(call);
    }

    public static Response put(String url, RequestBody body) throws IOException {
        return put(url, null, body);
    }

    public static Response put(String url, Collection<Header> headers, RequestBody body) throws IOException {
        Async.d(TAG, "put " + url);
        Call call = setupCall(url, builder -> {
            addHeaders(builder, headers);
            builder.put(body);
        });

        return execute(call);
    }

    public static Response post(String url, RequestBody body) throws IOException {
        return post(url, null, body);
    }

    public static Response post(String url, Collection<Header> headers, RequestBody body) throws IOException {
        Async.d(TAG, "post " + url);
        Call call = setupCall(url, builder -> {
            addHeaders(builder, headers);
            builder.post(body);
        });

        return execute(call);
    }

    public static Response delete(String url) throws IOException {
        return delete(url, null);
    }

    public static Response delete(String url, Collection<Header> headers) throws IOException {
        Async.d(TAG, "delete " + url);
        Call call = setupCall(url, builder -> {
            addHeaders(builder, headers);
            builder.delete();
        });

        return execute(call);
    }

    private static void addHeaders(Request.Builder builder, Collection<Header> headers) {
        if (headers == null) {
            return;
        }

        for (Header header : headers) {
            builder.addHeader(header.name.toString(), header.value.toString());
        }
    }

    private static Call setupCall(String url, BuilderModifier builderModifier) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(url);
        if (builderModifier != null) {
            builderModifier.modify(builder);
        }
        Request request = builder.build();

        return client.newCall(request);
    }

    private static Response execute(Call call) throws IOException {
        Response response = call.execute();

        if (response.isRedirect()) {
            String location = response.headers().get("Location");
            Async.d(TAG, "Following HTTP redirect to " + location);
            response = null;
            return get(location);
        }
        if (!response.isSuccessful()) {
            String s = "Unexpected response code " + response;
            IOException e = new IOException(s);
            Async.ee(TAG, s, e);
            throw e;
        }

        return response;
    }

    /**
     * Functional interface to do something to an OkHttp request builder before dispatch
     */
    private static interface BuilderModifier {
        void modify(Request.Builder builder);
    }
}
