/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;

import com.reactivecascade.i.IAltFuture;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.reactivecascade.Async.FILE;

public final class FileUtil extends Origin {
    private static final int BUFFER_SIZE = 16384;

    private static final String WRITE_PERMISSION_SECURITY_EXCEPTION = "Request permission WRITE_EXTERNAL_STORAGE";
    private static final String READ_PERMISSION_SECURITY_EXCEPTION = "Request permission READ_EXTERNAL_STORAGE";

    @IntDef({Context.MODE_PRIVATE, Context.MODE_APPEND})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FileMode {
    }

    @NonNull
    private final Context context;

    @FileMode
    private final int mode;

    public FileUtil(@NonNull Context context,
                    @FileMode int mode) {
        this.context = context;
        this.mode = mode;
    }

    public boolean checkWritePermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    public boolean checkReadPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public <IN> IAltFuture<IN, IN> writeAsync(@NonNull String fileName,
                                              @NonNull byte[] bytes) {
        if (!checkWritePermission()) {
            throw new SecurityException(WRITE_PERMISSION_SECURITY_EXCEPTION);
        }

        return FILE.then(
                () -> {
                    write(fileName, bytes);
                });
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public IAltFuture<String, byte[]> writeAsync(@NonNull byte[] bytes) {
        if (!checkWritePermission()) {
            throw new SecurityException(WRITE_PERMISSION_SECURITY_EXCEPTION);
        }

        return FILE.map(
                fileName -> {
                    write(fileName, bytes);
                    return bytes;
                });
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public IAltFuture<byte[], byte[]> writeAsync(@NonNull String fileName) {
        if (!checkWritePermission()) {
            throw new SecurityException(WRITE_PERMISSION_SECURITY_EXCEPTION);
        }

        return FILE.then(
                bytes -> {
                    write(fileName, bytes);
                });
    }

    @WorkerThread
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void write(@NonNull String fileName,
                      @NonNull byte[] bytes) {
        if (!checkWritePermission()) {
            throw new SecurityException(WRITE_PERMISSION_SECURITY_EXCEPTION);
        }

        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = context.openFileOutput(fileName, mode);
            fileOutputStream.write(bytes);
        } catch (FileNotFoundException e) {
            final String s = "Can not locate FILE: " + fileName;
            RCLog.d(this, s);
            RCLog.throwRuntimeException(this, s, e);
        } catch (IOException e) {
            final String s = "Can not write FILE: " + fileName;
            RCLog.d(this, s);
            RCLog.throwRuntimeException(this, s, e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    RCLog.e(this, "Can not close FILE output stream", e);
                }
            }
        }
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public IAltFuture<String, byte[]> readAsync() {
        if (!checkReadPermission()) {
            throw new SecurityException(READ_PERMISSION_SECURITY_EXCEPTION);
        }

        return FILE.map(this::read);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public IAltFuture<?, byte[]> readAsync(@NonNull String fileName) {
        if (!checkReadPermission()) {
            throw new SecurityException(READ_PERMISSION_SECURITY_EXCEPTION);
        }

        return FILE.then(
                () -> {
                    return read(fileName);
                });
    }

    @NonNull
    @WorkerThread
    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public byte[] read(@NonNull String fileName) {
        if (!checkReadPermission()) {
            throw new SecurityException(READ_PERMISSION_SECURITY_EXCEPTION);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
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
            RCLog.throwRuntimeException(this, "Can not locate FILE: " + fileName, e);
        } catch (IOException e) {
            RCLog.throwRuntimeException(this, "Can not read FILE: " + fileName, e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    RCLog.e(this, "Can not close FILE input stream: " + fileName, e);
                }
            }
        }

        return bos.toByteArray();
    }

    @WorkerThread
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public boolean delete(@NonNull String fileName) {
        if (!checkWritePermission()) {
            throw new SecurityException(WRITE_PERMISSION_SECURITY_EXCEPTION);
        }

        return context.deleteFile(fileName);
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public IAltFuture<?, Boolean> deleteAsync(@NonNull final String fileName) {
        if (!checkWritePermission()) {
            throw new SecurityException(WRITE_PERMISSION_SECURITY_EXCEPTION);
        }

        return FILE.then(
                () -> {
                    return delete(fileName);
                });
    }

    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public IAltFuture<String, Boolean> deleteAsync() {
        if (!checkWritePermission()) {
            throw new SecurityException(WRITE_PERMISSION_SECURITY_EXCEPTION);
        }

        return FILE.map(this::delete);
    }
}
