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

import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.IGettable;
import com.futurice.cascade.i.IThreadType;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.framed.Header;

import java.io.IOException;
import java.net.URL;
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
import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.originAsync;

/**
 * OkHttp convenience wrapper methods
 */
public final class NetUtil {
    private static final int MAX_NUMBER_OF_WIFI_NET_CONNECTIONS = 6;
    private static final int MAX_NUMBER_OF_3G_NET_CONNECTIONS = 4;
    private static final int MAX_NUMBER_OF_2G_NET_CONNECTIONS = 2;
    @NonNull
    final OkHttpClient mOkHttpClient;
    @NonNull
    final ImmutableValue<String> mOrigin;
    @NonNull
    private final TelephonyManager mTelephonyManager;
    @NonNull
    private final WifiManager mWifiManager;
    @NonNull
    private final IThreadType mNetReadThreadType;
    @NonNull
    private final IThreadType mNetWriteThradType;
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
        mOrigin = originAsync();
        this.mNetReadThreadType = netReadThreadType;
        this.mNetWriteThradType = netWriteThreadType;
        mOkHttpClient = new OkHttpClient();
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
    }

    @NonNull
    @WorkerThread
    public Response get(@NonNull final String url) throws IOException {
        return get(url, null);
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Response> getAsync(@NonNull final String url) {
        return mNetReadThreadType.then(() -> get(url, null));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <T> IAltFuture<T, Response> getAsync() {
        return mNetReadThreadType.map(url -> get(url.toString(), null));
    }

    @NonNull
    @WorkerThread
    public Response get(@NonNull final IGettable<String> urlGettable) throws IOException {
        return get(urlGettable.get(), null);
    }

    @NonNull
    @WorkerThread
    public Response get(
            @NonNull final String url,
            @Nullable final Collection<Header> headers) throws IOException {
        if (headers == null) {
            dd(mOrigin, "get " + url);
        } else {
            dd(mOrigin, "get " + url + " with " + headers.size() + " custom headers");
        }

        return execute(setupCall(url, builder -> {
            if (headers != null) {
                for (Header header : headers) {
                    builder.addHeader(header.name.utf8(), header.value.utf8());
                }
            }
        }));
    }

    @NonNull
    @WorkerThread
    public Response get(
            @NonNull final IGettable<String> urlGettable,
            @Nullable final Collection<Header> headers) throws IOException {
        return get(urlGettable.get(), headers);
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> getAsync(
            @Nullable final Collection<Header> headers) {
        return mNetReadThreadType.map(url -> get(url, headers));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> getAsync(
            @NonNull final String url,
            final IGettable<Collection<Header>> headersGettable) {
        return mNetReadThreadType.then(() -> get(url, headersGettable.get()));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> getAsync(
            @NonNull final String url,
            @Nullable final Collection<Header> headers) {
        return mNetReadThreadType.then(() -> get(url, headers));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> getAsync(
            @NonNull final IGettable<String> urlGettable,
            final IGettable<Collection<Header>> headersGettable) {
        return mNetReadThreadType.then(() -> get(urlGettable.get(), headersGettable.get()));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> getAsync(
            final IGettable<Collection<Header>> headersGettable) {
        return mNetReadThreadType.map(url -> get(url, headersGettable.get()));
    }

    @NonNull
    @WorkerThread
    public Response put(
            @NonNull final String url,
            @NonNull final RequestBody body) throws IOException {
        return put(url, null, body);
    }

    @NonNull
    @WorkerThread
    public Response put(
            @NonNull final String url,
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) throws IOException {
        dd(mOrigin, "put " + url);
        final Call call = setupCall(url, builder -> {
            addHeaders(builder, headers);
            builder.put(body);
        });

        return execute(call);
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Response> putAsync(
            @NonNull final String url,
            @NonNull final RequestBody body) {
        return mNetWriteThradType.then(() -> put(url, body));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<RequestBody, Response> putAsync(
            @NonNull final String url) {
        return mNetWriteThradType.map(body -> put(url, body));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> putAsync(
            @NonNull final RequestBody body) {
        return mNetWriteThradType.map(url -> put(url, body));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Response> putAsync(
            @NonNull final String url,
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) {
        return mNetWriteThradType.then(() -> put(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> putAsync(
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) {
        return mNetWriteThradType.map(url -> put(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<RequestBody, Response> putAsync(
            @NonNull final String url,
            @Nullable final Collection<Header> headers) {
        return mNetWriteThradType.map(body -> put(url, headers, body));
    }

    @NonNull
    @WorkerThread
    public Response post(
            @NonNull final String url,
            @NonNull final RequestBody body) throws IOException {
        return post(url, null, body);
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Response> postAsync(
            @NonNull final String url,
            @NonNull final RequestBody body) {
        return mNetWriteThradType.then(() -> post(url, null, body));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> postAsync(
            @NonNull final RequestBody body) {
        return mNetWriteThradType.map(url -> post(url, null, body));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<RequestBody, Response> postAsync(
            @NonNull final String url) {
        return mNetWriteThradType.map(body -> post(url, null, body));
    }

    @NonNull
    @WorkerThread
    public Response post(
            @NonNull final URL url,
            @NonNull final RequestBody body) throws IOException {
        return post(url.toString(), null, body);
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Response> postAsync(
            @NonNull final String url,
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) {
        return mNetWriteThradType.then(() -> post(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> postAsync(
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) {
        return mNetWriteThradType.map(url -> post(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<RequestBody, Response> postAsync(
            @NonNull final String url,
            @Nullable final Collection<Header> headers) {
        return mNetWriteThradType.map(body -> post(url, headers, body));
    }

    @NonNull
    @WorkerThread
    public Response post(
            @NonNull final String url,
            @Nullable final Collection<Header> headers,
            @NonNull final RequestBody body) throws IOException {
        dd(mOrigin, "post " + url);
        final Call call = setupCall(url, builder -> {
            addHeaders(builder, headers);
            builder.post(body);
        });

        return execute(call);
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Response> deleteAsync(@NonNull final String url) {
        return mNetWriteThradType.then(() -> delete(url, null));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> deleteAsync() {
        return mNetWriteThradType.map(url -> delete(url, null));
    }

    @NonNull
    @WorkerThread
    public Response delete(@NonNull final String url) throws IOException {
        return delete(url, null);
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Response> deleteAsync(
            @NonNull final String url,
            @Nullable final Collection<Header> headers) {
        return mNetWriteThradType.then(() -> delete(url, headers));
    }

    @NonNull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<String, Response> deleteAsync(
            @Nullable final Collection<Header> headers) {
        return mNetWriteThradType.map(url -> delete(url, headers));
    }

    @NonNull
    @WorkerThread
    public Response delete(
            @NonNull final String url,
            @Nullable final Collection<Header> headers) throws IOException {
        dd(mOrigin, "delete " + url);
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

        for (Header header : headers) {
            builder.addHeader(header.name.utf8(), header.value.utf8());
        }
    }

    @NonNull
    private Call setupCall(
            @NonNull final String url,
            @Nullable final BuilderModifier builderModifier) throws IOException {
        final Request.Builder builder = new Request.Builder()
                .url(url);
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

            dd(mOrigin, "Following HTTP redirect to " + location);
            return get(location);
        }
        if (!response.isSuccessful()) {
            final String s = "Unexpected response code " + response;
            final IOException e = new IOException(s);

            ee(mOrigin, s, e);
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
        SupplicantState s = mWifiManager.getConnectionInfo().getSupplicantState();
        NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);

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
            default:
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
                return NetType.NET_4G;
        }
    }

    public enum NetType {NET_2G, NET_2_5G, NET_3G, NET_3_5G, NET_4G}

    /**
     * Functional interface to do something to an OkHttp request builder before dispatch
     */
    private interface BuilderModifier {
        void modify(Request.Builder builder);
    }
}
