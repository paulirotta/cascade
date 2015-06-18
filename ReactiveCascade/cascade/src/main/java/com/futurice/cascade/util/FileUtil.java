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
package com.futurice.cascade.util;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.active.IAltFuture;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.futurice.cascade.Async.*;

public final class FileUtil {
    @IntDef({Context.MODE_PRIVATE, Context.MODE_APPEND})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FileMode {}

    private static final int BUFFER_SIZE = 16384;
    private final Context context;
    @FileMode
    private final int mode;
    private final ImmutableValue<String> origin;

    public FileUtil(
            @NonNull @nonnull final Context context,
            @FileMode final int mode) {
        this.context = context;
        this.mode = mode;
        origin = originAsync();
    }

    @NonNull @nonnull
    public <IN> IAltFuture<IN, IN> writeAsync(
            @NonNull @nonnull final String fileName,
            @NonNull @nonnull final byte[] bytes) {
        return FILE.then(() -> {
            write(fileName, bytes);
        });
    }

    @NonNull @nonnull
    public IAltFuture<String, byte[]> writeAsync(
            @NonNull @nonnull final byte[] bytes) {
        return FILE.map(fileName -> {
            write(fileName, bytes);
            return bytes;
        });
    }

    @NonNull @nonnull
    public IAltFuture<byte[], byte[]> writeAsync(
            @NonNull @nonnull final String fileName) {
        return FILE.then(bytes -> {
            write(fileName, bytes);
        });
    }

    @WorkerThread
    public void write(
            @NonNull @nonnull final String fileName,
            @NonNull @nonnull final byte[] bytes) {
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

    @NonNull @nonnull
    public IAltFuture<String, byte[]> readAsync() {
        return FILE.map(this::read);
    }

    @NonNull @nonnull
    public IAltFuture<?, byte[]> readAsync(@NonNull @nonnull final String fileName) {
        return FILE.then(() -> {
            return read(fileName);
        });
    }

    @NonNull @nonnull
    @WorkerThread
    public byte[] read(@NonNull @nonnull final String fileName) {
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

    @WorkerThread
    public boolean delete(@NonNull @nonnull final String fileName) {
        return context.deleteFile(fileName);
    }

    @NonNull @nonnull
    public IAltFuture<?, Boolean> deleteAsync(@NonNull @nonnull final String fileName) {
        return FILE.then(() -> {
            return delete(fileName);
        });
    }

    @NonNull @nonnull
    public IAltFuture<String, Boolean> deleteAsync() {
        return FILE.map(this::delete);
    }
}
