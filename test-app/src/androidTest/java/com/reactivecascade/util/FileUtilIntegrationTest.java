/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.ActivityCompat;
import android.test.mock.MockContext;
import android.util.Log;

import com.reactivecascade.AsyncBuilder;
import com.reactivecascade.CascadeIntegrationTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FileUtilIntegrationTest extends CascadeIntegrationTest {
    static final String TAG = FileUtilIntegrationTest.class.getSimpleName();
    static final String TEST_FILE_NAME = "TESTfileNAME.txt";
    static final AtomicInteger testCounter = new AtomicInteger();

    static String getUniqueTestData() {
        return "test data " + testCounter.incrementAndGet();
    }

    FileUtil fileUtil;

    public FileUtilIntegrationTest() {
        super();
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        new AsyncBuilder(appContext)
                .setStrictMode(false)
                .build();
        fileUtil = new FileUtil(appContext, Context.MODE_PRIVATE);
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "missing permission");
        }
    }

    @CallSuper
    @After
    public void cleanup() throws Exception {
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            fileUtil.delete(TEST_FILE_NAME);
        }
        super.cleanup();
    }

    @Test
    public void testWriteThenRead() throws Exception {
        String s = getUniqueTestData();

        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "missing permission");
        }
        fileUtil.write(TEST_FILE_NAME, s.getBytes());
        String s2 = new String(fileUtil.read(TEST_FILE_NAME));
        assertEquals(s, s2);
    }

    @Test
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void testDeleteOfNonexistantFile() throws Exception {
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "missing permission");
        }
        boolean response = fileUtil.delete("nonFile");
        assertFalse(response);
    }

    @Test
    public void testDeleteOfFile() throws Exception {
        String s = getUniqueTestData();

        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "missing permission");
        }
        fileUtil.write(TEST_FILE_NAME, getUniqueTestData().getBytes());
        boolean response = fileUtil.delete(TEST_FILE_NAME);
        assertTrue(response);
    }
}
