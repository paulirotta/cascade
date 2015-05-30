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
package com.futurice.cascade.rest;

import android.content.*;

import com.futurice.cascade.i.*;
import com.futurice.cascade.util.FileUtil;

import java.io.File;
import java.io.*;
import java.util.*;

import static com.futurice.cascade.Async.*;

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

    /**
     * Create a new <code>FileService</code> with the specified default writeMode which will be used
     * by all {@link RESTService} operations
     *
     * @param dir              a relative or full directory name, not ending with "/", accessible with the writeMode
     * @param deleteOnExit
     * @param context
     * @param writeMode        - AFile FILE system write writeMode from the constants defined in {@link android.content.Context}
     * @param fileReadIThreadType
     * @param fileWriteIThreadType
     */
    public FileMirrorService(String name, String dir, boolean deleteOnExit, Context context, int writeMode, IThreadType fileReadIThreadType, IThreadType fileWriteIThreadType) {
        super(name, fileReadIThreadType, fileWriteIThreadType);

        if (!fileWriteIThreadType.isInOrderExecutor()) {
            throwIllegalArgumentException(this, "Provide a fileWriteIThreadType which supports in order execution. Usually this means a single threaded ServiceExecutor");
        }
        //TODO Confirm file write permissions for the specified directory
        dd(this, "Initializing FileMirrorService");
        this.dir = context.getDir(dir, writeMode);
        this.path = dir;
        this.context = context;
        this.writeMode = writeMode;
        if (deleteOnExit) {
            this.dir.deleteOnExit();
            for (String file : this.dir.list()) {
                dd(this, "Found FILE in deleteOnExit() FileService directory. Likely irregular app termination last execute. Deleting: " + file);
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
    public byte[] get(String key) throws IOException {
        synchronized (mutex) {
            key = applyPath(key);
            vv(this, "Start FILE read: " + key);
            return FileUtil.readFile(context, key);
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
    public void put(String key, byte[] bytes) throws Exception {
        synchronized (mutex) {
            put(key, bytes, writeMode);
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
            vv(this, "Start FILE delete: " + key);
            boolean result = FileUtil.deleteFile(context, key);
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
    protected byte[] put(String filename, byte[] bytes, int writeMode) throws IOException {
        FileOutputStream fos = null;

        if (filename == null) {
            throwIllegalArgumentException(this, "put(filename, bytes) was passed a null filename");
        }

        if (filename == null || bytes == null) {
            throwIllegalArgumentException(this, " put(filename, bytes) was passed a null byte[]");
        }

        filename = applyPath(filename);
        try {
            vv(this, "Start FILE write: " + filename);
            fos = context.openFileOutput(filename, writeMode);
            fos.write(bytes);
        } catch (IOException e) {
            ee(this, "Can not put(" + filename + ")" + bytes.length, e);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        vv(this, "End FILE write: " + filename);

        return bytes;
    }
}
