/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.reactivecascade.functional.ImmutableValue;
import com.reactivecascade.i.CallOrigin;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IAsyncOrigin;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.DefaultThreadType;
import com.reactivecascade.util.DoubleQueue;
import com.reactivecascade.util.Origin;
import com.reactivecascade.util.TypedThread;
import com.reactivecascade.util.UIExecutorService;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
 * to {@link com.reactivecascade.Async#WORKER}
 */
@CallOrigin
public class AsyncBuilder {
    private static final String TAG = AsyncBuilder.class.getSimpleName();
    private static final long RESET_TIMEOUT = 5000; // For closing thread pools in preperation for the next integration test

    @VisibleForTesting
    static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    @VisibleForTesting
    static final int NUMBER_OF_CONCURRENT_NET_READS = 4;

    @VisibleForTesting
    private static final AtomicInteger threadUid = new AtomicInteger(); // All threads created on all AsyncBuilders are assigned unique, consecutive numbers

    @VisibleForTesting
    static boolean initialized = false;

    @VisibleForTesting
    static Thread serialWorkerThread;

    @NonNull
    @VisibleForTesting
    private final Context context;

    @VisibleForTesting
    static final AtomicBoolean workerPoolIncludesSerialWorkerThread = new AtomicBoolean(false);

    @VisibleForTesting
    static Thread uiThread;

    @VisibleForTesting
    static ExecutorService uiExecutorService = null;

    @VisibleForTesting
    static boolean useForkedState;

    @VisibleForTesting
    static boolean runtimeAssertionsEnabled;

    @VisibleForTesting
    static boolean strictMode;

    @VisibleForTesting
    static boolean showErrorStackTraces;

    @VisibleForTesting
    static boolean failFast;

    @VisibleForTesting
    static boolean traceAsyncOrigin;

    @VisibleForTesting
    static IThreadType workerThreadType;

    @VisibleForTesting
    static IThreadType serialWorkerThreadType;

    @VisibleForTesting
    static IThreadType uiThreadType;

    @VisibleForTesting
    static IThreadType netReadThreadType;

    @VisibleForTesting
    static IThreadType netWriteThreadType;

    @VisibleForTesting
    static IThreadType fileThreadType;

    @VisibleForTesting
    BlockingQueue<Runnable> workerQueue;

    @VisibleForTesting
    BlockingQueue<Runnable> serialWorkerQueue;

    @VisibleForTesting
    BlockingQueue<Runnable> fileQueue;

    @VisibleForTesting
    BlockingQueue<Runnable> netReadQueue;

    @VisibleForTesting
    BlockingQueue<Runnable> netWriteQueue;

    @VisibleForTesting
    ExecutorService workerExecutorService;

    @VisibleForTesting
    ExecutorService serialWorkerExecutorService;

    @VisibleForTesting
    ExecutorService fileExecutorService;

    @VisibleForTesting
    ExecutorService netReadExecutorService;

    @VisibleForTesting
    ExecutorService netWriteExecutorService;

    static {
        reset();
    }

    /**
     * Reset state to default
     */
    @UiThread
    @VisibleForTesting
    static void reset() {
        initialized = false;
        if (uiExecutorService != null) {
            uiExecutorService.shutdownNow();
            uiExecutorService = null;
        }
        useForkedState = BuildConfig.DEBUG;
        runtimeAssertionsEnabled = BuildConfig.DEBUG;
        strictMode = BuildConfig.DEBUG;
        showErrorStackTraces = BuildConfig.DEBUG;
        failFast = BuildConfig.DEBUG;
        traceAsyncOrigin = BuildConfig.DEBUG;

        uiThreadType = null;
        resetThreadType(workerThreadType);
        workerThreadType = null;
        resetThreadType(serialWorkerThreadType);
        serialWorkerThreadType = null;
        resetThreadType(netReadThreadType);
        netReadThreadType = null;
        resetThreadType(netWriteThreadType);
        netWriteThreadType = null;
        resetThreadType(fileThreadType);
        fileThreadType = null;
    }

    private static void resetThreadType(@Nullable IThreadType threadType) {
        if (threadType != null) {
            threadType.shutdownNow("Reset for next integration test", null, () -> Log.i(TAG, "TIMEOUT when shutting " + threadType + " in preperation of next integration test. Please ensure your previous integration test cleans up after itself within " + RESET_TIMEOUT + "ms"), RESET_TIMEOUT);
        }
    }

    /**
     * Create a new <code>AsyncBuilder</code> that will run as long as the specified
     * {@link android.content.Context} will run.
     * <p>
     * Unless you have reason to do otherwise, you probably want to pass
     * {@link android.app.Activity#getApplicationContext()} to ensure that the asynchronous
     * actions last past the end of the current {@link android.app.Activity}. If you do so,
     * you can for effeciency only create the one instance for the entire application lifecycle
     * by for example setting a static variable the first time the <code>AsyncBuilder</code>
     * is used.
     *
     * @param context application context
     */
    @UiThread
    public AsyncBuilder(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Please pass a non-null Context to the AsyncBuilder constructor");
        }
        Context c = context.getApplicationContext();

        if (c != null) {
            this.context = c;
        } else {
            Log.i(TAG, "Instrumentation test run detected");
            this.context = context;
        }
    }

    /**
     * Set whether the library should treat this as a runtimeAssertionsEnabled build with regard to code flow of
     * runtimeAssertionsEnabled assist features
     * <p>
     * The default from is {@link BuildConfig#DEBUG}
     *
     * @param enabled mode
     */
    @UiThread
    public AsyncBuilder setRuntimeAssertionsEnabled(boolean enabled) {
        Log.v(TAG, "setRuntimeAssertionsEnabled(" + enabled + ")");
        AsyncBuilder.runtimeAssertionsEnabled = enabled;

        return this;
    }

    /**
     * Set whether the library should verify at time of {@link IAltFuture#fork()} that it has not
     * already been forked. This is useful for debugging, but requires in an additional state transition
     * so may impact performance. Force this to <code>true</code> to make your production builds perform
     * an additional test usually considered relevant only to testing.
     * <p>
     * The default from is {@link BuildConfig#DEBUG}
     *
     * @param enabled mode
     */
    @UiThread
    public AsyncBuilder setUseForkedState(boolean enabled) {
        Log.v(TAG, "setUseForkedState(" + enabled + ")");
        AsyncBuilder.useForkedState = enabled;

        return this;
    }

    /**
     * Set whether strict mode is enabled
     * <p>
     * The default from is {@link BuildConfig#DEBUG}
     *
     * @param enabled mode
     */
    @UiThread
    public AsyncBuilder setStrictMode(boolean enabled) {
        Log.v(TAG, "setStrictMode(" + enabled + ")");
        AsyncBuilder.strictMode = enabled;

        return this;
    }

    @UiThread
    public AsyncBuilder setShowErrorStackTraces(boolean enabled) {
        Log.v(TAG, "setShowErrorStackTraces(" + enabled + ")");
        AsyncBuilder.showErrorStackTraces = enabled;

        return this;
    }

    /**
     * Set this to false if you prefer the app to log exceptions during debugOrigin but continue running
     * <p>
     * The default from is {@link BuildConfig#DEBUG}
     *
     * @param failFast <code>true</code> to stop on first error for clear debugging
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setFailFast(boolean failFast) {
        Log.v(TAG, "setFailFast(" + failFast + ")");
        AsyncBuilder.failFast = failFast;
        return this;
    }

    /**
     * By default, error stack traces are shown in the debug output. When running system testing code which
     * intentionally throws errors, this may be better disabled.
     *
     * @param traceAsyncOrigin <code>true</code> to show stack traces
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setTraceAsyncOrigin(boolean traceAsyncOrigin) {
        Log.v(TAG, "setTraceAsyncOrigin(" + traceAsyncOrigin + ")");
        AsyncBuilder.traceAsyncOrigin = traceAsyncOrigin;

        return this;
    }

    /**
     * Get the group of threads which execute CPU-bound tasks
     *
     * @return the threadType
     */
    @NonNull
    @NotCallOrigin
    @VisibleForTesting
    @UiThread
    IThreadType getWorkerThreadType() {
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

    /**
     * @param workerThreadType thread type for CPU-bound tasks
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setWorkerThreadType(@NonNull IThreadType workerThreadType) {
        Log.v(TAG, "setWorkerThreadType(" + workerThreadType + ")");
        AsyncBuilder.workerThreadType = workerThreadType;

        return this;
    }

    /**
     * @return the single-threaded thread type for CPU-bound tasks
     */
    @NonNull
    @NotCallOrigin
    @VisibleForTesting
    @UiThread
    IThreadType getSerialWorkerThreadType() {
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

    /**
     * @param serialWorkerThreadType the single-threaded thread type for CPU-bound tasks
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setSerialWorkerThreadType(@NonNull final IThreadType serialWorkerThreadType) {
        Log.v(TAG, "setSerialWorkerThreadType(" + serialWorkerThreadType + ")");
        this.serialWorkerThreadType = serialWorkerThreadType;

        return this;
    }

    /**
     * @return a thread type wrapper for the system's UI thread
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    static IThreadType getUiThreadType(@Nullable Context context) {
        if (AsyncBuilder.uiThreadType == null) {
            AsyncBuilder.uiThreadType = new DefaultThreadType("UIThreadType", getUiExecutorService(context), null);
        }

        return AsyncBuilder.uiThreadType;
    }

    /**
     * @param uiThreadType thread type for UI activities
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setUIThreadType(@NonNull IThreadType uiThreadType) {
        Log.v(TAG, "setUIThreadType(" + uiThreadType + ")");
        AsyncBuilder.uiThreadType = uiThreadType;
        return this;
    }

    /**
     * @return thread type for UI activities
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    IThreadType getNetReadThreadType() {
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

    /**
     * @param netReadThreadType thread type for reading (non-mutating state) from network servers
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setNetReadThreadType(@NonNull IThreadType netReadThreadType) {
        Log.v(TAG, "setNetReadThreadType(" + netReadThreadType + ")");
        AsyncBuilder.netReadThreadType = netReadThreadType;
        return this;
    }

    /**
     * @return thread type for writing (mutating state) to network servers
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    IThreadType getNetWriteThreadType() {
        if (netWriteThreadType == null) {
            final ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setNetWriteThreadType(new DefaultThreadType("NetWriteThreadType",
                            getNetWriteExecutorService(threadTypeImmutableValue),
                            getNetWriteQueue()
                    )
            );
            threadTypeImmutableValue.set(netWriteThreadType);
        }

        return netWriteThreadType;
    }

    /**
     * @param netWriteThreadType
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setNetWriteThreadType(@NonNull IThreadType netWriteThreadType) {
        Log.v(TAG, "setNetWriteThreadType(" + netWriteThreadType + ")");
        AsyncBuilder.netWriteThreadType = netWriteThreadType;
        return this;
    }

    /**
     * @return
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    IThreadType getFileThreadType() {
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

    /**
     * @param fileThreadType
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setFileThreadType(@NonNull IThreadType fileThreadType) {
        Log.v(TAG, "setFileThreadType(" + fileThreadType + ")");
        AsyncBuilder.fileThreadType = fileThreadType;
        return this;
    }

    @NonNull
    @UiThread
    private Thread getWorkerThread(@NonNull final IThreadType threadType,
                                   @NonNull final Runnable runnable) {
        if (NUMBER_OF_CORES == 1 || workerPoolIncludesSerialWorkerThread.getAndSet(true)) {
            return new TypedThread(threadType, runnable, createThreadId("WorkerThread"));
        }

        return getSerialWorkerThread(threadType, runnable);
    }

    @NonNull
    @VisibleForTesting
    @UiThread
    ExecutorService getWorkerExecutorService(@NonNull ImmutableValue<IThreadType> threadTypeImmutableValue) {
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
                    runnable -> getWorkerThread(threadTypeImmutableValue.get(), runnable)
            ));
        }

        return workerExecutorService;
    }

    @NonNull
    private static String createThreadId(@NonNull String threadCategory) {
        return threadCategory + threadUid.getAndIncrement();
    }

    @NonNull
    @UiThread
    private Thread getSerialWorkerThread(@NonNull IThreadType threadType,
                                         @NonNull Runnable runnable) {
        if (serialWorkerThread == null) {
            serialWorkerThread = new TypedThread(threadType, runnable, createThreadId("SerialWorkerThread"));
        }

        return serialWorkerThread;
    }

    @NonNull
    @VisibleForTesting
    @UiThread
    protected ExecutorService getSerialWorkerExecutorService(@NonNull ImmutableValue<IThreadType> threadTypeImmutableValue) {
        Log.v(TAG, "getSerialWorkerExecutorService()");

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

        return serialWorkerExecutorService;
    }

    /**
     * @return the builder, for chaining
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    BlockingQueue<Runnable> getWorkerQueue() {
        Log.v(TAG, "getWorkerQueue()");

        if (workerQueue == null) {
            Log.d(TAG, "Creating default worker queue");
            setWorkerQueue(new LinkedBlockingDeque<>());
        }

        return workerQueue;
    }

    /**
     * @param queue
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setWorkerQueue(@NonNull final BlockingQueue<Runnable> queue) {
        Log.v(TAG, "setWorkerQueue(" + queue + ")");

        workerQueue = queue;
        return this;
    }

    /**
     * Call {@link #setWorkerQueue(java.util.concurrent.BlockingQueue)} before calling this method
     * if you wish to use something other than the default.
     *
     * @return the builder, for chaining
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    BlockingQueue<Runnable> getSerialWorkerQueue() {
        Log.v(TAG, "getSerialWorkerQueue()");

        if (serialWorkerQueue == null) {
            Log.d(TAG, "Creating default in-order worker queue");
            setSerialWorkerQueue(new DoubleQueue<>(getWorkerQueue()));
        }

        return serialWorkerQueue;
    }

    /**
     * @param queue of CPU-bound tasks for strict in-order execution
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setSerialWorkerQueue(@NonNull final BlockingQueue<Runnable> queue) {
        Log.v(TAG, "setSerialWorkerQueue(" + queue + ")");

        serialWorkerQueue = queue;
        return this;
    }

    /**
     * @return the builder, for chaining
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    BlockingQueue<Runnable> getFileQueue() {
        Log.v(TAG, "getFileQueue()");

        if (fileQueue == null) {
            Log.d(TAG, "Creating default file read queue");
            setFileQueue(new LinkedBlockingDeque<>());
        }

        return fileQueue;
    }

    /**
     * @param queue
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setFileQueue(@NonNull BlockingQueue<Runnable> queue) {
        Log.v(TAG, "setFileQueue(" + queue + ")");

        this.fileQueue = queue;
        return this;
    }

    /**
     * @return the builder, for chaining
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    BlockingQueue<Runnable> getNetReadQueue() {
        Log.v(TAG, "getNetReadQueue()");

        if (netReadQueue == null) {
            Log.d(TAG, "Creating default net read queue");
            setNetReadQueue(new LinkedBlockingDeque<>());
        }

        return netReadQueue;
    }

    /**
     * @param queue
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setNetReadQueue(@NonNull BlockingQueue<Runnable> queue) {
        Log.v(TAG, "setNetReadQueue(" + queue + ")");

        this.netReadQueue = queue;
        return this;
    }

    /**
     * @return the builder, for chaining
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    BlockingQueue<Runnable> getNetWriteQueue() {
        Log.v(TAG, "getNetWriteQueue()");

        if (netWriteQueue == null) {
            Log.d(TAG, "Creating default worker net write queue");
            setNetWriteQueue(new LinkedBlockingDeque<>());
        }

        return netWriteQueue;
    }

    /**
     * @param queue
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setNetWriteQueue(@NonNull BlockingQueue<Runnable> queue) {
        Log.v(TAG, "setNetWriteQueue(" + queue + ")");

        this.netWriteQueue = queue;
        return this;
    }

    /**
     * @param threadTypeImmutableValue
     * @return the builder, for chaining
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    ExecutorService getFileExecutorService(@NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        Log.v(TAG, "getFileExecutorService()");

        if (fileExecutorService == null) {
            Log.d(TAG, "Creating default file read executor service");
            setFileExecutorService(new ThreadPoolExecutor(1, 1,
                    0L, TimeUnit.MILLISECONDS,
                    getFileQueue(),
                    runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, createThreadId("FileThread")))
            );
        }

        return fileExecutorService;
    }

    /**
     * @param threadTypeImmutableValue
     * @return the builder, for chaining
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    ExecutorService getNetReadExecutorService(@NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        Log.v(TAG, "getNetReadExecutorService()");

        if (netReadExecutorService == null) {
            Log.d(TAG, "Creating default net read executor service");
            setNetReadExecutorService(new ThreadPoolExecutor(1, NUMBER_OF_CONCURRENT_NET_READS,
                    1000, TimeUnit.MILLISECONDS, getNetReadQueue(),
                    runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, createThreadId("NetReadThread")))
            );
        }

        return netReadExecutorService;
    }

    /**
     * @param threadTypeImmutableValue
     * @return the builder, for chaining
     */
    @NonNull
    @VisibleForTesting
    @UiThread
    ExecutorService getNetWriteExecutorService(@NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        Log.v(TAG, "getNetWriteExecutorService()");

        if (netWriteExecutorService == null) {
            Log.d(TAG, "Creating default net write executor service");
            setNetWriteExecutorService(Executors.newSingleThreadExecutor(
                    runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, createThreadId("NetWriteThread")))
            );
        }

        return netWriteExecutorService;
    }

    @NonNull
    @VisibleForTesting
    @UiThread
    static ExecutorService getUiExecutorService(@Nullable Context context) {
        Log.v(TAG, "getUiExecutorService()");

        if (uiExecutorService == null) {
            try {
                AsyncBuilder.uiExecutorService = new UIExecutorService(new Handler(context.getMainLooper()));
            } catch (Exception e) {
                Log.i(TAG, "WARNING: Test configuration- looper is synthesized");
                if (Looper.getMainLooper() == null) {
                    Looper.prepareMainLooper();
                }
                AsyncBuilder.uiExecutorService = new UIExecutorService(new Handler(Looper.getMainLooper()));
            }
        }

        return uiExecutorService;
    }

    /**
     * Note that if you override this, you may also want to override the associated default
     * {@link #setUiThread(Thread)}
     *
     * @param uiExecutorService
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setUiExecutorService(@NonNull ExecutorService uiExecutorService) {
        Log.v(TAG, "setUiExecutorService()");
        AsyncBuilder.uiExecutorService = uiExecutorService;
        return this;
    }

    /**
     * Note that if you override this, you may also want to override the associated
     * {@link #setWorkerThreadType(com.reactivecascade.i.IThreadType)} to for example match the <code>inOrderExecution</code> parameter
     *
     * @param executorService the service to be used for most callbacks split general purpose processing. The default implementation
     *                        is a threadpool sized based to match the number of CPU cores. Most operations on this executor
     *                        do not block for IO, those are relegated to specialized executors
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setWorkerExecutorService(@NonNull ExecutorService executorService) {
        Log.v(TAG, "setWorkerExecutorService(" + executorService + ")");
        workerExecutorService = executorService;
        return this;
    }

    /**
     * @param executorService
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setSerialWorkerExecutorService(@NonNull ExecutorService executorService) {
        Log.v(TAG, "setSerialWorkerExecutorService(" + executorService + ")");
        serialWorkerExecutorService = executorService;
        return this;
    }

    /**
     * Indicate that all {@link Async#WORKER} tasks are single threaded on {@link Async#SERIAL_WORKER}.
     * <p>
     * This may be useful to temporarily add to your builder for testing if you wish to test if issues
     * are facing are caused or not caused by background task concurrency.
     *
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder singleThreadedWorkerExecutorService() {
        Log.v(TAG, "singleThreadedWorkerExecutorService()");
        final ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
        this.workerExecutorService = Executors.newSingleThreadScheduledExecutor(
                runnable ->
                        new TypedThread(threadTypeImmutableValue.get(), runnable, createThreadId("SingleThreadedWorker"))
        );
        threadTypeImmutableValue.set(getWorkerThreadType());
        return this;
    }

    /**
     * @param fileExecutorService to be used for file reading and writing
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setFileExecutorService(@NonNull ExecutorService fileExecutorService) {
        Log.v(TAG, "setFileExecutorService(" + fileExecutorService + ")");
        this.fileExecutorService = fileExecutorService;
        return this;
    }

    /**
     * @param netReadExecutorService
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setNetReadExecutorService(@NonNull ExecutorService netReadExecutorService) {
        Log.v(TAG, "setNetReadExecutorService(" + netReadExecutorService + ")");
        this.netReadExecutorService = netReadExecutorService;
        return this;
    }

    /**
     * Retrieve or create the thread group that will hand net writes.
     *
     * @param netWriteExecutorService
     * @return the builder, for chaining
     */
    @NonNull
    @UiThread
    public AsyncBuilder setNetWriteExecutorService(@NonNull ExecutorService netWriteExecutorService) {
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
    @UiThread
    public AsyncBuilder setUiThread(@NonNull Thread uiThread) {
        Log.v(TAG, "setUiThread(" + uiThread + ")");
        uiThread.setName("UIThread");
        AsyncBuilder.uiThread = uiThread;

        return this;
    }

    @VisibleForTesting
    static Thread getUiThread(@Nullable Context context) {
        Thread thread = uiThread;
        if (thread == null) {
            thread = Thread.currentThread();
            if (context != null) {
                thread = context.getMainLooper().getThread();
            } else {
                Log.i(TAG, "WARNING- Test and tools mode: UI thread may not be the actual system UI thread");
            }
        }

        return thread;
    }

    /**
     * Complete construction of the {@link AsyncBuilder}.
     * <p>
     * The first (and usually only unless doing testing) call to this method locks in the implementation
     * configurion defaults of the {@link Async} utility class and all the services it provides.
     *
     * @return the newly created Async class. This from can be ignored if you prefer to <code>static import com.futurice.Async.*</code>
     * for convenience.
     */
    @NonNull
    @NotCallOrigin
    @UiThread
    public Async build() {
        if (strictMode) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        getWorkerThreadType();
        getSerialWorkerThreadType();
        getNetReadThreadType();
        getNetWriteThreadType();
        getFileThreadType();

        /*
         * End of low level one time initialization stage
         * From the following statement onward, static values in
         * the Async class are fixed until the next program start
         */
        getUiExecutorService(context);
        IAsyncOrigin origin = AsyncBuilder.traceAsyncOrigin ? new Origin() : IAsyncOrigin.ORIGIN_NOT_SET;
        AsyncBuilder.workerThreadType.setOrigin(origin);
        AsyncBuilder.serialWorkerThreadType.setOrigin(origin);
        AsyncBuilder.netReadThreadType.setOrigin(origin);
        AsyncBuilder.netWriteThreadType.setOrigin(origin);
        AsyncBuilder.fileThreadType.setOrigin(origin);

        Async async = new Async(context);
        Log.v(TAG, "AsyncBuilder complete");
        initialized = true;

        return async; //TODO Pass the builder as an argument to the constructor
    }
}
