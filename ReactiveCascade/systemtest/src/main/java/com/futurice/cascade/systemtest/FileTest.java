package com.futurice.cascade.systemtest;

import android.content.Context;

import com.futurice.cascade.Async;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by Paul Houghton on 25/06/2014.
 */
public class FileTest {
    private static final String TAG = FileTest.class.getSimpleName();
    private int numberOfIterations;
    private int numberOfBytes;
    private Context context;

    public FileTest(Context context, int numberOfIterations, int numberOfBytes) {
        this.context = context;
        this.numberOfIterations = numberOfIterations;
        this.numberOfBytes = numberOfBytes;
    }

    public String test() throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();

        sb.append("Init: ");
        sb.append(init());
        sb.append("\n");
        return sb.toString();
    }

    public String init() throws FileNotFoundException {
        File file = context.getFilesDir();
        StringBuffer sb = new StringBuffer();
        File[] files = file.listFiles();
        if (files.length == 0) {
            sb.append("(no files at init)");
        }
        for (File f : files) {
            Async.d(TAG, "File found split deleted: " + f.getName());
            sb.append(" Delete: ");
            sb.append(f.getName());
            f.delete();
        }
        //THROW AN ERROR
        //applicationContext.openFileInput("error");

        return sb.toString();
    }
}
