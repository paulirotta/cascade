/*
 * Copyright (c) 2015 Futurice GmbH. All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.futurice.cascade.rest;

import android.content.Context;
import android.support.annotation.NonNull;

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.functional.IAltFuture;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.futurice.cascade.Async.FILE;
import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.throwRuntimeException;

public final class FileUtil {
    private static final int BUFFER_SIZE = 16384;
    private final Context context;
    private final int mode;
    private final ImmutableValue<String> origin;

    public FileUtil(
            @NonNull final Context context,
            final int mode) {
        this.context = context;
        this.mode = mode;
        origin = originAsync();
    }

    @NonNull
    public <IN> IAltFuture<IN, IN> writeFileAsync(
            @NonNull final String fileName,
            @NonNull final byte[] bytes) {
        return FILE.then(() -> {
            writeFile(fileName, bytes);
        });
    }

    @NonNull
    public IAltFuture<String, byte[]> writeFileAsync(
            @NonNull final byte[] bytes) {
        return FILE.map(fileName -> {
            writeFile(fileName, bytes);
            return bytes;
        });
    }

    @NonNull
    public IAltFuture<byte[], byte[]> writeFileAsync(
            @NonNull final String fileName) {
        return FILE.then(bytes -> {
            writeFile(fileName, bytes);
        });
    }

    public void writeFile(
            @NonNull final String fileName,
            @NonNull final byte[] bytes) {
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = context.openFileOutput(fileName, mode);
            fileOutputStream.write(bytes);
        } catch (FileNotFoundException e) {
            final String s = "Can not locate FILE: " + fileName;
            dd(origin, s);
            throwRuntimeException(origin, s, e);
        } catch (IOException e) {
            final String s = "Can not write FILE: " + fileName;
            dd(origin, s);
            throwRuntimeException(origin, s, e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    ee(origin, "Can not close FILE output stream", e);
                }
            }
        }
    }

    @NonNull
    public IAltFuture<String, byte[]> readFileAsync() {
        return FILE.map(this::readFile);
    }

    @NonNull
    public IAltFuture<?, byte[]> readFileAsync(@NonNull final String fileName) {
        return FILE.then(() -> {
            return readFile(fileName);
        });
    }

    @NonNull
    public byte[] readFile(@NonNull final String fileName) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = context.openFileInput(fileName);

            final byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            for (; ; ) {
                count = fileInputStream.read(buffer, 0, buffer.length);
                if (count < 0) {
                    break;
                }
                bos.write(buffer, 0, count);
            }
        } catch (FileNotFoundException e) {
            final String s = "Can not locate FILE: " + fileName;
            dd(origin, s);
            throwRuntimeException(origin, s, e);
        } catch (IOException e) {
            final String s = "Can not read FILE: " + fileName;
            dd(origin, s);
            throwRuntimeException(origin, s, e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    ee(origin, "Can not close FILE input stream: " + fileName, e);
                }
            }
        }

        return bos.toByteArray();
    }

    public boolean deleteFile(@NonNull final String fileName) {
        return context.deleteFile(fileName);
    }

    @NonNull
    public IAltFuture<?, Boolean> deleteFileAsync(@NonNull final String fileName) {
        return FILE.then(() -> {
            return deleteFile(fileName);
        });
    }

    @NonNull
    public IAltFuture<String, Boolean> deleteFileAsync() {
        return FILE.map(this::deleteFile);
    }
}
