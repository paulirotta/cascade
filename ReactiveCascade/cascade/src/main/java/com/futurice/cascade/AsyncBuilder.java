/*
The MIT License (MIT)

Copyright (c) 2015 Futurice Oy and individual contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.futurice.cascade;

import android.content.*;
import android.os.*;
import android.support.annotation.NonNull;
import android.util.*;

import com.futurice.cascade.functional.*;
import com.futurice.cascade.i.*;
import com.futurice.cascade.rest.*;
import com.futurice.cascade.util.DefaultThreadType;
import com.futurice.cascade.util.TypedThread;
import com.futurice.cascade.util.UIExecutorService;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * <code><pre>
 *     public static IThreadType threadType;
 * .. onCreate ..
 * if (threadType == null) {
 *    threadType = new ThreadTypeBuilder(this.getApplicationContext()).build();
 * } *
 * </pre></code>
 * <p>
 * The singled threaded class loader ensures all ALog variables are set only once,
 * but injectable for configuration split testability from values previously set here for best performance
 * <p>
 * The first ALog created because the default ALog split is accessible from that point forward by references
 * to {@link com.futurice.cascade.Async#WORKER}
 */
@CallOrigin
public class AsyncBuilder {
    public static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    public static final int NUMBER_OF_CONCURRENT_NET_READS = 4;
    static final String NOT_INITIALIZED = "Please initialize the following in for example Activity.onCreate() before the classloader directly or indirectly invokes ThreadType.class:  new AsyncBuilder(this).build();";
    private static final String TAG = AsyncBuilder.class.getSimpleName();
    private static final AtomicInteger threadNumber = new AtomicInteger();
    private static final AtomicBoolean workerPoolIncludesSerialWorkerThread = new AtomicBoolean(false);
    public static Thread serialWorkerThread;

    public static volatile AsyncBuilder asyncBuilder = null;
    public Thread uiThread;
    public final Context context;
    public ExecutorService uiExecutorService;
    public boolean debug = true; //BuildConfig.DEBUG; // true in debugOrigin builds, false in production builds, determined at build time to help JAVAC and PROGUARD clean out debugOrigin-only support code for speed and size
    public boolean failFast = debug;
    public boolean showErrorStackTraces = debug;
    //TODO Periodically check if recent Android updates have fixed this gradle bug, https://code.google.com/p/android/issues/detail?id=52962
    //TODO Manual gradle work-around, https://gist.github.com/almozavr/d59e770d2a6386061fcb
    //TODO Add a flag for independently enabling or disable runtime assertions and tests at Gradle level. Currently many optimistic assumptions that can make the error show up only later are made when DEBUG==false, but this aggressive optimization might need to be switched off independently to verify if the code path difference is the problem or help when the problem is speed sensitive
    public boolean strictMode = false; //TODO turn back on with "debug" to test this

    private IThreadType workerThreadType;
    private IThreadType serialWorkerThreadType;
    private IThreadType uiThreadType;
    private IThreadType netReadThreadType;
    private IThreadType netWriteThreadType;
    private IThreadType fileThreadType;
    private IThreadType fileWriteThreadType;
    private MirrorService fileService;
    //    private SignalVisualizerClient signalVisualizerClient = null;
    private static BlockingQueue<Runnable> workerQueue;
    private static BlockingQueue<Runnable> serialWorkerQueue;
    private BlockingQueue<Runnable> fileQueue;
    private BlockingQueue<Runnable> netReadQueue;
    private BlockingQueue<Runnable> netWriteQueue;
    private ExecutorService workerExecutorService;
    private ExecutorService serialWorkerExecutorService;
    private ExecutorService fileReadExecutorService;
    private ExecutorService fileWriteExecutorService;
    private ExecutorService netReadExecutorService;
    private ExecutorService netWriteExecutorService;
    private AbstractRESTService netAbstractRESTService;

    public static boolean isInitialized() {
        return asyncBuilder != null;
    }

    /**
     * Create a new <code>AsyncBuilder</code> that will execute as long as the specified
     * {@link android.content.Context} will execute.
     * <p>
     * Unless you have reason to do otherwise, you probably want to pass
     * {@link android.app.Activity#getApplicationContext()} to ensure that the asynchronous
     * actions last past the end of the current {@link android.app.Activity}. If you do so,
     * you can for effeciency only create the one instance for the entire application lifecycle
     * by for example setting a static variable the first time the <code>AsyncBuilder</code>
     * is used.
     *
     * @param context
     */
    public AsyncBuilder(@NonNull final Context context) {
        Context c = context;
        try {
            c = context.getApplicationContext();
        } catch(NullPointerException e) {
            // Needed for instrumentation setup with Android test runner
        }
        this.context = c;
    }

    @NonNull
    public IThreadType getWorkerThreadType() {
        if (workerThreadType == null) {
            ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setWorkerThreadType(new DefaultThreadType("WorkerThreadType",
                            getWorkerExecutorService(threadTypeImmutableValue),
                            getWorkerQueue()
                    )
            );
            threadTypeImmutableValue.set(workerThreadType);
        }

        return workerThreadType;
    }

    @NonNull
    public IThreadType getSerialWorkerThreadType() {
        if (serialWorkerThreadType == null) {
            ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setSerialWorkerThreadType(new DefaultThreadType("SerialWorkerThreadType",
                            getSerialWorkerExecutorService(threadTypeImmutableValue),
                            getSerialWorkerQueue()
                    )
            );
            threadTypeImmutableValue.set(serialWorkerThreadType);
        }

        return serialWorkerThreadType;
    }

    @NonNull
    public AsyncBuilder setWorkerThreadType(@NonNull final IThreadType workerThreadType) {
        Log.v(TAG, "setWorkerThreadType(" + workerThreadType + ")");
        this.workerThreadType = workerThreadType;

        return this;
    }

    @NonNull
    public AsyncBuilder setSerialWorkerThreadType(@NonNull final IThreadType serialWorkerThreadType) {
        Log.v(TAG, "setSerialWorkerThreadType(" + serialWorkerThreadType + ")");
        this.serialWorkerThreadType = serialWorkerThreadType;

        return this;
    }

    @NonNull
    public IThreadType getUiThreadType() {
        if (uiThreadType == null) {
            setUIThreadType(new DefaultThreadType("UIThreadType", getUiExecutorService(), null));
        }

        return uiThreadType;
    }

    @NonNull
    public AsyncBuilder setUIThreadType(@NonNull final IThreadType uiThreadType) {
        Log.v(TAG, "setUIThreadType(" + uiThreadType + ")");
        this.uiThreadType = uiThreadType;
        return this;
    }

    @NonNull
    public IThreadType getNetReadThreadType() {
        if (netReadThreadType == null) {
            final ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setNetReadThreadType(new DefaultThreadType("NetReadThreadType",
                            getNetReadExecutorService(threadTypeImmutableValue),
                            getNetReadQueue()
                    )
            );
            threadTypeImmutableValue.set(netReadThreadType);
        }

        return netReadThreadType;
    }

    @NonNull
    public AsyncBuilder setNetReadThreadType(@NonNull final IThreadType netReadThreadType) {
        Log.v(TAG, "setNetReadThreadType(" + netReadThreadType + ")");
        this.netReadThreadType = netReadThreadType;
        return this;
    }

    @NonNull
    public IThreadType getNetWriteThreadType() {
        if (netWriteThreadType == null) {
            ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setNetWriteThreadType(new DefaultThreadType("NetWriteThreadType",
                            getNetWriteExecutorService(threadTypeImmutableValue),
                            getNetWriteQueue()
                    )
            );
            threadTypeImmutableValue.set(netWriteThreadType);
        }

        return netWriteThreadType;
    }

    @NonNull
    public AsyncBuilder setNetWriteThreadType(@NonNull final IThreadType netWriteThreadType) {
        Log.v(TAG, "setNetWriteThreadType(" + netWriteThreadType + ")");
        this.netWriteThreadType = netWriteThreadType;
        return this;
    }

    @NonNull
    public IThreadType getFileThreadType() {
        if (fileThreadType == null) {
            final ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setFileThreadType(new DefaultThreadType("FileReadThreadType",
                            getFileExecutorService(threadTypeImmutableValue),
                            getFileQueue()
                    )
            );
            threadTypeImmutableValue.set(fileThreadType);
        }

        return fileThreadType;
    }

    @NonNull
    public AsyncBuilder setFileThreadType(@NonNull final IThreadType fileThreadType) {
        Log.v(TAG, "setFileThreadType(" + fileThreadType + ")");
        this.fileThreadType = fileThreadType;
        return this;
    }

    @NonNull
    public AsyncBuilder setFileWriteThreadType(@NonNull final IThreadType fileWriteThreadType) {
        Log.v(TAG, "setFileWriteThreadType(" + fileWriteThreadType + ")");
        this.fileWriteThreadType = fileWriteThreadType;
        return this;
    }

    @NonNull
    public MirrorService getFileService() {
        if (fileService == null) {
            Log.v(TAG, "Creating default file service");
            setFileService(new FileMirrorService("Default FileMirrorService",
                    "FileMirrorService",
                    false,
                    context,
                    Context.MODE_PRIVATE,
                    getFileThreadType()));
        }

        return fileService;
    }

    @NonNull
    public AsyncBuilder setFileService(@NonNull final MirrorService fileService) {
        Log.v(TAG, "setFileService(" + fileService + ")");
        this.fileService = fileService;
        return this;
    }

    @NonNull
    private static Thread getWorkerThread(IThreadType threadType, Runnable runnable) {
        if (NUMBER_OF_CORES == 1 || workerPoolIncludesSerialWorkerThread.getAndSet(true)) {
            return new TypedThread(threadType, runnable, "WorkerThread" + threadNumber.getAndIncrement());
        }
        return getSerialWorkerThread(threadType, runnable);
    }

    @NonNull
    public ExecutorService getWorkerExecutorService(@NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        if (workerExecutorService == null) {
            Log.v(TAG, "Creating default worker executor service");
            final BlockingQueue<Runnable> q = getWorkerQueue();
            final int numberOfThreads = q instanceof BlockingDeque ? NUMBER_OF_CORES : 1;

            setWorkerExecutorService(new ThreadPoolExecutor(
                    numberOfThreads,
                    numberOfThreads,
                    1000,
                    TimeUnit.MILLISECONDS,
                    q,
                    runnable ->
                            getWorkerThread(threadTypeImmutableValue.get(), runnable)
            ));
        }

        return workerExecutorService;
    }

    private static synchronized Thread getSerialWorkerThread(@NonNull final IThreadType threadType, @NonNull final Runnable runnable) {
        if (serialWorkerThread == null) {
            serialWorkerThread = new TypedThread(threadType, runnable, "SerialWorkerThread" + threadNumber.getAndIncrement());
        }

        return serialWorkerThread;
    }

    @NonNull
    public ExecutorService getSerialWorkerExecutorService(@NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        if (serialWorkerExecutorService == null) {
            Log.v(TAG, "Creating default serial worker executor service");

            setSerialWorkerExecutorService(new ThreadPoolExecutor(
                            1,
                            1,
                            1000,
                            TimeUnit.MILLISECONDS,
                            getSerialWorkerQueue(),
                            runnable -> getSerialWorkerThread(threadTypeImmutableValue.get(), runnable))
            );
        }

        return workerExecutorService;
    }

    public static void setWorkerQueue(@NonNull final BlockingQueue<Runnable> queue) {
        Log.d(TAG, "setWorkerQueue(" + queue + ")");
        workerQueue = queue;
    }

    public static void setSerialWorkerQueue(@NonNull final BlockingQueue<Runnable> queue) {
        Log.d(TAG, "setSerialWorkerQueue(" + queue + ")");
        serialWorkerQueue = queue;
    }

    public static BlockingQueue<Runnable> getWorkerQueue() {
        if (workerQueue == null) {
            Log.d(TAG, "Creating default worker queue");
            setWorkerQueue(new LinkedBlockingDeque<>());
        }

        return workerQueue;
    }

    /**
     * Call {@link #setWorkerQueue(java.util.concurrent.BlockingQueue)} before calling this method
     * if you wish to use something other than the default.
     *
     * @return
     */
    @NonNull
    public static BlockingQueue<Runnable> getSerialWorkerQueue() {
        if (serialWorkerQueue == null) {
            Log.d(TAG, "Creating default in-order worker queue");
            setSerialWorkerQueue(new DoubleQueue<>(getWorkerQueue()));
        }

        return serialWorkerQueue;
    }

    public void setFileQueue(@NonNull final BlockingQueue<Runnable> queue) {
        Log.d(TAG, "setFileQueue(" + queue + ")");
        this.fileQueue = queue;
    }

    @NonNull
    public BlockingQueue<Runnable> getFileQueue() {
        if (fileQueue == null) {
            Log.d(TAG, "Creating default file read queue");
            setFileQueue(new LinkedBlockingDeque<>());
        }

        return fileQueue;
    }

    public void setNetReadQueue(@NonNull final BlockingQueue<Runnable> queue) {
        Log.d(TAG, "setNetReadQueue(" + queue + ")");
        this.netReadQueue = queue;
    }

    public void setNetWriteQueue(@NonNull final BlockingQueue<Runnable> queue) {
        Log.d(TAG, "setNetWriteQueue(" + queue + ")");
        this.netWriteQueue = queue;
    }

    @NonNull
    public BlockingQueue<Runnable> getNetReadQueue() {
        if (netReadQueue == null) {
            Log.d(TAG, "Creating default net read queue");
            setNetReadQueue(new LinkedBlockingDeque<>());
        }

        return netReadQueue;
    }

    @NonNull
    public BlockingQueue<Runnable> getNetWriteQueue() {
        if (netWriteQueue == null) {
            Log.d(TAG, "Creating default worker net write queue");
            setNetWriteQueue(new LinkedBlockingDeque<>());
        }

        return netWriteQueue;
    }

    @NonNull
    public ExecutorService getFileExecutorService(
            @NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        if (fileReadExecutorService == null) {
            Log.d(TAG, "Creating default file read executor service");
            setFileReadExecutorService(new ThreadPoolExecutor(1, 1,
                            0L, TimeUnit.MILLISECONDS,
                            getFileQueue(),
                            runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, "FileThread" + threadNumber.getAndIncrement()))
            );
        }
        //TODO else Warn if threadType does match the previously created IThreadType parameter

        return fileReadExecutorService;
    }

    @NonNull
    public ExecutorService getNetReadExecutorService(
            @NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        if (netReadExecutorService == null) {
            Log.d(TAG, "Creating default net read executor service");
            setNetReadExecutorService(new ThreadPoolExecutor(1, NUMBER_OF_CONCURRENT_NET_READS,
                            1000, TimeUnit.MILLISECONDS, getNetReadQueue(),
                            runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, "NetReadThread" + threadNumber.getAndIncrement()))
            );
        }

        return netReadExecutorService;
    }

    @NonNull
    public ExecutorService getNetWriteExecutorService(
            @NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        if (netWriteExecutorService == null) {
            Log.d(TAG, "Creating default net write executor service");
            setNetWriteExecutorService(Executors.newSingleThreadExecutor(
                            runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, "NetWriteThread" + threadNumber.getAndIncrement()))
            );
        }

        return netWriteExecutorService;
    }

    @NonNull
    public ExecutorService getUiExecutorService() {
        if (context == null) {
            Exception e = new IllegalStateException(NOT_INITIALIZED);
            Log.e(TAG, NOT_INITIALIZED, e);
            System.exit(-1);
        }
        if (uiExecutorService == null) {
            setUiExecutorService(new UIExecutorService(new Handler(context.getMainLooper())));
        }

        return uiExecutorService;
    }

    /**
     * Note that if you override this, you may also want to override the associated default
     * {@link #setUI_Thread(Thread)}
     *
     * @param uiExecutorService
     * @return the Builder
     */
    @NonNull
    public AsyncBuilder setUiExecutorService(@NonNull final ExecutorService uiExecutorService) {
        Log.d(TAG, "setUiExecutorService(" + uiExecutorService + ")");
        this.uiExecutorService = uiExecutorService;
        return this;
    }

    @NonNull
    public AbstractRESTService getNetAbstractRESTService() {
        if (netAbstractRESTService == null) {
            setNetAbstractRESTService(new NetRESTService("Default NetRESTService", context,
                    getNetReadThreadType(), getNetWriteThreadType()));
        }

        return netAbstractRESTService;
    }

    @NonNull
    public AsyncBuilder setNetAbstractRESTService(@NonNull final NetRESTService netAbstractRESTService) {
        Log.d(TAG, "setNetRESTService(" + netAbstractRESTService + ")");
        this.netAbstractRESTService = netAbstractRESTService;
        return this;
    }

    /**
     * Note that if you override this, you may also want to override the associated
     * {@link #setWorkerThreadType(com.futurice.cascade.i.IThreadType)} to for example match the <code>inOrderExecution</code> parameter
     *
     * @param executorService the service to be used for most callbacks split general purpose processing. The default implementation
     *                        is a threadpool sized based to match the number of CPU cores. Most operations on this executor
     *                        do not block for IO, those are relegated to specialized executors
     * @return the Builder
     */
    public void setWorkerExecutorService(@NonNull final ExecutorService executorService) {
        Log.v(TAG, "setWorkerExecutorService(" + executorService + ")");
        workerExecutorService = executorService;
    }

    public void setSerialWorkerExecutorService(@NonNull final ExecutorService executorService) {
        Log.v(TAG, "setSerialWorkerExecutorService(" + executorService + ")");
        serialWorkerExecutorService = executorService;
    }

    @NonNull
    public AsyncBuilder singleThreadedWorkerExecutorService() {
        Log.v(TAG, "singleThreadedWorkerExecutorService()");
        final ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
        this.workerExecutorService = Executors.newSingleThreadScheduledExecutor(
                runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, "SingleThreadedWorker" + threadNumber.getAndIncrement())
        );
        threadTypeImmutableValue.set(getWorkerThreadType());
        return this;
    }

    @NonNull
    public AsyncBuilder setFileReadExecutorService(@NonNull final ExecutorService fileReadExecutorService) {
        Log.v(TAG, "setFileReadExecutorService(" + fileReadExecutorService + ")");
        this.fileReadExecutorService = fileReadExecutorService;
        return this;
    }

    public void setFileWriteExecutorService(@NonNull final ExecutorService executorService) {
        Log.v(TAG, "setFileWriteExecutorService(" + fileWriteExecutorService + ")");
        fileWriteExecutorService = executorService;
    }

    @NonNull
    public AsyncBuilder setNetReadExecutorService(@NonNull final ExecutorService netReadExecutorService) {
        Log.v(TAG, "setNetReadExecutorService(" + netReadExecutorService + ")");
        this.netReadExecutorService = netReadExecutorService;
        return this;
    }

    @NonNull
    public AsyncBuilder setNetWriteExecutorService(@NonNull final ExecutorService netWriteExecutorService) {
        Log.v(TAG, "setNetWriteExecutorService(" + netWriteExecutorService + ")");
        this.netWriteExecutorService = netWriteExecutorService;
        return this;
    }

    /**
     * Change the thread marked as the thread from which user interface calls are made
     * <p>
     * This is just a marker useful for application logic. It does not modify the UI implementation
     * in any way.
     *
     * @param uiThread
     * @return
     */
    @NonNull
    public AsyncBuilder setUI_Thread(@NonNull final Thread uiThread) {
        Log.v(TAG, "setUI_Thread(" + uiThread + ")");
        uiThread.setName("UIThread");
        this.uiThread = uiThread;

        return this;
    }

    /**
     * Set this to false if you prefer the app to log exceptions during debugOrigin but continue running
     * <p>
     * This setting only affects debugOrigin builds. Production builds do not close on first Exception
     * <p>
     * Defaults is true
     * <p>
     *
     * @param failFast
     * @return
     */
    @NonNull
    public AsyncBuilder setFailFast(final boolean failFast) {
        Log.v(TAG, "setFailFast(" + failFast + ")");
        this.failFast = failFast;
        return this;
    }

    /**
     * By default, error stack traces are shown in the debug output. When running system testing code which
     * intentionally throws errors, this may be better disabled.
     *
     * @param showErrorStackTraces
     * @return
     */
    @NonNull
    public AsyncBuilder setShowErrorStackTraces(final boolean showErrorStackTraces) {
        Log.v(TAG, "setShowErrorStackTraces(" + showErrorStackTraces + ")");
        this.showErrorStackTraces = showErrorStackTraces;
        return this;
    }

//    public AsyncBuilder setSignalVisualizerUri(URI uri, List<BasicNameValuePair> extraHeaders) {
//        signalVisualizerClient = new SignalVisualizerClient.Builder()
//                .setUri(uri)
//                .setExtraHeaders(extraHeaders)
//                .build();
//        if (signalVisualizerClient == null) {
//            Log.v(TAG, "signalVisualizerClient set");
//        } else {
//            Log.v(TAG, "No signalVisualizerClient");
//        }
//
//        return this;
//    }
//
//    public SignalVisualizerClient getSignalVisualizerClient() {
//        return signalVisualizerClient;
//    }

    @NonNull
    public Async build() {
        if (uiThread == null) {
            Thread thread = Thread.currentThread();
            try {
                context.getMainLooper().getThread();
            } catch (NullPointerException e) {
                // Needed for Google instrumentation test runner
            }
            setUI_Thread(thread);
        }
        if (strictMode) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        Log.v(TAG, "AsyncBuilder complete");

        asyncBuilder = this;
//        return new Async();
        return null;
    }
}
