/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;
import android.test.mock.MockContext;

import com.reactivecascade.AsyncBuilder;
import com.reactivecascade.CascadeIntegrationTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FileUtilIntegrationTest extends CascadeIntegrationTest {
    private static final String TEST_FILE_NAME = "TESTfileNAME.txt";
    private static final String TEST_CODE = "TESTcode";

    final AsyncMockContext mockContext = new AsyncMockContext();
    private FileUtil mockFileUtil;

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
        mockFileUtil = new FileUtil(mockContext, Context.MODE_PRIVATE);
    }

    @Test
    public void testMockWriteShouldAccessFileSystemOnce() {
        mockFileUtil.write("someFile", "something to write".getBytes());
        assertEquals(1, mockContext.fileOpens);
        assertEquals(1, mockContext.fileWrites);
    }

    @Test
    public void testMockReadFileShouldAccessFileSystemOnce() {
        mockFileUtil.read("someFile");
        assertEquals(1, mockContext.fileOpens);
        assertEquals(3, mockContext.fileBufferReads);
        assertEquals(2, mockContext.fileReads);
    }

    @Test
    public void testMockDeleteOfNonexistantFile() {
        boolean response = mockFileUtil.delete("nonFile");
        assertFalse(response);
        assertEquals(0, mockContext.fileDeletes);
    }

    @Test
    public void testMockDeleteOfFile() {
        boolean response = mockFileUtil.delete("someFile");
        assertTrue(response);
        assertEquals(1, mockContext.fileDeletes);
    }

    @Test
    public void testActualWriteReadDelete() {
        FileUtil fileUtil = new FileUtil(appContext, Context.MODE_PRIVATE);
        fileUtil.write(TEST_FILE_NAME, TEST_CODE.getBytes());
        byte[] bytes = fileUtil.read(TEST_FILE_NAME);
        assertEquals(TEST_CODE, new String(bytes));
        assertTrue(fileUtil.delete(TEST_FILE_NAME));
        assertFalse(fileUtil.delete(TEST_FILE_NAME));
    }

    public class AsyncMockContext extends MockContext {
        public int fileWrites = 0;
        public int fileReads = 0;
        public int fileBufferReads = 0;
        public int fileDeletes = 0;
        public int fileOpens = 0;

        @Override // Context
        public FileOutputStream openFileOutput(@NonNull String name, int mode) throws FileNotFoundException {
            if (!name.equals("someFile")) {
                throw new FileNotFoundException("No such file");
            }
            fileOpens++;

            return new StubOutputStream();
        }

        @Override // Context
        public FileInputStream openFileInput(@NonNull String name) throws FileNotFoundException {
            if (!name.equals("someFile")) {
                throw new FileNotFoundException("No such file");
            }
            fileOpens++;

            return new StubInputStream();
        }

        @Override // Context
        public boolean deleteFile(@NonNull String name) {
            if (!name.equals("someFile")) {
                return false;
            }

            fileDeletes++;
            return true;
        }

        class StubOutputStream extends FileOutputStream {
            public StubOutputStream() throws FileNotFoundException {
                super(FileDescriptor.out);
            }

            // count number of calls, don't bother to really write something
            @Override // OutputStream
            public void write(@NonNull byte[] buffer) throws IOException {
                fileWrites++;
            }
        }

        class StubInputStream extends FileInputStream {
            private final byte[] BUFFER_READ_SEQUENCE = new byte[]{45, 0, 33};
            private int readPosition = 0;

            public StubInputStream() throws FileNotFoundException {
                super(FileDescriptor.in);
            }

            // count number of calls, don't bother to really write something
            @Override // InputStream
            public int read() throws IOException {
                fileReads++;

                return 12;
            }

            // count number of calls, don't bother to really write something
            @Override // InputStream
            public int read(@NonNull byte[] buffer, final int byteOffset, final int byteCount) throws IOException {
                fileReads++;
                int bytesRead = -1;

                for (int i = byteOffset; readPosition < BUFFER_READ_SEQUENCE.length && i < byteOffset + byteCount; i++) {
                    buffer[i] = BUFFER_READ_SEQUENCE[readPosition++];
                    fileBufferReads++;
                    bytesRead = readPosition;
                }

                return bytesRead;
            }
        }
    }
}
