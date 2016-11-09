/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.CallSuper;
import android.support.annotation.RequiresPermission;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.ActivityCompat;

import com.reactivecascade.DefaultCascadeIntegrationTest;
import com.reactivecascade.test.TestActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FileUtilIntegrationTest extends DefaultCascadeIntegrationTest {
    static final String TAG = FileUtilIntegrationTest.class.getSimpleName();
    private static final String TEST_FILE_NAME = "TESTfileNAME.txt";
    private static final AtomicInteger testCounter = new AtomicInteger();

    private static String getUniqueTestData() {
        return "test data " + testCounter.incrementAndGet();
    }

    private FileUtil fileUtil;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        if (fileUtil == null) {
            fileUtil = new FileUtil(getContext(), Context.MODE_PRIVATE);
        }

        getContext().startActivity(new Intent(getContext(), TestActivity.class));
    }

    @CallSuper
    @After
    public void cleanupTestFile() throws Exception {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activityTestRule.getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    TestActivity.MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }
        fileUtil.delete(TEST_FILE_NAME);
    }

    @Test
    public void testWriteThenRead() throws Exception {
        String s = getUniqueTestData();

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activityTestRule.getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    TestActivity.MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }
        fileUtil.write(TEST_FILE_NAME, s.getBytes());
        String s2 = new String(fileUtil.read(TEST_FILE_NAME));
        assertEquals(s, s2);
    }

    @Test
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void testDeleteOfNonexistantFile() throws Exception {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activityTestRule.getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    TestActivity.MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }
        boolean response = fileUtil.delete("nonFile");
        assertFalse(response);
    }

    @Test
    public void testDeleteOfFile() throws Exception {
        String s = getUniqueTestData();

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activityTestRule.getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    TestActivity.MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }
        fileUtil.write(TEST_FILE_NAME, getUniqueTestData().getBytes());
        boolean response = fileUtil.delete(TEST_FILE_NAME);
        assertTrue(response);
    }
}
