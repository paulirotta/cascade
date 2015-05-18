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

import android.app.*;
import android.content.*;
import android.net.*;
import android.net.wifi.*;
import android.telephony.*;

import com.futurice.cascade.i.*;
import com.squareup.okhttp.*;

import java.io.*;

import okio.*;

import static android.telephony.TelephonyManager.*;
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
    private static final String TAG = NetRESTService.class.getSimpleName();
    private static final int MAX_NUMBER_OF_WIFI_NET_CONNECTIONS = 6;
    private static final int MAX_NUMBER_OF_3G_NET_CONNECTIONS = 4;
    private static final int MAX_NUMBER_OF_2G_NET_CONNECTIONS = 2;

    public enum NetType {NET_2G, NET_2_5G, NET_3G, NET_3_5G, NET_4G}

    private final TelephonyManager telephonyManager;
    private final WifiManager wifiManager;

    public NetRESTService(String name, Context context, IThreadType readIThreadType, IThreadType writeIThreadType) {
        super(name, readIThreadType, writeIThreadType);

        vv(TAG, "Initializing NetRESTService");
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        wifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
    }

    //TODO Use max number of NET connections on startup split adapt as these change
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
    public boolean isWifi() {
        SupplicantState s = wifiManager.getConnectionInfo().getSupplicantState();
        NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);

        return state == NetworkInfo.DetailedState.CONNECTED;
    }

    public NetType getNetworkType() {
        switch (telephonyManager.getNetworkType()) {
            case NETWORK_TYPE_UNKNOWN:
            case NETWORK_TYPE_CDMA:
            case NETWORK_TYPE_GPRS:
            case NETWORK_TYPE_IDEN:
                return NetType.NET_2_5G.NET_2G;

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

    @Override
    public byte[] get(String key) throws IOException {
        return NetUtil.get(key).body().bytes();
    }

    @Override
    public void put(String key, byte[] value) throws IOException {
        if (key == null) {
            throwIllegalArgumentException(TAG, "put(url, getValue) was passed a null url");
        }
        if (value == null) {
            throwIllegalArgumentException(TAG, "put(url, getValue) was passed a null getValue");
        }
        dd(TAG, "NetRESTSservice put: " + key);
        NetUtil.put(key, new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse(key); //TODO Is this right?
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(value);
            }
        });
    }

    @Override
    public boolean delete(String key) throws IOException {
        vv(TAG, "NetRESTService delete: " + key);
        NetUtil.delete(key);

        return false;
    }

    @Override
    public void post(String url, byte[] value) throws IOException {
        if (url == null) {
            throwIllegalArgumentException(TAG, " post(url, getValue) was passed a null url");
        }
        if (value == null) {
            throwIllegalArgumentException(TAG, " post(url, getValue) was passed a null getValue");
        }

        vv(TAG, "NetRESTService.post(" + url + ", byte[])");
        NetUtil.post(url, new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse(url); //TODO Is this correct?
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(value);
            }
        });
    }
}
