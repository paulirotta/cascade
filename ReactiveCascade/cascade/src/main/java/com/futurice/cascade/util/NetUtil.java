/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.WorkerThread;
import android.telephony.TelephonyManager;

import com.futurice.cascade.active.RunnableAltFuture;
import com.futurice.cascade.active.SettableAltFuture;
import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.i.IAsyncOrigin;
import com.futurice.cascade.i.IGettable;
import com.futurice.cascade.i.IThreadType;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.framed.Header;

import java.io.IOException;
import java.util.Collection;

import static android.telephony.TelephonyManager.NETWORK_TYPE_1xRTT;
import static android.telephony.TelephonyManager.NETWORK_TYPE_CDMA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EDGE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EHRPD;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B;
import static android.telephony.TelephonyManager.NETWORK_TYPE_GPRS;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_IDEN;
import static android.telephony.TelephonyManager.NETWORK_TYPE_LTE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UMTS;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN;
import static com.futurice.cascade.Async.NET_READ;
import static com.futurice.cascade.Async.NET_WRITE;

/**
 * OkHttp convenience wrapper methods
 */
public final class NetUtil extends Origin {
    private static final int MAX_NUMBER_OF_WIFI_NET_CONNECTIONS = 6;
    private static final int MAX_NUMBER_OF_3G_NET_CONNECTIONS = 4;
    private static final int MAX_NUMBER_OF_2G_NET_CONNECTIONS = 2;
    @NonNull
    private final OkHttpClient mOkHttpClient;
    @NonNull
    private final TelephonyManager mTelephonyManager;
    @NonNull
    private final WifiManager mWifiManager;
    @NonNull
    private final IThreadType mNetReadThreadType;
    @NonNull
    private final IThreadType mNetWriteThreadType;

    @RequiresPermission(allOf = {Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE})
    public NetUtil(@NonNull final Context context) {
        this(context, NET_READ, NET_WRITE);
    }

    public NetUtil(
            @NonNull final Context context,
            @NonNull final IThreadType netReadThreadType,
            @NonNull final IThreadType netWriteThreadType) {
        this.mNetReadThreadType = netReadThreadType;
        this.mNetWriteThreadType = netWriteThreadType;
        mOkHttpClient = new OkHttpClient();
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
    }

    @NonNull
    @WorkerThread
    public <T> Response get(@NonNull final T url) throws IOException {
        return get(url, null);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> getAsync(@NonNull final T url) {
        return new RunnableAltFuture<>(mNetReadThreadType, () ->
                get(url, null));
    }

    /**
     * Take the output from the previous step in the chain as the URL.
     * <p>
     * Note that the input object has {@link T#toString()} is called to generate the URL at the last
     * possible moment before the network connection is opened. This may be useful to delay the decision
     * of which URL will be used, for example to decide what is the highest priority use of the network
     * at that moment.
     *
     * @param <T> the output of the upchain step
     * @return alt future of the network response
     */
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> getAsync() {
        return new RunnableAltFuture<>(mNetReadThreadType, (T url) ->
                get(url.toString(), null));
    }

    /**
     * @param urlGettable
     * @param <T>
     * @return
     * @throws IOException           if problem with network
     * @throws IllegalStateException if <code>urlGettable</code> can not yet be determined.
     *                               Consider using <code>urlGettable.then()</code> instead.
     */
    @NonNull
    @WorkerThread
    public <T> Response get(@NonNull final IGettable<T> urlGettable) throws IOException {
        return get(urlGettable.get().toString());
    }

    @NonNull
    @WorkerThread
    public <T> Response get(
            @NonNull final T url,
            @Nullable final Collection<Header> headers) throws IOException {
        if (headers == null) {
            RCLog.d(getOrigin(), "get " + url);
        } else {
            RCLog.d(getOrigin(), "get " + url + " with " + headers.size() + " custom headers");
        }

        return execute(setupCall(url, builder -> {
            addHeaders(builder, headers);
        }));
    }

    /**
     * @param urlGettable
     * @param headers
     * @param <T>
     * @return
     * @throws IOException
     * @throws IllegalStateException if <code>urlGettable</code> can not be determined. Consider using
     *                               <code>urlGettable.then(headers)</code> instead.
     */
    @NonNull
    @WorkerThread
    public <T> Response get(
            @NonNull final IGettable<T> urlGettable,
            @Nullable final Collection<Header> headers) throws IOException {
        return get(urlGettable.get(), headers);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> getAsync(
            @Nullable final Collection<Header> headers) {
        return new RunnableAltFuture<>(mNetReadThreadType, (T url) ->
                get(url.toString(), headers));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    public <T> IAltFuture<T, Response> getAsync(
            @NonNull final T url,
            final IGettable<Collection<Header>> headersGettable) {
        if (headersGettable instanceof IAltFuture) {
            return ((IAltFuture<?, T>) headersGettable)
                    .on(mNetReadThreadType)
                    .then(() ->
                            get(url, headersGettable.get()));
        }

        return new RunnableAltFuture<>(mNetReadThreadType, () ->
                get(url, headersGettable.get()));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> getAsync(
            @NonNull final T url,
            @Nullable final Collection<Header> headers) {
        return new RunnableAltFuture<>(mNetReadThreadType, () ->
                get(url, headers));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    public <T> IAltFuture<T, Response> getAsync(
            @NonNull final IGettable<T> urlGettable,
            final IGettable<Collection<Header>> headersGettable) {
        final IAltFuture<T, Response> headOfChain = new SettableAltFuture<>(mNetReadThreadType);
        IAltFuture<T, Response> chainedAltFuture = headOfChain;
        boolean chained = false;

        if (urlGettable instanceof IAltFuture) {
            chainedAltFuture = chainedAltFuture.await((IAltFuture<?, Response>) urlGettable);
            chained = true;
        }
        if (headersGettable instanceof IAltFuture) {
            chainedAltFuture = chainedAltFuture
                    .await((IAltFuture<?, Collection<Header>>) headersGettable);
            chained = true;
        }

        if (chained) {
            final IAltFuture<Response, Response> tailOfChain = chainedAltFuture.then(() ->
                    get(urlGettable.get(), headersGettable.get()));

//            FIXME We need to return the chain as a single entity, or does forking fire up this alternate chain and back down?
            return headOfChain;
        }

        return new RunnableAltFuture<>(mNetReadThreadType, () ->
                get(urlGettable.get(), headersGettable.get()));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> getAsync(
            final IGettable<Collection<Header>> headersGettable) {
        return new RunnableAltFuture<>(mNetReadThreadType, (T url) ->
                get(url, headersGettable.get()));
    }

    @NonNull
    @WorkerThread
    public <T> Response put(
            @NonNull final T url,
            @NonNull final RequestBody body) throws IOException {
        return put(url, null, body);
    }

    @NonNull
    @WorkerThread
    public <T> Response put(
            @NonNull final T url,
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) throws IOException {
        RCLog.d(getOrigin(), "put " + url + " headers=" + headers + " body=" + body);

        return execute(setupCall(url, builder -> {
            addHeaders(builder, headers);
            builder.put(body);
        }));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> putAsync(
            @NonNull final T url,
            @NonNull final RequestBody body) {
        return new RunnableAltFuture<>(mNetWriteThreadType, () ->
                put(url, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<RequestBody, Response> putAsync(
            @NonNull final T url) {
        return new RunnableAltFuture<>(mNetWriteThreadType, (RequestBody body) ->
                put(url, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> putAsync(
            @NonNull final RequestBody body) {
        return new RunnableAltFuture<>(mNetWriteThreadType, (T url) ->
                put(url, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> putAsync(
            @NonNull final T url,
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) {
        return new RunnableAltFuture<>(mNetWriteThreadType, () ->
                put(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> putAsync(
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) {
        return new RunnableAltFuture<>(mNetWriteThreadType, (T url) ->
                put(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<RequestBody, Response> putAsync(
            @NonNull final T url,
            @Nullable final Collection<Header> headers) {
        return new RunnableAltFuture<RequestBody, Response>(mNetWriteThreadType, (RequestBody body) ->
                put(url, headers, body));
    }

    @NonNull
    @WorkerThread
    public <T> Response post(
            @NonNull final T url,
            @NonNull final RequestBody body) throws IOException {
        return post(url, null, body);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> postAsync(
            @NonNull final T url,
            @NonNull final RequestBody body) {
        return new RunnableAltFuture<>(mNetWriteThreadType, () ->
                post(url, null, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> postAsync(
            @NonNull final RequestBody body) {
        return new RunnableAltFuture<>(mNetWriteThreadType, (T url) ->
                post(url, null, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<RequestBody, Response> postAsync(
            @NonNull final T url) {
        return new RunnableAltFuture<>(mNetWriteThreadType, (RequestBody body) ->
                post(url, null, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> postAsync(
            @NonNull final T url,
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) {
        return new RunnableAltFuture<>(mNetWriteThreadType, () ->
                post(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> postAsync(
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) {
        return new RunnableAltFuture<>(mNetWriteThreadType, (T url) ->
                post(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<RequestBody, Response> postAsync(
            @NonNull final T url,
            @Nullable final Collection<Header> headers) {
        return new RunnableAltFuture<>(mNetWriteThreadType, (RequestBody body) ->
                post(url, headers, body));
    }

    @NonNull
    @WorkerThread
    public <T> Response post(
            @NonNull final T url,
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) throws IOException {
        RCLog.d(getOrigin(), "post " + url);
        final Call call = setupCall(url, builder -> {
            addHeaders(builder, headers);
            builder.post(body);
        });

        return execute(call);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> deleteAsync(@NonNull final T url) {
        return new RunnableAltFuture<>(mNetWriteThreadType, () ->
                delete(url, null));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> deleteAsync() {
        return new RunnableAltFuture<>(mNetWriteThreadType, (T url) ->
                delete(url, null));
    }

    @NonNull
    @WorkerThread
    public <T> Response delete(@NonNull final T url) throws IOException {
        return delete(url, null);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> deleteAsync(
            @NonNull final T url,
            @Nullable final Collection<Header> headers) {
        return new RunnableAltFuture<>(mNetWriteThreadType, () ->
                delete(url, headers));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> deleteAsync(
            @Nullable final Collection<Header> headers) {
        return new RunnableAltFuture<>(mNetWriteThreadType, (T url) ->
                delete(url, headers));
    }

    @NonNull
    @WorkerThread
    public <T> Response delete(
            @NonNull final T url,
            @Nullable final Collection<Header> headers) throws IOException {
        RCLog.d(getOrigin(), "delete " + url);
        final Call call = setupCall(url, builder -> {
            addHeaders(builder, headers);
            builder.delete();
        });

        return execute(call);
    }

    private void addHeaders(
            @NonNull final Request.Builder builder,
            @Nullable final Collection<Header> headers) {
        if (headers == null) {
            return;
        }

        for (final Header header : headers) {
            builder.addHeader(header.name.utf8(), header.value.utf8());
        }
    }

    @NonNull
    private <T> Call setupCall(
            @NonNull final T url,
            @Nullable final BuilderModifier builderModifier) throws IOException {
        final Request.Builder builder = new Request.Builder()
                .url(url.toString());
        if (builderModifier != null) {
            builderModifier.modify(builder);
        }

        return mOkHttpClient.newCall(builder.build());
    }

    /**
     * Complete the okhttp Call action synchronously on the current thread.
     * <p>
     * We are explicitly using our own threading model for debuggability and concurrency management
     * reasons rather than delegating that to the library
     *
     * @param call
     * @return
     * @throws IOException
     */
    @NonNull
    @WorkerThread
    private Response execute(@NonNull final Call call) throws IOException {
        final Response response = call.execute();

        if (response.isRedirect()) {
            final String location = response.headers().get("Location");

            RCLog.d(getOrigin(), "Following HTTP redirect to " + location);
            return get(location);
        }
        if (!response.isSuccessful()) {
            final String s = "Unexpected response code " + response;
            final IOException e = new IOException(s);

            RCLog.e(getOrigin(), s, e);
            throw e;
        }

        return response;
    }

    //TODO Use max number of NET connections on startup split adapt as these change
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public int getMaxNumberOfNetConnections() {
        if (isWifi()) {
            return MAX_NUMBER_OF_WIFI_NET_CONNECTIONS;
        }

        switch (getNetworkType()) {
            case NET_2G:
            case NET_2_5G:
                return MAX_NUMBER_OF_2G_NET_CONNECTIONS;

            case NET_3G:
            case NET_3_5G:
            case NET_4G:
            default:
                return MAX_NUMBER_OF_3G_NET_CONNECTIONS;

        }
    }

    /**
     * Check if a current network WIFI connection is CONNECTED
     *
     * @return
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public boolean isWifi() {
        final SupplicantState s = mWifiManager.getConnectionInfo().getSupplicantState();
        final NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);

        return state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR;
    }

    @NonNull
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public NetType getNetworkType() {
        switch (mTelephonyManager.getNetworkType()) {
            case NETWORK_TYPE_UNKNOWN:
            case NETWORK_TYPE_CDMA:
            case NETWORK_TYPE_GPRS:
            case NETWORK_TYPE_IDEN:
                return NetType.NET_2G;

            case NETWORK_TYPE_EDGE:
                return NetType.NET_2_5G;

            case NETWORK_TYPE_UMTS:
            case NETWORK_TYPE_1xRTT:
                return NetType.NET_3G;

            case NETWORK_TYPE_EHRPD:
            case NETWORK_TYPE_EVDO_0:
            case NETWORK_TYPE_EVDO_A:
            case NETWORK_TYPE_EVDO_B:
            case NETWORK_TYPE_HSPA:
            case NETWORK_TYPE_HSPAP:
            case NETWORK_TYPE_HSUPA:
            case NETWORK_TYPE_HSDPA:
                return NetType.NET_3_5G;

            case NETWORK_TYPE_LTE:
            default:
                return NetType.NET_4G;
        }
    }

    public enum NetType {NET_2G, NET_2_5G, NET_3G, NET_3_5G, NET_4G, NET_5G}

    /**
     * Functional interface to do something to an OkHttp request builder before dispatch
     */
    private interface BuilderModifier {
        void modify(Request.Builder builder);
    }
}
