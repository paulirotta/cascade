/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.reactivecascade.i.IAction;
import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IActionOneR;
import com.reactivecascade.i.IActionR;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IAsyncOrigin;
import com.reactivecascade.i.IBindingContext;
import com.reactivecascade.i.IRunnableAltFuture;
import com.reactivecascade.i.ISettableAltFuture;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.util.BindingContextUtil;
import com.reactivecascade.util.DefaultThreadType;
import com.reactivecascade.util.TypedThread;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An IThreadType containing asynchronous threading relationships, resource concurrency limits for performance,
 * split default values associated with these. Individual calls may choose to override these defaults, but the
 * default separates concerns to make several cross-cutting concerns of architecture explicit split centrally
 * managed.
 * <p>
 * There is one "default" ThreadType, which is statically bound at class loading time based on the current
 * values of the {@link AsyncBuilder}. This increases the opportunities
 * for performance optimization by javac split Proguard. It also decreases the verbosity split boilerplate
 * clutter of using this IThreadType for common tasks.
 * <p>
 * You may for testing split other purposes create additional {@link com.reactivecascade.i.IThreadType}s. If you do,
 * pay attention that the underlying {@link java.util.concurrent.ExecutorService} is either
 * dedicated or shared for concurrency management split peak resource contention management.
 * <p>
 * Rather than create an entirely new {@link com.reactivecascade.i.IThreadType}, a more common need outside of system-level testing is to create
 * individual {@link com.reactivecascade.i.IThreadType} objects by creating a stand alone
 * {@link DefaultThreadType} for managing tasks in one section of your architecture.
 */
public final class Async {
    //TODO TIMER system tests
    public static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r ->
            new Thread(r, "Timer"));
    /**
     * A marker from returned when a thread is not part of Cascade.
     * <p>
     * See also {@link Async#UI}. The system thread / main thread / UI thread is not created by Cascade, it does
     * not extend this class. It is wrapped in a special implementation with the the assistance of
     * {@link com.reactivecascade.util.UIExecutorService}
     */
    public static final IThreadType NON_CASCADE_THREAD = new IThreadType() {
        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @Override // IThreadType
        public boolean isInOrderExecutor() {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @Override
        public boolean isCascadeThread() {
            return false;
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @Override // IThreadType
        public <IN> void execute(@NonNull IAction<IN> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @Override // IThreadType
        public void run(@NonNull Runnable runnable) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @Override // IThreadType
        public <IN> void run(@NonNull IAction<IN> action,
                             @NonNull IActionOne<Exception> onErrorAction) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @Override // IThreadType
        public <IN> void runNext(@NonNull IAction<IN> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @Override // IThreadType
        public void runNext(@NonNull Runnable runnable) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @Override // IThreadType
        public boolean moveToHeadOfQueue(@NonNull Runnable runnable) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @Override // IThreadType
        public <IN> void runNext(@NonNull IAction<IN> action,
                                 @NonNull IActionOne<Exception> onErrorAction) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override // IThreadType
        public <IN> Runnable wrapActionWithErrorProtection(@NonNull IAction<IN> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override // IThreadType
        public <IN> Runnable wrapActionWithErrorProtection(@NonNull IAction<IN> action,
                                                           @NonNull IActionOne<Exception> onErrorAction) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override // IThreadType
        public <IN> IAltFuture<IN, IN> then(@NonNull IAction<IN> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override // IThreadType
        public <IN> IAltFuture<IN, IN> then(@NonNull IActionOne<IN> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override // IThreadType
        public <OUT> ISettableAltFuture<OUT> from(@NonNull OUT value) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override // IThreadType
        public <OUT> ISettableAltFuture<OUT> from() {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override // IThreadType
        public <IN, OUT> IAltFuture<IN, OUT> then(@NonNull IActionR<OUT> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override // IThreadType
        public <IN, OUT> IAltFuture<IN, OUT> map(@NonNull IActionOneR<IN, OUT> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @Override // IThreadType
        public <IN, OUT> void fork(@NonNull IRunnableAltFuture<IN, OUT> runnableAltFuture) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @Override
        public void setOrigin(@NonNull IAsyncOrigin origin) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @Override // IThreadType
        public boolean isShutdown() {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override
        public List<Runnable> shutdownNow(@NonNull String reason, @Nullable IAction<?> actionOnDedicatedThreadAfterAlreadyStartedTasksComplete, @Nullable IAction<?> actionOnDedicatedThreadIfTimeout, long timeoutMillis) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override // IThreadType
        public <IN> Future<Boolean> shutdown(long timeoutMillis, @Nullable IAction<IN> afterShutdownAction) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        /**
         * This is a marker class only.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        @Override // INamed
        public String getName() {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }
    };

    /**
     * A setting controlling an additional state change which make debugging applications easier
     * but at the cost of a slight performance cost.
     * <p>
     * The default is <code>true</code> for debug builds, false for production builds. Change with {@link AsyncBuilder#setUseForkedState(boolean)}
     */
    public static final boolean USE_FORKED_STATE = AsyncBuilder.useForkedState;

    /**
     * A setting to enable or disable runtime assertion tests. These allow the contract of pre-conditions
     * for a section of code to be enforced, but can be turned off for additional speed.
     * <p>
     * The default is <code>true</code> for debug builds, false for production builds. Change with {@link AsyncBuilder#setRuntimeAssertionsEnabled(boolean)}
     */
    public static final boolean RUNTIME_ASSERTIONS = AsyncBuilder.runtimeAssertionsEnabled;

    /**
     * A setting to enable or disable runtime assertion tests. These allow the contract of pre-conditions
     * for a section of code to be enforced, but can be turned off for additional speed.
     * <p>
     * The default is <code>true</code> for debug builds, false for production builds. Change with {@link AsyncBuilder#setRuntimeAssertionsEnabled(boolean)}
     */
    public static final boolean TRACE_ASYNC_ORIGIN = AsyncBuilder.traceAsyncOrigin; // This makes finding where in you code a given log line was directly or indirectly called, but slows running

    @NonNull
    public static final Thread UI_THREAD = AsyncBuilder.getUiThread(null); // The main system thread, or the current thread if test/tooling has not initialized the library

    /**
     * Halt exectcution on first error
     * <p>
     * Default is <code>true</code>. Change with {@link AsyncBuilder#setFailFast(boolean)}
     */
    public static final boolean FAIL_FAST = AsyncBuilder.failFast; // Default true- stop on the first error in debugOrigin builds to make debugging from the first point of failure easier

    /**
     * The default {@link com.reactivecascade.i.IThreadType} for CPU-bound and background tasks.
     * <p>
     * Use this unless serialized {@link #UI} or other resource-related constraints should limit
     * concurrent execution of a function.
     * <p>
     * <code><pre>
     *     import static com.reactivecascade.Async.*;
     *     ..
     *     ArrayList&lt;String&gt; list = for (
     *     UI.sub(() -> textView.setText("Blah");
     * </pre></code>
     */
    @NonNull
    public static final IThreadType WORKER = AsyncBuilder.worker;

    @NonNull
    public static final IThreadType SERIAL_WORKER = AsyncBuilder.serialWorker;

    @VisibleForTesting
    @NonNull
    final IThreadType serialWorker = AsyncBuilder.serialWorker;
    /**
     * The default {@link com.reactivecascade.i.IThreadType} implementation which gives uniform access
     * to the system's {@link #UI_THREAD}. Example use:
     * <p>
     * <code><pre>
     *     import static com.reactivecascade.ThreadType.*;
     *     ..
     *     UI.sub(() -> textView.setText("Blah");
     * </pre></code>
     */
    @NonNull
    public static final IThreadType UI = AsyncBuilder.getUiThreadType(null /* fallback value for UI tools use */);

    @NonNull
    public static final IThreadType FILE = AsyncBuilder.file;

    /**
     * A group of background thread for concurrently reading from the network
     * <p>
     * TODO Automatically adjusted thread pool size based on current connection type
     */
    @NonNull
    public static final IThreadType NET_READ = AsyncBuilder.netRead;

    /**
     * A single thread for making writes to the network.
     * <p>
     * Upstream bandwidth on mobile is generally quite limited, so one write at a time will tend to help
     * tasks finish more quickly. This also simplifies cache invalidation on POST and PUT operations more
     * coherent.
     */
    @NonNull
    public static final IThreadType NET_WRITE = AsyncBuilder.netWrite;

    public static volatile boolean SHOW_ERROR_STACK_TRACES = AsyncBuilder.showErrorStackTraces; // For clean unit testing. This can be temporarily turned off for a single threaded system or unit test code block to keep _intentional_ unit test errors from cluttering the stack trace.
    private static final int FAIL_FAST_SLEEP_BEFORE_SYSTEM_EXIT = 1000; // The idea is this helps the user and debugger see the issue and logs can catch up before bombing the app too fast to see what was happening
    private static volatile boolean exitWithErrorCodeStarted = false;

    /**
     * Opened by the first call to {@link AsyncBuilder#build()}.
     *
     * This context is never closed
     * TODO Close the context for Service use
     * TODO Close the context during orderly Application closure
     */
    public static final IBindingContext<Context> DEFAULT_BINDING_CONTEXT = new BindingContextUtil.DefaultBindingContext<>();

    private static final String NOT_INITIALIZED = "Please invoke new AsyncBuilder(Context).build() earlier, for example in Activity.onCreate() or Application.onCreate()";

    static {
        if (!AsyncBuilder.initialized) {
            Log.e(Async.class.getSimpleName(), NOT_INITIALIZED, new IllegalStateException(NOT_INITIALIZED));
        }
    }

    @UiThread
    Async(@NonNull Context context) {
        if (!DEFAULT_BINDING_CONTEXT.isOpen()) {
            DEFAULT_BINDING_CONTEXT.openBindingContext(context);
        }
    }

    /**
     * Ugly hack from http://stackoverflow.com/questions/21367646/how-to-determine-if-android-application-is-started-with-junit-testing-instrument
     *
     * @return true if this is a test, or false if this is a production app
     */
    private static boolean isTestMode() {
        boolean result;
        try {
            Class.forName("com.reactivecascade.AsyncBuilderTest");
            result = true;
        } catch (final Exception e) {
            result = false;
        }
        return result;
    }

    public static void exitWithErrorCode(@NonNull String tag,
                                         @NonNull String message,
                                         @Nullable Throwable t) {
        if (isTestMode()) {
            // Some integration tests fail without this- the test harness is disturbed by app shutdown
            return;
        }

        final int errorCode = 1;

        // Kill the app hard after some delay. You are not allowed to refire this Intent in some critical phases (Activity startup)
        //TODO let the Activity or Service down slowly and gently with lifecycle callbacks if production build
        if (exitWithErrorCodeStarted) {
            Log.v(tag, "Already existing, ignoring exit with error code (" + errorCode + "): " + message + "-" + t);
        } else {
            exitWithErrorCodeStarted = true; // Not a thread-safe perfect lock, but fast and good enough to generally avoid duplicate shutdown messages during debug
            if (t != null) {
                Log.e(tag, "Exit with error code (" + errorCode + "): " + message, t);
            } else {
                Log.i(tag, "Exit, no error code : " + message);
            }
            WORKER.shutdownNow("exitWithErrorCode: " + message, null, null, 0);
            NET_READ.shutdownNow("exitWithErrorCode: " + message, null, null, 0);
            NET_WRITE.shutdownNow("exitWithErrorCode: " + message, null, null, 0);
            FILE.shutdownNow("exitWithErrorCode: " + message, null, null, 0);

            new Thread(() -> {
                try {
                    // Give the user time to see a popup split adb time to receive the error messages from this process before it dies
                    Thread.sleep(FAIL_FAST_SLEEP_BEFORE_SYSTEM_EXIT);
                } catch (Exception e2) {
                    Log.d(tag, "Problem while pausing before failfast system exit due to " + t, e2);
                }
                TIMER.shutdownNow();
                System.exit(errorCode);
            }, "FailFastDelayThread")
                    .start();
        }
    }

//    /**
//     * Pass a signal to the JavaScript visualizer client running on the LAN
//     *
//     * @param tag
//     * @param from
//     * @param extraInfo
//     */
//    public static void visualize(Object tag, String from, String extraInfo) {
//        if (signalVisualizerClient != null) {
//            signalVisualizerClient.sendEventMessage(getTag(tag), System.currentTimeMillis(), from, extraInfo);
//        }
//    }


//    /**
//     * Pass a signal to the JavaScript visualizer client running on the LAN
//     *
//     * @param tag
//     * @param from
//     * @param extraInfo
//     */
//    public static void visualize(Object tag, long from, String extraInfo) {
//        if (signalVisualizerClient != null) {
//            signalVisualizerClient.sendEventMessage(getTag(tag), System.currentTimeMillis(), from, extraInfo);
//        }
//    }

//    /**
//     * Pass a signal to the JavaScript visualizer client running on the LAN
//     *
//     * @param tag
//     * @param from
//     * @param extraInfo
//     */
//    public static void visualize(Object tag, JSONObject from, String extraInfo) {
//        if (signalVisualizerClient != null) {
//            signalVisualizerClient.sendEventMessage(getTag(tag), System.currentTimeMillis(), from.toString(), extraInfo);
//        }
//    }

    /**
     * If the current thread belongs to more than one <code>ThreadType</>, sub the returned ThreadType will be the one
     * which created the Thread
     * <p>
     * This is used for debugging only. For performance reasons it will always return <code>null</code>
     * in production builds.
     * <p>
     * Beware of debugging confusion if you use one Thread as part of the executor in multiple different ThreadTypes
     *
     * @return the current ThreadType, or the token {@link Async#NON_CASCADE_THREAD} if the thread is not part of the Cascade
     */
    @NonNull
    public static IThreadType currentThreadType() {
        final Thread thread = Thread.currentThread();

        if (thread instanceof TypedThread) {
            return ((TypedThread) thread).getThreadType();
        } else if (isUiThread()) {
            return UI;
        }

        return NON_CASCADE_THREAD;
    }

    /**
     * Check if currently running on the main system or "user interface" thread
     *
     * @return <code>true</code> if this is the one true system thread for the current {@link android.content.Context}
     */
    public static boolean isUiThread() {
        return Thread.currentThread() == UI_THREAD;
    }
}
