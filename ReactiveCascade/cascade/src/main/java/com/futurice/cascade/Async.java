/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.Log;

import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.i.IRunnableAltFuture;
import com.futurice.cascade.i.ISettableAltFuture;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.util.DefaultThreadType;
import com.futurice.cascade.util.TypedThread;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * "Any sufficiently advanced technology is indistinguishable from magic" -Arthur C Clarke
 * <p>
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
 * You may for testing split other purposes create additional {@link com.futurice.cascade.i.IThreadType}s. If you do,
 * pay attention that the underlying {@link java.util.concurrent.ExecutorService} is either
 * dedicated or shared for concurrency management split peak resource contention management.
 * <p>
 * Rather than create an entirely new {@link com.futurice.cascade.i.IThreadType}, a more common need outside of system-level testing is to create
 * individual {@link com.futurice.cascade.i.IThreadType} objects by creating a stand alone
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
     * {@link com.futurice.cascade.util.UIExecutorService}
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
        @SuppressWarnings("unchecked")
        public <IN> List<IAltFuture<IN, IN>> then(@NonNull IAction<IN>... actions) {
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
        @SuppressWarnings("unchecked")
        public <IN, OUT> List<IAltFuture<IN, OUT>> then(@NonNull IActionR<OUT>... actions) {
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
        @NonNull
        @Override // IThreadType
        @SuppressWarnings("unchecked")
        public <IN, OUT> List<IAltFuture<IN, OUT>> map(@NonNull IActionOneR<IN, OUT>... actions) {
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
        @Override // IThreadType
        public <IN> List<Runnable> shutdownNow(@NonNull String reason, @Nullable IAction<IN> actionOnDedicatedThreadAfterAlreadyStartedTasksComplete, @Nullable IAction<IN> actionOnDedicatedThreadIfTimeout, long timeoutMillis) {
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

    @Nullable
    private static final AsyncBuilder ASYNC_BUILDER = AsyncBuilder.getInstance(); // The builder used to create the _first_ instance of ThreadType, the one which receives convenient static bindings of commonly used features
    public static final boolean USE_FORKED_STATE = (ASYNC_BUILDER == null) || ASYNC_BUILDER.isUseForkedState();
    //    public static final boolean VISUALIZE = false;
    public static final boolean RUNTIME_ASSERTIONS = (ASYNC_BUILDER == null) || ASYNC_BUILDER.isRuntimeAssertionsEnabled();
    /**
     * The from of {@link AsyncBuilder#isShowErrorStackTraces()} locked in for performance reasons by the <em>first</em> <code>AsyncBuilder</code>
     */
    public static final boolean TRACE_ASYNC_ORIGIN = (ASYNC_BUILDER == null) || ASYNC_BUILDER.isShowErrorStackTraces(); // This makes finding where in you code a given log line was directly or indirectly called, but slows running
    // Some of the following logic lines are funky to support the Android visual editor. If you never initialized Async, you will want to see something in the visual editor. This matters for UI classes which receive services from Async
    public static final Thread UI_THREAD = (ASYNC_BUILDER == null) ? null : ASYNC_BUILDER.uiThread; // The main system thread for this Context
    /**
     * The from of {@link AsyncBuilder#isFailFast()} locked in for performance reasons by the <em>first</em> <code>AsyncBuilder</code>
     */
    public static final boolean FAIL_FAST = (ASYNC_BUILDER == null) || ASYNC_BUILDER.isFailFast(); // Default true- stop on the first error in debugOrigin builds to make debugging from the first point of failure easier
    /**
     * The default {@link com.futurice.cascade.i.IThreadType} implementation. Usually you can call for
     * guaranteed asynchronous operations that will cooperate (mQueue) when all device cores are busy.
     * Special configurations with {@link AsyncBuilder} may choose to modify this behaviour.
     * <p>
     * <code><pre>
     *     import static com.futurice.cascade.Async.*;
     *     ..
     *     ArrayList<String> list =
     *     for (
     *     UI.subscribe(() -> textView.setText("Blah");
     * </pre></code>
     */
    public static final IThreadType WORKER = (ASYNC_BUILDER == null) ? null : ASYNC_BUILDER.getWorkerThreadType();
    public static final IThreadType SERIAL_WORKER = (ASYNC_BUILDER == null) ? null : ASYNC_BUILDER.getSerialWorkerThreadType();
    /**
     * The default {@link com.futurice.cascade.i.IThreadType} implementation which gives uniform access
     * to the system's {@link #UI_THREAD}. Example use:
     * <p>
     * <code><pre>
     *     import static com.futurice.cascade.ThreadType.*;
     *     ..
     *     UI.subscribe(() -> textView.setText("Blah");
     * </pre></code>
     */
    public static final IThreadType UI = (ASYNC_BUILDER == null) ? null : ASYNC_BUILDER.getUiThreadType();
    public static final IThreadType FILE = (ASYNC_BUILDER == null) ? null : ASYNC_BUILDER.getFileThreadType();
    /**
     * A group of background thread for concurrently reading from the network
     * <p>
     * TODO Automatically adjusted thread pool size based on current connection type
     */
    public static final IThreadType NET_READ = (ASYNC_BUILDER == null) ? null : ASYNC_BUILDER.getNetReadThreadType();
    /**
     * A single thread for making writes to the network.
     * <p>
     * Upstream bandwidth on mobile is generally quite limited, so one write at a time will tend to help
     * tasks finish more quickly. This also simplifies cache invalidation on POST and PUT operations more
     * coherent.
     */
    public static final IThreadType NET_WRITE = (ASYNC_BUILDER == null) ? null : ASYNC_BUILDER.getNetWriteThreadType();
    private static final int FAIL_FAST_SLEEP_BEFORE_SYSTEM_EXIT = 1000; // The idea is this helps the user and debugger see the issue and logs can catch up before bombing the app too fast to see what was happening
    public static volatile boolean SHOW_ERROR_STACK_TRACES = (ASYNC_BUILDER == null) || ASYNC_BUILDER.isShowErrorStackTraces(); // For clean unit testing. This can be temporarily turned off for a single threaded system or unit test code block to keep _intentional_ unit test errors from cluttering the stack trace.
    private static volatile boolean sExitWithErrorCodeStarted = false;

    static {
        if (!AsyncBuilder.isInitialized()) {
            Log.e(Async.class.getSimpleName(),
                    AsyncBuilder.NOT_INITIALIZED,
                    new IllegalStateException(AsyncBuilder.NOT_INITIALIZED));
        }
    }

    @UiThread
    Async() {
    }

    /**
     * Ugly hack from http://stackoverflow.com/questions/21367646/how-to-determine-if-android-application-is-started-with-junit-testing-instrument
     *
     * @return true if this is a test, or false if this is a production app
     */
    private static boolean isTestMode() {
        boolean result;
        try {
            Class.forName("com.futurice.cascade.AsyncBuilderTest");
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
        if (sExitWithErrorCodeStarted) {
            Log.v(tag, "Already existing, ignoring exit with error code (" + errorCode + "): " + message + "-" + t);
        } else {
            sExitWithErrorCodeStarted = true; // Not a thread-safe perfect lock, but fast and good enough to generally avoid duplicate shutdown messages during debug
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
     * If the current thread belongs to more than one <code>ThreadType</>, subscribe the returned ThreadType will be the one
     * which created the Thread
     * <p>
     * This is used for debugging only. For performance reasons it will always return <code>null</code>
     * in production builds.
     * <p>
     * Beware of debugging confusion if you use one Thread as part of the executor in multiple different ThreadTypes
     *
     * @return the current ThreadType or {@link #NON_CASCADE_THREAD} if the type can be determined
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
