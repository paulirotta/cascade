/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.Manifest;
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

import com.reactivecascade.functional.RunnableAltFuture;
import com.reactivecascade.functional.SettableAltFuture;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IGettable;
import com.reactivecascade.i.IThreadType;

import java.io.IOException;
import java.util.Collection;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.framed.Header;

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

/**
 * OkHttp convenience wrapper methods
 */
public final class NetUtil extends Origin {
    public enum NetType {NET_2G, NET_2_5G, NET_3G, NET_3_5G, NET_4G, NET_5G}

    private static final int MAX_NUMBER_OF_WIFI_NET_CONNECTIONS = 6;
    private static final int MAX_NUMBER_OF_3G_NET_CONNECTIONS = 4;
    private static final int MAX_NUMBER_OF_2G_NET_CONNECTIONS = 2;

    @NonNull
    private final OkHttpClient okHttpClient;

    @NonNull
    private final TelephonyManager telephonyManager;

    @NonNull
    private final WifiManager wifiManager;

    @NonNull
    private final IThreadType netReadThreadType;

    @NonNull
    private final IThreadType netWriteThreadType;

//    /**
//     * Create a {@link NetUtil} instance which uses the default {@link com.reactivecascade.Async#NET_READ}
//     * and {@link com.reactivecascade.Async#NET_WRITE} to run asynchonous tasks.
//     *
//     * @param context run time context
//     */
//    public NetUtil(@NonNull final Context context) {
//        this(context, Async.NET_READ, Async.NET_WRITE);
//    }

    /**
     * Create a {@link NetUtil} instance with custom thread groups for network reads and writes
     *
     * @param context            run time context
     * @param netReadThreadType  executes read tasks
     * @param netWriteThreadType executes write tasks
     */
    @RequiresPermission(allOf = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE})
    public NetUtil(@NonNull Context context,
                   @NonNull IThreadType netReadThreadType,
                   @NonNull IThreadType netWriteThreadType) {
        this.netReadThreadType = netReadThreadType;
        this.netWriteThreadType = netWriteThreadType;
        okHttpClient = new OkHttpClient();
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    @NonNull
    @WorkerThread
    public <T> Response get(@NonNull T url) throws IOException {
        return get(url, null);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> getAsync(@NonNull final T url) {
        return new RunnableAltFuture<>(netReadThreadType,
                () -> {
                    return get(url, null);
                });
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
        return new RunnableAltFuture<>(netReadThreadType,
                (T url) ->
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
    public <T> Response get(@NonNull IGettable<T> urlGettable) throws IOException {
        return get(urlGettable.get().toString());
    }

    @NonNull
    @WorkerThread
    public <T> Response get(@NonNull T url,
                            @Nullable Collection<Header> headers) throws IOException {
        if (headers == null) {
            RCLog.d(getOrigin(), "get " + url);
        } else {
            RCLog.d(getOrigin(), "get " + url + " with " + headers.size() + " custom headers");
        }

        return execute(setupCall(url,
                builderModifier -> {
                    addHeaders(builderModifier, headers);
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
    public <T> Response get(@NonNull IGettable<T> urlGettable,
                            @Nullable Collection<Header> headers) throws IOException {
        return get(urlGettable.get(), headers);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> getAsync(
            @Nullable final Collection<Header> headers) {
        return new RunnableAltFuture<>(netReadThreadType,
                (T url) -> get(url.toString(), headers));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    public <T> IAltFuture<T, Response> getAsync(
            @NonNull final T url,
            final IGettable<Collection<Header>> headersGettable) {
        if (headersGettable instanceof IAltFuture) {
            return ((IAltFuture<?, T>) headersGettable)
                    .on(netReadThreadType)
                    .then(() -> get(url, headersGettable.get()));
        }

        return new RunnableAltFuture<>(netReadThreadType, () ->
                get(url, headersGettable.get()));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> getAsync(
            @NonNull final T url,
            @Nullable final Collection<Header> headers) {
        return new RunnableAltFuture<>(netReadThreadType, () ->
                get(url, headers));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @SuppressWarnings("unchecked")
    public <T> IAltFuture<T, Response> getAsync(@NonNull IGettable<T> urlGettable,
                                                @NonNull IGettable<Collection<Header>> headersGettable) {
        IAltFuture<Response, Response> chainedAltFuture = new SettableAltFuture<>(netReadThreadType);
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

        //TODO Compound chaining
//        if (chained) {
//            final IAltFuture<Response, Response> tailOfChain = chainedAltFuture.then(() ->
//                    get(urlGettable.get(), headersGettable.get()));
//
////            FIXME We need to return the chain as a single entity, or does forking fire up this alternate chain and back down?
//            return new CompoundAltFuture<>(headOfChain, tailOfChain);
//        }

        return new RunnableAltFuture<>(netReadThreadType,
                () -> get(urlGettable.get(),
                        headersGettable.get()));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> getAsync(
            @NonNull IGettable<Collection<Header>> headersGettable) {
        return new RunnableAltFuture<>(netReadThreadType,
                (T url) -> get(url, headersGettable.get()));
    }

    @NonNull
    @WorkerThread
    public <T> Response put(@NonNull T url,
                            @NonNull RequestBody body) throws IOException {
        return put(url, null, body);
    }

    @NonNull
    @WorkerThread
    public <T> Response put(@NonNull T url,
                            @Nullable Collection<Header> headers,
                            @NonNull RequestBody body) throws IOException {
        RCLog.d(getOrigin(), "put " + url + " headers=" + headers + " body=" + body);

        return execute(setupCall(url,
                builder -> {
                    addHeaders(builder, headers);
                    builder.put(body);
                }));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> putAsync(@NonNull T url,
                                                @NonNull RequestBody body) {
        return new RunnableAltFuture<>(netWriteThreadType,
                () -> put(url, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<RequestBody, Response> putAsync(@NonNull T url) {
        return new RunnableAltFuture<>(netWriteThreadType,
                (RequestBody body) -> put(url, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> putAsync(@NonNull RequestBody body) {
        return new RunnableAltFuture<>(netWriteThreadType,
                (T url) -> put(url, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> putAsync(@NonNull T url,
                                                @Nullable Collection<Header> headers,
                                                @NonNull RequestBody body) {
        return new RunnableAltFuture<>(netWriteThreadType,
                () -> put(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> putAsync(@Nullable Collection<Header> headers,
                                                @NonNull RequestBody body) {
        return new RunnableAltFuture<>(netWriteThreadType, (T url) ->
                put(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<RequestBody, Response> putAsync(@NonNull T url,
                                                          @Nullable Collection<Header> headers) {
        return new RunnableAltFuture<>(netWriteThreadType, (RequestBody body) ->
                put(url, headers, body));
    }

    @NonNull
    @WorkerThread
    public <T> Response post(@NonNull T url,
                             @NonNull RequestBody body) throws IOException {
        return post(url, null, body);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> postAsync(@NonNull T url,
                                                 @NonNull RequestBody body) {
        return new RunnableAltFuture<>(netWriteThreadType,
                () -> post(url, null, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> postAsync(@NonNull RequestBody body) {
        return new RunnableAltFuture<>(netWriteThreadType,
                (T url) -> post(url, null, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<RequestBody, Response> postAsync(@NonNull T url) {
        return new RunnableAltFuture<>(netWriteThreadType,
                (RequestBody body) -> post(url, null, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> postAsync(@NonNull T url,
                                                 @Nullable Collection<Header> headers,
                                                 @NonNull RequestBody body) {
        return new RunnableAltFuture<>(netWriteThreadType,
                () -> post(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> postAsync(@Nullable Collection<Header> headers,
                                                 @NonNull RequestBody body) {
        return new RunnableAltFuture<>(netWriteThreadType,
                (T url) -> post(url, headers, body));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<RequestBody, Response> postAsync(@NonNull T url,
                                                           @Nullable Collection<Header> headers) {
        return new RunnableAltFuture<>(netWriteThreadType, (RequestBody body) ->
                post(url, headers, body));
    }

    @NonNull
    @WorkerThread
    public <T> Response post(@NonNull T url,
                             @Nullable Collection<Header> headers,
                             @NonNull RequestBody body) throws IOException {
        RCLog.d(getOrigin(), "post " + url);

        final Call call = setupCall(
                url,
                builder -> {
                    addHeaders(builder, headers);
                    builder.post(body);
                });

        return execute(call);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> deleteAsync(@NonNull T url) {
        return new RunnableAltFuture<>(netWriteThreadType, () ->
                delete(url, null));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> deleteAsync() {
        return new RunnableAltFuture<>(netWriteThreadType,
                (T url) -> delete(url, null));
    }

    @NonNull
    @WorkerThread
    public <T> Response delete(@NonNull T url) throws IOException {
        return delete(url, null);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<?, Response> deleteAsync(@NonNull final T url,
                                                   @Nullable final Collection<Header> headers) {
        return new RunnableAltFuture<>(netWriteThreadType,
                () -> delete(url, headers));
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <T> IAltFuture<T, Response> deleteAsync(@Nullable Collection<Header> headers) {
        return new RunnableAltFuture<>(netWriteThreadType, (T url) ->
                delete(url, headers));
    }

    @NonNull
    @WorkerThread
    public <T> Response delete(@NonNull T url,
                               @Nullable Collection<Header> headers) throws IOException {
        RCLog.d(getOrigin(), "delete " + url);

        final Call call = setupCall(url, builder -> {
            addHeaders(builder, headers);
            builder.delete();
        });

        return execute(call);
    }

    private void addHeaders(@NonNull Request.Builder builder,
                            @Nullable Collection<Header> headers) {
        if (headers == null) {
            return;
        }

        for (final Header header : headers) {
            builder.addHeader(header.name.utf8(), header.value.utf8());
        }
    }

    @NonNull
    private <T> Call setupCall(@NonNull T url,
                               @Nullable BuilderModifier builderModifier) throws IOException {
        final Request.Builder builder = new Request.Builder().url(url.toString());

        if (builderModifier != null) {
            builderModifier.modify(builder);
        }

        return okHttpClient.newCall(builder.build());
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
    @RequiresPermission(Manifest.permission.INTERNET)
    private Response execute(@NonNull Call call) throws IOException {
        final Response response = call.execute();

        if (response.isRedirect()) {
            final String location = response.headers().get("Location");

            RCLog.d(getOrigin(), "Following HTTP redirect to " + location);
            return get(location);
        }
        if (!response.isSuccessful()) {
            String s = "Unexpected response code " + response;
            IOException e = new IOException(s);

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
     * @return <code>true</code> if connected or currently connecting to WIFI
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public boolean isWifi() {
        SupplicantState s = wifiManager.getConnectionInfo().getSupplicantState();
        NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);

        return state == NetworkInfo.DetailedState.CONNECTED ||
                state == NetworkInfo.DetailedState.OBTAINING_IPADDR;
    }

    @NonNull
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public NetType getNetworkType() {
        switch (telephonyManager.getNetworkType()) {
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

    /**
     * Functional interface to do something to an OkHttp request builder before dispatch
     */
    private interface BuilderModifier {
        void modify(Request.Builder builder);
    }
}
