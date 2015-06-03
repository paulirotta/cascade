package com.futurice.cascade.util;

import android.test.suitebuilder.annotation.LargeTest;

import com.futurice.cascade.i.functional.IAltFuture;
import com.futurice.cascade.test.AsyncAndroidTestCase;
import com.squareup.okhttp.Response;

import static org.assertj.core.api.Assertions.*;
import static com.futurice.cascade.Async.*;

/**
 * Integration tests for the NetUtil class
 *
 * Created by phou on 6/2/2015.
 */
public class NetUtilTest extends AsyncAndroidTestCase {

    public NetUtilTest() {
        super();
    }

    public void setUp() throws Exception {
        super.setUp();

        setDefaultTimeoutMillis(15000); // Give real net traffic enough time to complete
    }

    public void testExecAfterPendingReadsAsync() throws Exception {

    }

    @LargeTest
    public void testGetAsync() throws Exception {
        IAltFuture<?, Response> iaf = getNetUtil().getAsync("http://httpbin.org/get")
                .fork();
        assertThat(awaitDone(iaf).isSuccessful()).isTrue();
    }

    @LargeTest
    public void testGetAsyncFromWORKER() throws Exception {
        IAltFuture<?, Response> iaf = WORKER
                .from("http://httpbin.org/get")
                .then(getNetUtil().getAsync())
                .fork();
        assertThat(awaitDone(iaf).isSuccessful()).isTrue();
    }

    @LargeTest
    public void testGet() throws Exception {
        assertThat(getNetUtil().get("http://httpbin.org/").body().bytes().length).isGreaterThan(100);
    }

    public void testGet1() throws Exception {

    }

    public void testGetAsync2() throws Exception {

    }

    public void testPutAsync() throws Exception {

    }

    public void testPutAsync1() throws Exception {

    }

    public void testPutAsync2() throws Exception {

    }

    public void testPut() throws Exception {

    }

    public void testPutAsync3() throws Exception {

    }

    public void testPutAsync4() throws Exception {

    }

    public void testPutAsync5() throws Exception {

    }

    public void testPut1() throws Exception {

    }

    public void testPost() throws Exception {

    }

    public void testPostAsync() throws Exception {

    }

    public void testPostAsync1() throws Exception {

    }

    public void testPostAsync2() throws Exception {

    }

    public void testPost1() throws Exception {

    }

    public void testPostAsync3() throws Exception {

    }

    public void testPostAsync4() throws Exception {

    }

    public void testPostAsync5() throws Exception {

    }

    public void testPost2() throws Exception {

    }

    public void testDeleteAsync() throws Exception {

    }

    public void testDeleteAsync1() throws Exception {

    }

    public void testDelete() throws Exception {

    }

    public void testDeleteAsync2() throws Exception {

    }

    public void testDeleteAsync3() throws Exception {

    }

    public void testDelete1() throws Exception {

    }

    public void testGetMaxNumberOfNetConnections() throws Exception {

    }

    public void testIsWifi() throws Exception {

    }

    public void testGetNetworkType() throws Exception {

    }
}