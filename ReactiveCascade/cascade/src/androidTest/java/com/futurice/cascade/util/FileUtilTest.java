package com.futurice.cascade.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.LargeTest;

import com.futurice.cascade.AsyncAndroidTestCase;
import com.futurice.cascade.i.nonnull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@LargeTest
public class FileUtilTest extends AsyncAndroidTestCase {
    final AsyncMockContext mockContext = new AsyncMockContext();

    public class AsyncMockContext extends MockContext {
        public int fileWrites = 0;
        public int fileReads = 0;
        public int fileBufferReads = 0;
        public int fileDeletes = 0;
        public int fileOpens = 0;

        @Override // Context
        public FileOutputStream openFileOutput(@NonNull @nonnull String name, int mode) throws FileNotFoundException {
            if (!name.equals("someFile")) {
                throw new FileNotFoundException("No such file");
            }
            fileOpens++;

            return new StubOutputStream();
        }

        @Override // Context
        public FileInputStream openFileInput(@NonNull @nonnull String name) throws FileNotFoundException {
            if (!name.equals("someFile")) {
                throw new FileNotFoundException("No such file");
            }
            fileOpens++;

            return new StubInputStream();
        }

        @Override // Context
        public boolean deleteFile(@NonNull @nonnull String name) {
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
            public void write(@NonNull @nonnull byte[] buffer) throws IOException {
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
            public int read(@NonNull @nonnull byte[] buffer, final int byteOffset, final int byteCount) throws IOException {
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

    ;

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

    @Test
    public void testMockWriteShouldAccessFileSystemOnce() {
        mockFileUtil.write("someFile", "something to write".getBytes());
        assertThat(mockContext.fileOpens).isEqualTo(1);
        assertThat(mockContext.fileWrites).isEqualTo(1);
    }

    @Test
    public void testMockReadFileShouldAccessFileSystemOnce() {
        mockFileUtil.read("someFile");
        assertThat(mockContext.fileOpens).isEqualTo(1);
        assertThat(mockContext.fileBufferReads).isEqualTo(3);
        assertThat(mockContext.fileReads).isEqualTo(2);
    }

    @Test
    public void testMockDeleteOfNonexistantFile() {
        boolean response = mockFileUtil.delete("nonFile");
        assertThat(response).isFalse();
        assertThat(mockContext.fileDeletes).isEqualTo(0);
    }

    @Test
    public void testMockDeleteOfFile() {
        boolean response = mockFileUtil.delete("someFile");
        assertThat(response).isTrue();
        assertThat(mockContext.fileDeletes).isEqualTo(1);
    }

    private static final String TEST_FILE_NAME = "TESTfileNAME.txt";
    private static final String TEST_CODE = "TESTcode";

    @Test
    public void testActualWriteReadDelete() {
        getFileUtil().write(TEST_FILE_NAME, TEST_CODE.getBytes());
        byte[] bytes = getFileUtil().read(TEST_FILE_NAME);
        assertThat(new String(bytes)).isEqualTo(TEST_CODE);
        boolean deleted = getFileUtil().delete(TEST_FILE_NAME);
        assertThat(deleted).isTrue();
        boolean reDeleted = getFileUtil().delete(TEST_FILE_NAME);
        assertThat(reDeleted).isFalse();
    }
}
