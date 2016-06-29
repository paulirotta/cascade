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
package com.reactivecascade.icebox;

import android.webkit.WebView;

import com.reactivecascade.Async;
import com.reactivecascade.i.IAspect;
import com.reactivecascade.rest.RESTService;

import java.io.IOException;

/**
 * Asynchronous access to an Javascript REST API running in a Webview
 *
 */
public class WebViewRESTService extends RESTService<String> {
    private static final String TAG = Async.class.getSimpleName();
    private final WebView webView;

    /**
     * Create a new REST service using the specified asynchronous implementation
     *
     * @param readIAspect
     */
    public WebViewRESTService(WebView webView, IAspect readIAspect, IAspect writeIAspect) {
        super(readIAspect, writeIAspect);

        this.webView = webView;
    }

    @Override
    protected String get(String key) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException(TAG + " get(key, getValue) was passed a null key");
        }

        Async.e(TAG, "TODO WebviewService.get(URI)"); //TODO WebviewService.get(URI)
        return null;
    }

    @Override
    protected void put(String key, String value) throws IOException {
        if (key == null || value == null) {
            throw new IllegalArgumentException(TAG + " put(key, getValue) was passed a null key or getValue");
        }

        Async.ee(TAG, "TODO WebviewService.put(URI, String)"); //TODO WebviewService.put(URI, String)
    }

    @Override
    protected boolean delete(String key) throws IOException {
        if (key == null) {
            Async.throwIllegalArgumentException(TAG, " delete(key, getValue) was passed a null key");
        }
        Async.ee(TAG, "TODO WebviewService.delete(URI)"); //TODO WebviewService.delete(URI)

        return false;
    }

    @Override
    protected void post(String key, String value) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException(TAG + " post(key, getValue) was passed a null key");
        }
        if (value == null) {
            throw new IllegalArgumentException(TAG + " post(key, getValue) was passed a null getValue");
        }
        Async.ee(TAG, "TODO WebviewService.post(URI, getValue)"); //TODO WebviewService.post(URI, getValue)

        //clearCache();
    }
}
