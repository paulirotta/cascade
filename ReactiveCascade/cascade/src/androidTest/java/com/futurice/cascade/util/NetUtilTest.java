package com.futurice.cascade.util;

import android.test.suitebuilder.annotation.LargeTest;

import com.futurice.cascade.AsyncAndroidTestCase;
import com.futurice.cascade.i.functional.IAltFuture;
import com.squareup.okhttp.Response;

import static com.futurice.cascade.Async.SERIAL_WORKER;
import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the NetUtil class
 *
 * Created by phou on 6/2/2015.
 */
public class NetUtilTest extends AsyncAndroidTestCase {

    public NetUtilTest() {
        super(SERIAL_WORKER);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testExecAfterPendingReadsAsync() throws Exception {

    }

    @LargeTest
    public void testGetAsync() throws Exception {
        IAltFuture<?, Response> iaf = netUtil.getAsync("http://httpbin.org/get")
                .fork();
        Response response = awaitDone(iaf);
        assertThat(response.isSuccessful()).isTrue();
    }

    public void testGetAsync1() throws Exception {

    }

    public void testGet() throws Exception {

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