/*
The MIT License (MIT)

Copyright (c) 2015 Futurice Oy and individual contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package com.futurice.cascade.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.futurice.cascade.AsyncAndroidTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class UIExecutorServiceTest extends AsyncAndroidTestCase {
    int handleMessageCount;
    int dispatchMessageCount;
    int sendCount;

    UIExecutorService uiExecutorService;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        try {
            Looper.prepare();
        } catch (RuntimeException e) {
            // Only looper this test thread once
        }
        uiExecutorService = new UIExecutorService(new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleMessageCount++;
            }

            /**
             * Handle system messages here.
             */
            public void dispatchMessage(@NonNull Message msg) {
                super.dispatchMessage(msg);
                dispatchMessageCount++;
            }

            public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) {
                sendCount++;
                return super.sendMessageAtTime(msg, uptimeMillis);
            }
        });
    }

    @Test
    public void testIsShutdown() throws Exception {
        assertFalse(uiExecutorService.isShutdown());
    }

    @Test
    public void testIsTerminated() throws Exception {
        assertFalse(uiExecutorService.isTerminated());
    }

    @Test
    public void testSubmitCallable() throws Exception {
        uiExecutorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        });
        assertEquals("testSubmitCallable sends 1", 1, this.sendCount);
    }

    @Test
    public void testSubmitRunnable() throws Exception {
        uiExecutorService.submit(new Runnable() {
            @Override
            public void run() {

            }
        });
        assertEquals("testSubmitRunnable sends 1", 1, this.sendCount);
    }

    @Test
    public void testInvokeAll() throws Exception {

    }

    @Test
    public void testInvokeAll1() throws Exception {

    }

/*    public void testInvokeAnyCallable() throws Exception {
        ArrayList<Callable<String>> callableList = new ArrayList<>();
        callableList.add(new Callable() {
            @Override
            public String call() throws Exception {
                return null;
            }
        });
        callableList.add(new Callable() {
            @Override
            public String call() throws Exception {
                return null;
            }
        });
        uiExecutorService.invokeAny(callableList);
        assertTrue("Send at least 1", sendCount > 0);
    }*/

    @Test
    public void testInvokeAnyRunnable() throws Exception {

    }

    @Test
    public void testExecute() throws Exception {
        uiExecutorService.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
}
