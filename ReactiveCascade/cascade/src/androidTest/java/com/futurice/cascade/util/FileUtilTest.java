package com.futurice.cascade.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.LargeTest;

import com.futurice.cascade.AsyncAndroidTestCase;

import org.junit.Before;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class FileUtilTest extends AsyncAndroidTestCase {
    private static final String TEST_FILE_NAME = "TESTfileNAME.txt";
    private static final String TEST_CODE = "TESTcode";

    final AsyncMockContext mockContext = new AsyncMockContext();
    private FileUtil mockFileUtil;

    public FileUtilTest() {
        super();
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        mockFileUtil = new FileUtil(mockContext, Context.MODE_PRIVATE);
    }

    @LargeTest
    public void testMockWriteShouldAccessFileSystemOnce() {
        mockFileUtil.write("someFile", "something to write".getBytes());
        assertEquals(1, mockContext.fileOpens);
        assertEquals(1, mockContext.fileWrites);
    }

    @LargeTest
    public void testMockReadFileShouldAccessFileSystemOnce() {
        mockFileUtil.read("someFile");
        assertEquals(1, mockContext.fileOpens);
        assertEquals(3, mockContext.fileBufferReads);
        assertEquals(2, mockContext.fileReads);
    }

    @LargeTest
    public void testMockDeleteOfNonexistantFile() {
        boolean response = mockFileUtil.delete("nonFile");
        assertFalse(response);
        assertEquals(0, mockContext.fileDeletes);
    }

    @LargeTest
    public void testMockDeleteOfFile() {
        boolean response = mockFileUtil.delete("someFile");
        assertTrue(response);
        assertEquals(1, mockContext.fileDeletes);
    }

    @LargeTest
    public void testActualWriteReadDelete() {
        getFileUtil().write(TEST_FILE_NAME, TEST_CODE.getBytes());
        byte[] bytes = getFileUtil().read(TEST_FILE_NAME);
        assertEquals(TEST_CODE, new String(bytes));
        assertTrue(getFileUtil().delete(TEST_FILE_NAME));
        assertFalse(getFileUtil().delete(TEST_FILE_NAME));
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
