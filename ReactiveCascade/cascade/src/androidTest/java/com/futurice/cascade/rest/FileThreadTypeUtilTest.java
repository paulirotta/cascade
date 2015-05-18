package com.futurice.cascade.rest;

import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class FileThreadTypeUtilTest extends ApplicationTestCase<Application> {
    public int fileWrites = 0;
    public int fileReads = 0;
    public int fileBufferReads = 0;
    public int fileDeletes = 0;

    MockContext mockContext = new MockContext() {
        @Override
        public FileOutputStream openFileOutput(String name, int mode)
                throws FileNotFoundException {
            return new StubOutputStream();
        }

        @Override
        public FileInputStream openFileInput(String name)
                throws FileNotFoundException {
            return new StubInputStream();
        }

        @Override
        public boolean deleteFile(String name) {
            fileDeletes++;

            return false;
        }

        class StubOutputStream extends FileOutputStream {
            public StubOutputStream() throws FileNotFoundException {
                super(FileDescriptor.out);
            }

            // count number of calls, don't bother to really write something
            @Override
            public void write(byte[] buffer) throws IOException {
                fileWrites++;
            }
        }

        class StubInputStream extends FileInputStream {
            private final int[] BUFFER_READ_SEQUENCE = new int[]{45, 0, -1};

            public StubInputStream() throws FileNotFoundException {
                super(FileDescriptor.in);
            }

            // count number of calls, don't bother to really write something
            @Override
            public int read() throws IOException {
                fileReads++;

                return 12;
            }

            // count number of calls, don't bother to really write something
            @Override
            public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
                return BUFFER_READ_SEQUENCE[fileBufferReads++];
            }
        }
    };

    public FileThreadTypeUtilTest() {
        super(Application.class);
    }

    @SmallTest
    public void testWriteShouldAccessFileSystemOnce() {
        assertEquals("No FILE writes yet", fileWrites, 0);
        FileUtil.writeFile(mockContext, "filename", "something to write".getBytes());
        assertEquals("Failed to write touch FILE write", 1, fileWrites);
    }

    @SmallTest
    public void testReadConfigurationShouldAccessFileSystemOnce() {
        assertEquals("No FILE reads yet", 0, fileReads);
        assertEquals("No FILE buffer reads yet", 0, fileBufferReads);
        FileUtil.readFile(mockContext, "filename");
        assertEquals("Failed to read buffer 3 times", 3, fileBufferReads);
        assertEquals("Failed to read without touching single read", 0, fileReads);
    }

    @SmallTest
    public void testNullFilenameRead() {
        try {
            FileUtil.readFile(mockContext, null);
            fail("No null check on filename for FILE read");
        } catch (IllegalArgumentException e) {
        }
    }

    @SmallTest
    public void testNullFilenameWrite() {
        try {
            FileUtil.writeFile(mockContext, null, "blah".getBytes());
            fail("No null check on filename for FILE write");
        } catch (IllegalArgumentException e) {
        }
    }

    @SmallTest
    public void testNullBytesWrite() {
        try {
            FileUtil.writeFile(mockContext, "filename", null);
            fail("No null check on bytes for FILE write");
        } catch (IllegalArgumentException e) {
        }
    }

    @SmallTest
    public void testNullFilenameDelete() {
        try {
            FileUtil.deleteFile(mockContext, null);
            fail("No null check on filename for FILE delete");
        } catch (IllegalArgumentException e) {
        }
    }

    @SmallTest
    public void testSomethingDelete() {
        assertEquals("Zero FILE deletes", 0, fileDeletes);
        boolean response = FileUtil.deleteFile(mockContext, "something");
        assertFalse("File delete should return false", response);
        assertEquals("One FILE delete", 1, fileDeletes);
    }

    private static final String TEST_FILE_NAME = "TESTfileNAME.txt";
    private static final String TEST_CODE = "TESTcode";

    @MediumTest
    public void testActualWriteReadDelete() {
        Context realContext = getContext();
        FileUtil.writeFile(realContext, TEST_FILE_NAME, TEST_CODE.getBytes());
        byte[] bytes = FileUtil.readFile(realContext, TEST_FILE_NAME);
        String s = new String(bytes);
        assertEquals("File written was not read", TEST_CODE, s);
        boolean deleted = FileUtil.deleteFile(realContext, TEST_FILE_NAME);
        assertTrue("File not deleted", deleted);
        boolean reDeleted = FileUtil.deleteFile(realContext, TEST_FILE_NAME);
        assertFalse("File should have been already deleted", reDeleted);
    }
}
