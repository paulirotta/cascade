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

import android.content.*;

import java.io.*;

import static com.futurice.cascade.Async.*;

public class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();
    private static final int BUFFER_SIZE = 16384;

    public static void writeFile(Context ctx, String fileName, byte[] bytes) {
        if (fileName == null) {
            throwIllegalArgumentException(TAG, " writeFile(applicationContext, filename, bytes) was passed a null filename");
        }

        if (bytes == null) {
            throwIllegalArgumentException(TAG, "writeFile(applicationContext, filename, bytes) was passed a null byte[]");
        }

        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = ctx.openFileOutput(fileName, Context.MODE_PRIVATE);
            fileOutputStream.write(bytes);
        } catch (Exception e) {
            throwRuntimeException(TAG, "Can not write FILE: " + fileName, e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    ee(TAG, "Can not close FILE output stream", e);
                }
            }
        }
    }

    public static byte[] readFile(final Context context, final String fileName) {
        if (fileName == null) {
            throwIllegalArgumentException(TAG, "readFile(applicationContext, filename) was passed a null filename");
        }

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = context.openFileInput(fileName);

            final byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while (true) {
                count = fileInputStream.read(buffer, 0, buffer.length);
                if (count < 0) {
                    break;
                }
                bos.write(buffer, 0, count);
            }
        } catch (Exception e) {
            throwRuntimeException(TAG, "Can not read FILE", e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    ee(TAG, "Can not close FILE input stream: " + fileName, e);
                }
            }

            return bos.toByteArray();
        }
    }

    public static boolean deleteFile(Context ctx, String fileName) {
        if (fileName == null) {
            throwIllegalArgumentException(TAG, " delete(applicationContext, filename) was passed a null filename");
        }

        return ctx.deleteFile(fileName);
    }
}
