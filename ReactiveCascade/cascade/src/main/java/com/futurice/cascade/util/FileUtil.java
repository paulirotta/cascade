/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.util;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.i.IAsyncOrigin;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.futurice.cascade.Async.FILE;

public final class FileUtil extends Origin {
    private static final int BUFFER_SIZE = 16384;
    @NonNull
    private final Context mContext;
    @FileMode
    private final int mMode;
    public FileUtil(
            @NonNull final Context context,
            @FileMode final int mode) {
        this.mContext = context;
        this.mMode = mode;
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <IN> IAltFuture<IN, IN> writeAsync(
            @NonNull final String fileName,
            @NonNull final byte[] bytes) {
        return FILE.then(() -> {
            write(fileName, bytes);
        });
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<String, byte[]> writeAsync(
            @NonNull final byte[] bytes) {
        return FILE.map(fileName -> {
            write(fileName, bytes);
            return bytes;
        });
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<byte[], byte[]> writeAsync(
            @NonNull final String fileName) {
        return FILE.then(bytes -> {
            write(fileName, bytes);
        });
    }

    @WorkerThread
    public void write(
            @NonNull final String fileName,
            @NonNull final byte[] bytes) {
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = mContext.openFileOutput(fileName, mMode);
            fileOutputStream.write(bytes);
        } catch (FileNotFoundException e) {
            final String s = "Can not locate FILE: " + fileName;
            CLog.d(this, s);
            CLog.throwRuntimeException(this, s, e);
        } catch (IOException e) {
            final String s = "Can not write FILE: " + fileName;
            CLog.d(this, s);
            CLog.throwRuntimeException(this, s, e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    CLog.e(this, "Can not close FILE output stream", e);
                }
            }
        }
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<String, byte[]> readAsync() {
        return FILE.map(this::read);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, byte[]> readAsync(@NonNull final String fileName) {
        return FILE.then(() -> {
            return read(fileName);
        });
    }

    @NonNull
    @WorkerThread
    public byte[] read(@NonNull final String fileName) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = mContext.openFileInput(fileName);

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
            CLog.throwRuntimeException(this, "Can not locate FILE: " + fileName, e);
        } catch (IOException e) {
            CLog.throwRuntimeException(this, "Can not read FILE: " + fileName, e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    CLog.e(this, "Can not close FILE input stream: " + fileName, e);
                }
            }
        }

        return bos.toByteArray();
    }

    @WorkerThread
    public boolean delete(@NonNull final String fileName) {
        return mContext.deleteFile(fileName);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, Boolean> deleteAsync(@NonNull final String fileName) {
        return FILE.then(() -> {
            return delete(fileName);
        });
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<String, Boolean> deleteAsync() {
        return FILE.map(this::delete);
    }

    @IntDef({Context.MODE_PRIVATE, Context.MODE_APPEND})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FileMode {
    }
}
