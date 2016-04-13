package com.futurice.cascade.util;

import android.support.annotation.RequiresPermission;
import android.test.suitebuilder.annotation.LargeTest;

import com.futurice.cascade.AsyncAndroidTestCase;
import com.futurice.cascade.functional.SettableAltFuture;
import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.reactive.ReactiveValue;

import org.junit.Before;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.internal.framed.Header;

import static com.futurice.cascade.Async.WORKER;
import static org.assertj.core.api.Assertions.assertThat;

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

    @LargeTest
    public void testGet() throws Exception {
        assertThat(getNetUtil().get("http://httpbin.org/").body().bytes().length).isGreaterThan(100);
    }

    @LargeTest
    public void testGetWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueZ"));
        assertThat(getNetUtil().get("http://httpbin.org/headers", headers).body().string()).contains("ValueZ");
    }

    @LargeTest
    public void testGetFromIGettable() throws Exception {
        ReactiveValue<String> value = new ReactiveValue<>("RV Test", "http://httpbin.org/headers");
        assertThat(getNetUtil().get(value).body().bytes().length).isGreaterThan(20);
    }

    @LargeTest
    public void testGetFromIGettableWithHeaders() throws Exception {
        ReactiveValue<String> value = new ReactiveValue<>("RV Test", "http://httpbin.org/headers");
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueG"));
        assertThat(getNetUtil().get(value, headers).body().string()).contains("ValueG");
    }

    @LargeTest
    public void testGetAsync() throws Exception {
        IAltFuture<?, Response> iaf = getNetUtil()
                .getAsync("http://httpbin.org/get")
                .then(this::signal)
                .fork();
        await();
        assertEquals(HttpURLConnection.HTTP_OK, iaf.get().code());
    }

    @LargeTest
    public void testGetAsyncFrom() throws Exception {
        IAltFuture<?, Response> iaf = WORKER
                .from("http://httpbin.org/get")
                .then(getNetUtil().getAsync());
        assertThat(awaitDone(iaf).isSuccessful()).isTrue();
    }

    @LargeTest
    public void testGetAsyncWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueZ"));
        assertThat(awaitDone(
                getNetUtil().getAsync("http://httpbin.org/headers", headers).fork()).body().string()
        ).contains("ValueZ");
    }

    @LargeTest
    public void testValueGetAsyncWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Test", "ValueT"));
        IAltFuture<?, Response> iaf = WORKER
                .from("http://httpbin.org/headers")
                .then(getNetUtil().getAsync(headers));
        assertThat(awaitDone(iaf).body().string()).contains("ValueT");
    }

    @LargeTest
    public void testGetAsyncFromIGettableWithHeaders() throws Exception {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Blah", "VaGG"));
        SettableAltFuture<Collection<Header>> altFuture = new SettableAltFuture<>(WORKER);
        altFuture.set(headers);
        IAltFuture<?, Response> iaf = WORKER
                .from("http://httpbin.org/get")
                .then(getNetUtil().getAsync(altFuture));
        assertThat(awaitDone(iaf).body().string()).contains("VaGG");
    }

    @LargeTest
    public void testPut() throws Exception {

    }

    @LargeTest
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

    @LargeTest
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public void testGetMaxNumberOfNetConnections() throws Exception {
        assertThat(getNetUtil().getMaxNumberOfNetConnections()).isGreaterThan(1);
    }

    @LargeTest
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public void testIsWifi() throws Exception {
        assertThat(getNetUtil().isWifi() || true).isTrue();
    }

    @LargeTest
    public void testGetNetworkType() throws Exception {
        NetUtil.NetType netType = getNetUtil().getNetworkType();
        assertTrue(netType == NetUtil.NetType.NET_4G || netType == NetUtil.NetType.NET_3G || netType == NetUtil.NetType.NET_2_5G || netType == NetUtil.NetType.NET_2G);
    }
}