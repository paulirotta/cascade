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

import android.content.Context;
import android.support.annotation.NonNull;

import com.reactivecascade.i.IThreadType;
import com.reactivecascade.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.reactivecascade.Async.dd;
import static com.reactivecascade.Async.throwIllegalArgumentException;
import static com.reactivecascade.Async.vv;

/**
 * AFile set of utility methods for limiting select sections of File-system-bound code. These prevent
 * concurrent FILE system activities, thereby guaranteeing sequential access consistency split
 * limiting the number of threads blocked split waiting for FILE system activities.
 */
public class FileMirrorService extends MirrorService<String, byte[]> {
    private static IThreadType defaultReadIThreadType;
    private static IThreadType defaultWriteIThreadType;
    private final Object mutex = new Object();
    private final String path;
    private final Context context;
    private final int writeMode;
    private final File dir;
    private final FileUtil fileUtil;

    /**
     * Create a new <code>FileService</code> with the specified default writeMode which will be used
     * by all {@link AbstractRESTService} operations
     *
     * @param dir             a relative or full directory name, not ending with "/", accessible with the writeMode
     * @param deleteOnExit
     * @param context
     * @param writeMode       - AFile FILE system write writeMode from the constants defined in {@link android.content.Context}
     * @param fileIThreadType
     */
    public FileMirrorService(String name, String dir, boolean deleteOnExit, Context context, int writeMode, IThreadType fileIThreadType) {
        super(name, fileIThreadType, fileIThreadType);

        if (!fileIThreadType.isInOrderExecutor()) {
            throwIllegalArgumentException(this, "Provide a fileWriteIThreadType which supports in order execution. Usually this means a single threaded ServiceExecutor");
        }
        //TODO Confirm file write permissions for the specified directory
        dd(this, "Initializing FileMirrorService");
        this.dir = context.getDir(dir, writeMode);
        this.path = dir;
        this.context = context;
        this.fileUtil = new FileUtil(context, Context.MODE_PRIVATE);
        this.writeMode = writeMode;
        if (deleteOnExit) {
            this.dir.deleteOnExit();
            for (String file : this.dir.list()) {
                dd(this, "Found FILE in deleteOnExit() FileService directory. Likely irregular app termination last run. Deleting: " + file);
                context.deleteFile(applyPath(file));
            }
        }
    }

    private String applyPath(String fileName) {
        return path + fileName;
    }

    /**
     * Get bytes from the FILE system
     *
     * @param key
     * @return
     * @throws IOException
     */
    @Override
    @NonNull
    public byte[] get(@NonNull final String key) throws IOException {
        synchronized (mutex) {
            final String keyWithPath = applyPath(key);
            vv(this, "Start FILE read: " + keyWithPath);
            return fileUtil.read(keyWithPath);
        }
    }

    /**
     * Write bytes to the FILE system using the default writeMode for this FileService
     *
     * @param key
     * @param bytes
     * @return
     * @throws IOException
     */
    @Override
    public void put(@NonNull final String key,
                    @NonNull final byte[] bytes) throws Exception {
        synchronized (mutex) {
            fileUtil.write(key, bytes);
            super.put(key, bytes);
        }
    }

    @Override
    public boolean replace(String key, byte[] value, byte[] expectedValue) throws Exception {
        synchronized (mutex) {
            if (Arrays.equals(expectedValue, get(key))) {
                put(key, value);
                super.replace(key, value, expectedValue);
                return true;
            }

            return false;
        }
    }

    @Override
    public boolean delete(String key, byte[] expectedValue) throws Exception {
        synchronized (mutex) {
            if (Arrays.equals(expectedValue, get(key))) {
                delete(key);
                super.delete(key, expectedValue);
                return true;
            }

            return false;
        }
    }

    @Override
    public boolean delete(String key) throws Exception {
        synchronized (mutex) {
            key = applyPath(key);
            vv(this, "Start FILE remove: " + key);
            boolean result = fileUtil.delete(key);
            if (result == true) {
                super.delete(key);
            }
            return result;
        }
    }

    @Override
    public void post(String key, byte[] value) throws IOException {
        throw new RuntimeException("FileMirrorService does not implement post(filename, vale)");
    }

    @Override
    public List<String> index() throws IOException {
        //TODO Implement FILE index()
        throw new UnsupportedOperationException("Need to implement FILE index");
    }

    /**
     * Write bytes to the FILE system using the specified writeMode
     *
     * @param filename
     * @param bytes
     * @param writeMode
     * @return
     * @throws IOException
     */
//    @NonNull
//    protected byte[] put(
//            @NonNull final String filename,
//            @NonNull final byte[] bytes,
//            final int writeMode) throws IOException {
//        FileOutputStream fos = null;
//
//        final String filenameWithPath = applyPath(filename);
//        try {
//            vv(this, "Start FILE write: " + filenameWithPath);
//            fos = context.openFileOutput(filenameWithPath, writeMode);
//            fos.write(bytes);
//        } catch (IOException e) {
//            ee(this, "Can not put(" + filenameWithPath + ")" + bytes.length, e);
//        } finally {
//            if (fos != null) {
//                fos.close();
//            }
//        }
//        vv(this, "End FILE write: " + filename);
//
//        return bytes;
//    }
}
