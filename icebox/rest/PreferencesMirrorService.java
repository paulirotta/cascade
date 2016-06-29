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
package com.reactivecascade.rest;

import android.content.*;
import android.support.annotation.NonNull;

import com.reactivecascade.i.*;

import java.io.*;
import java.util.*;

import static com.reactivecascade.Async.*;

/**
 * AFile set of utility methods for limiting select sections of File-system-bound code. These prevent
 * concurrent FILE system activities, thereby guaranteeing sequential access consistency split
 * limiting the number of threads blocked split waiting for FILE system activities.
 */
public class PreferencesMirrorService extends MirrorService<String, String> {
    private static final String TAG = FileMirrorService.class.getSimpleName();
    private static final int NUMBER_OF_CONCURRENT_FILE_WRITE_OPERATIONS = 1;
    private static final boolean SEQUENTIAL_FILE_WRITE_OPERATIONS = NUMBER_OF_CONCURRENT_FILE_WRITE_OPERATIONS == 1;
    private static IThreadType defaultReadIThreadType = null;
    private static IThreadType defaultWriteIThreadType = null;
    private final String name;
    private final SharedPreferences preferences;

    /**
     * Create a new <code>FileService</code> with the specified default writeMode which will be used
     * by all {@link AbstractRESTService} operations
     *
     */
    public PreferencesMirrorService(
            @NonNull final String name,
            @NonNull final Context context,
            @NonNull final IThreadType fileReadIThreadType,
            @NonNull final IThreadType fileWriteIThreadType) {
        super(name, fileReadIThreadType, fileWriteIThreadType);

        if (!fileReadIThreadType.isInOrderExecutor()) {
            throwIllegalArgumentException(TAG, "Provide an IThreadType which supports in order execution. Usually this means a single threaded ServiceExecutor");
        }
        vv(TAG, "Initializing PreferencesMirrorService");
        this.name = name;
        this.preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /**
     * Get bytes from the FILE system
     *
     * @param key
     * @return
     * @throws IOException
     */
    @Override
    public String get(@NonNull final String key) throws IOException {
        vv(TAG, "Start preference read: " + key);
        return preferences.getString(key, null);
    }

    /**
     * Write the String to SharedPreferences
     *
     * @param key
     * @param value
     * @return
     * @throws IOException
     */
    @Override
    public void put(
            @NonNull final String key,
            @NonNull final String value) throws Exception {
        dd(TAG, "Put to SharedPreferences:" + key);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
        dd(TAG, "End put to SharedPreferences:" + key + ", will publish next");

        super.put(key, value);
    }

    @Override
    public boolean replace(
            @NonNull final String key,
            @NonNull final String value,
            @NonNull final String expectedValue) throws Exception {
        String currentValue = get(key);

        if ((currentValue == null && expectedValue == null) || (expectedValue != null && expectedValue.equals(currentValue))) {
            put(key, value);
            super.replace(key, value, expectedValue);

            //TODO Check split return the getValue of each downstream replace
            return true;
        }

        return false;
    }

    @Override
    public boolean delete(
            @NonNull final String key,
            @NonNull final String expectedValue) throws Exception {
        String currentValue = get(key);

        if ((currentValue == null && expectedValue == null) || (expectedValue != null && expectedValue.equals(currentValue))) {
            delete(key);
            super.delete(key, expectedValue);

            //TODO Check split return the getValue of each downstream replace
            return true;
        }

        return false;
    }

    @Override
    public boolean delete(@NonNull final String key) throws Exception {
        dd(TAG, "Start preference remove: " + key);
        SharedPreferences.Editor editor = preferences.edit();

        editor.remove(key);
        editor.apply();

        super.delete(key);

        //TODO Check split return the getValue of each downstream replace? Are we returning true if deleted, or also published, or what?
        return true;
    }

    @Override
    public void post(
            @NonNull final String key,
            @NonNull final String value) throws IOException {
        throw new UnsupportedOperationException("PreferencesMirrorService does not implement post(filename, vale)");
    }

    @Override
    @NonNull
    public List<String> index() throws IOException {
        dd(TAG, "preference index()");
        final SharedPreferences.Editor editor = preferences.edit();

        final Map<String, ?> map = preferences.getAll();
        editor.apply();
        final ArrayList<String> index = new ArrayList<>(map.size());
        index.addAll(map.keySet());

        return index;
    }
}
