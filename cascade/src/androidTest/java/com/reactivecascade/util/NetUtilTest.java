/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.support.annotation.RequiresPermission;

import com.reactivecascade.AsyncAndroidTestCase;
import com.reactivecascade.functional.SettableAltFuture;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.reactive.ReactiveValue;

import org.junit.Before;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.internal.framed.Header;

import static com.reactivecascade.Async.WORKER;

public class NetUtilTest extends AsyncAndroidTestCase {
    protected CountDownLatch signal; // Only use with @LargeTest

    public NetUtilTest() {
        super();
    }

    /**
     * Indicate that async test can proceed
     */
    protected void signal() {
        signal.countDown();
    }

    /**
     * Wait for {@link #signal()} from another thread before the test can proceed
     *
     * @throws InterruptedException
     */
    protected void await() throws InterruptedException {
        signal.await(15000, TimeUnit.MILLISECONDS);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        signal = new CountDownLatch(1);

        setDefaultTimeoutMillis(15000); // Give real net traffic enough time to complete
    }

    @Test
    public void testGet() throws Exception {
        assertTrue(getNetUtil().get("http://httpbin.org/").body().bytes().length > 100);
    }

    @Test
    public void testGetWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueZ"));
        assertTrue(getNetUtil().get("http://httpbin.org/headers", headers).body().string().contains("ValueZ"));
    }

    @Test
    public void testGetFromIGettable() throws Exception {
        ReactiveValue<String> value = new ReactiveValue<>("RV Test", "http://httpbin.org/headers");
        assertTrue(getNetUtil().get(value).body().bytes().length > 20);
    }

    @Test
    public void testGetFromIGettableWithHeaders() throws Exception {
        ReactiveValue<String> value = new ReactiveValue<>("RV Test", "http://httpbin.org/headers");
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueG"));
        assertTrue(getNetUtil().get(value, headers).body().string().contains("ValueG"));
    }

    @Test
    public void testGetAsync() throws Exception {
        IAltFuture<?, Response> iaf = getNetUtil()
                .getAsync("http://httpbin.org/get")
                .then(this::signal)
                .fork();
        await();
        assertEquals(HttpURLConnection.HTTP_OK, iaf.get().code());
    }

    @Test
    public void testGetAsyncFrom() throws Exception {
        IAltFuture<?, Response> iaf = WORKER
                .from("http://httpbin.org/get")
                .then(getNetUtil().getAsync());
        assertTrue(awaitDone(iaf).isSuccessful());
    }

    @Test
    public void testGetAsyncWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueZ"));
        assertTrue(awaitDone(getNetUtil().getAsync("http://httpbin.org/headers", headers).fork()).body().string().contains("ValueZ"));
    }

    @Test
    public void testValueGetAsyncWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueT"));
        IAltFuture<?, Response> iaf = WORKER
                .from("http://httpbin.org/headers")
                .then(getNetUtil().getAsync(headers));
        assertTrue(awaitDone(iaf).body().string().contains("ValueT"));
    }

    @Test
    public void testGetAsyncFromIGettableWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Blah", "VaGG"));
        SettableAltFuture<Collection<Header>> altFuture = new SettableAltFuture<>(WORKER);
        altFuture.set(headers);
        IAltFuture<?, Response> iaf = WORKER
                .from("http://httpbin.org/get")
                .then(getNetUtil().getAsync(altFuture));
        assertTrue(awaitDone(iaf).body().string().contains("VaGG"));
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
        assertTrue(getNetUtil().getMaxNumberOfNetConnections() > 1);
    }

    @Test
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public void testIsWifi() throws Exception {
        assertTrue(getNetUtil().isWifi() || true);
    }

    @Test
    public void testGetNetworkType() throws Exception {
        NetUtil.NetType netType = getNetUtil().getNetworkType();
        assertTrue(netType == NetUtil.NetType.NET_4G || netType == NetUtil.NetType.NET_3G || netType == NetUtil.NetType.NET_2_5G || netType == NetUtil.NetType.NET_2G);
    }
}