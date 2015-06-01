package com.futurice.cascade.rest;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.MediumTest;

import com.futurice.cascade.AsyncApplicationTestCase;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class FileUtilTest extends AsyncApplicationTestCase<Application> {
    final AsyncMockContext mockContext = new AsyncMockContext();

    class AsyncMockContext extends MockContext {
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
    };

    private FileUtil mockFileUtil;

    public FileUtilTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mockFileUtil = new FileUtil(mockContext, Context.MODE_PRIVATE);
    }

    @MediumTest
    public void testMockWriteShouldAccessFileSystemOnce() {
        mockFileUtil.writeFile("someFile", "something to write".getBytes());
        assertThat(mockContext.fileOpens).isEqualTo(1);
        assertThat(mockContext.fileWrites).isEqualTo(1);
    }

    @MediumTest
    public void testMockReadFileShouldAccessFileSystemOnce() {
        mockFileUtil.readFile("someFile");
        assertThat(mockContext.fileOpens).isEqualTo(1);
        assertThat(mockContext.fileBufferReads).isEqualTo(3);
        assertThat(mockContext.fileReads).isEqualTo(2);
    }

    @MediumTest
    public void testMockDeleteOfNonexistantFile() {
        boolean response = mockFileUtil.deleteFile("nonFile");
        assertThat(response).isFalse();
        assertThat(mockContext.fileDeletes).isEqualTo(0);
    }

    @MediumTest
    public void testMockDeleteOfFile() {
        boolean response = mockFileUtil.deleteFile("someFile");
        assertThat(response).isTrue();
        assertThat(mockContext.fileDeletes).isEqualTo(1);
    }

    private static final String TEST_FILE_NAME = "TESTfileNAME.txt";
    private static final String TEST_CODE = "TESTcode";

    @MediumTest
    public void testActualWriteReadDelete() {
        fileUtil.writeFile(TEST_FILE_NAME, TEST_CODE.getBytes());
        byte[] bytes = fileUtil.readFile(TEST_FILE_NAME);
        assertThat(new String(bytes)).isEqualTo(TEST_CODE);
        boolean deleted = fileUtil.deleteFile(TEST_FILE_NAME);
        assertThat(deleted).isTrue();
        boolean reDeleted = fileUtil.deleteFile(TEST_FILE_NAME);
        assertThat(reDeleted).isFalse();
    }
}
