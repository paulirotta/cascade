/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.support.annotation.RequiresPermission;
import android.support.test.runner.AndroidJUnit4;

import com.reactivecascade.Async;
import com.reactivecascade.AsyncBuilder;
import com.reactivecascade.DefaultCascadeIntegrationTest;
import com.reactivecascade.functional.SettableAltFuture;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.reactive.ReactiveValue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;

import okhttp3.Response;
import okhttp3.internal.framed.Header;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class NetUtilIntegrationTest extends DefaultCascadeIntegrationTest {
    private static NetUtil netUtil;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        if (netUtil == null) {
            netUtil = new NetUtil(getContext(), AsyncBuilder.netRead, AsyncBuilder.netWrite);
        }
        defaultTimeoutMillis = 5000; // Give real net traffic enough time to complete
    }

    @AfterClass
    public static void cleanupNetUtil() throws Exception {
        netUtil = null;
    }

    @Test
    public void testGet() throws Exception {
        assertTrue(netUtil.get("http://httpbin.org/").body().bytes().length > 100);
    }

    @Test
    public void testGetWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueZ"));
        assertTrue(netUtil.get("http://httpbin.org/headers", headers).body().string().contains("ValueZ"));
    }

    @Test
    public void testGetFromIGettable() throws Exception {
        ReactiveValue<String> value = new ReactiveValue<>("RV Test", AsyncBuilder.worker, null, null);
        value.set("http://httpbin.org/headers");
        int length = netUtil.get(value).body().bytes().length;
        assertTrue(length > 20);
    }

    @Test
    public void testGetFromIGettableWithHeaders() throws Exception {
        ReactiveValue<String> value = new ReactiveValue<>("RV Test", AsyncBuilder.worker, null, null);
        value.set("http://httpbin.org/headers");
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueG"));
        String s = netUtil.get(value, headers).body().string();
        assertTrue(s.contains("ValueG"));
    }

    @Test
    public void testGetAsync() throws Exception {
        IAltFuture<?, Response> iaf = netUtil
                .getAsync("http://httpbin.org/get")
                .fork();
        await(iaf);
        assertEquals(HttpURLConnection.HTTP_OK, iaf.get().code());
    }

    @Test
    public void testGetAsyncFrom() throws Exception {
        IAltFuture<?, Response> iaf = AsyncBuilder.worker
                .from("http://httpbin.org/get")
                .then(netUtil.getAsync())
                .fork();
        assertTrue(await(iaf).isSuccessful());
    }

    @Test
    public void testGetAsyncWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueZ"));
        IAltFuture<String, Response> iaf = netUtil
                .getAsync("http://httpbin.org/headers", headers)
                .fork();
        assertTrue(await(iaf).body().string().contains("ValueZ"));
    }

    @Test
    public void testValueGetAsyncWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueT"));
        IAltFuture<String, Response> iaf = AsyncBuilder.worker
                .from("http://httpbin.org/headers")
                .then(netUtil.getAsync(headers))
                .fork();
        assertTrue(await(iaf).body().string().contains("ValueT"));
    }

    @Test
    public void testGetAsyncFromIGettableWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Blah", "VaGG"));
        SettableAltFuture<Collection<Header>> altFuture = new SettableAltFuture<>(AsyncBuilder.worker);
        altFuture.set(headers);
        IAltFuture<?, Response> iaf = AsyncBuilder.worker
                .from("http://httpbin.org/get")
                .then(netUtil.getAsync(altFuture))
                .fork();
        assertTrue(await(iaf).body().string().contains("VaGG"));
    }

    @Test
    public void testPut() throws Exception {

    }

    @Test
    public void testPut1() throws Exception {

    }

    @Test
    public void testPutAsync() throws Exception {

    }

    @Test
    public void testPutAsync1() throws Exception {

    }

    @Test
    public void testPutAsync2() throws Exception {

    }

    @Test
    public void testPutAsync3() throws Exception {

    }

    @Test
    public void testPutAsync4() throws Exception {

    }

    @Test
    public void testPutAsync5() throws Exception {

    }

    @Test
    public void testPost() throws Exception {

    }

    @Test
    public void testPostAsync() throws Exception {

    }

    @Test
    public void testPostAsync1() throws Exception {

    }

    @Test
    public void testPostAsync2() throws Exception {

    }

    @Test
    public void testPost1() throws Exception {

    }

    @Test
    public void testPostAsync3() throws Exception {

    }

    @Test
    public void testPostAsync4() throws Exception {

    }

    @Test
    public void testPostAsync5() throws Exception {

    }

    @Test
    public void testPost2() throws Exception {

    }

    @Test
    public void testDeleteAsync() throws Exception {

    }

    @Test
    public void testDeleteAsync1() throws Exception {

    }

    @Test
    public void testDelete() throws Exception {

    }

    @Test
    public void testDeleteAsync2() throws Exception {

    }

    @Test
    public void testDeleteAsync3() throws Exception {

    }

    @Test
    public void testDelete1() throws Exception {

    }

    @Test
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public void testGetMaxNumberOfNetConnections() throws Exception {
        assertTrue(netUtil.getMaxNumberOfNetConnections() > 1);
    }

    @Test
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public void testIsWifi() throws Exception {
        assertFalse(netUtil.isWifi());
    }

    @Test
    public void testGetNetworkType() throws Exception {
        NetUtil.NetType netType = netUtil.getNetworkType();
        assertTrue(netType == NetUtil.NetType.NET_4G ||
                netType == NetUtil.NetType.NET_3G ||
                netType == NetUtil.NetType.NET_2_5G ||
                netType == NetUtil.NetType.NET_2G);
    }
}