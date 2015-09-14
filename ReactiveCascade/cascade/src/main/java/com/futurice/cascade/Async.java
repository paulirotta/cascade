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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.active.IRunnableAltFuture;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IActionR;
import com.futurice.cascade.i.IActionTwo;
import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.IOnErrorAction;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.cascade.util.AbstractThreadType;
import com.futurice.cascade.util.DefaultThreadType;
import com.futurice.cascade.util.TypedThread;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

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
    private static final ConcurrentHashMap<String, Class> sClassNameMap = new ConcurrentHashMap<>(); // "classname" -> Class. Used by DEBUG builds to more quickly trace mOrigin of a log message back into your code
    private static final ConcurrentHashMap<String, Method> sMethodNameMap = new ConcurrentHashMap<>(); // "classname-methodname" -> Method. Used by DEBUG builds to more quickly trace mOrigin of a log message back into your code
    private static volatile boolean sExitWithErrorCodeStarted = false;

    static {
        if (!AsyncBuilder.isInitialized()) {
            Exception e = new IllegalStateException(AsyncBuilder.NOT_INITIALIZED);
            Log.e(Async.class.getSimpleName(), AsyncBuilder.NOT_INITIALIZED, e);
        }
    }

    private static final AsyncBuilder ASYNC_BUILDER = AsyncBuilder.sAsyncBuilder; // The builder used to create the _first_ instance of ThreadType, the one which receives convenient static bindings of commonly used features
    public static final boolean DEBUG = (ASYNC_BUILDER == null) || ASYNC_BUILDER.debug; //BuildConfig.DEBUG; // true in debugOrigin builds, false in production builds, determined at build time to help JAVAC and PROGUARD clean out debugOrigin-only support code for speed and size
    //TODO Periodically check if recent Android updates have fixed this gradle bug, https://code.google.com/p/android/issues/detail?id=52962
    //TODO Manual gradle work-around, https://gist.github.com/almozavr/d59e770d2a6386061fcb
    //TODO Add a flag for independently enabling or disable runtime assertions and tests at Gradle level. Currently many optimistic assumptions that can make the error show up only later are made when DEBUG==false, but this aggressive optimization might need to be switched off independently to verify if the code path difference is the problem or help when the problem is speed sensitive
    //    public static final boolean VISUALIZE = false;
    public static final boolean TRACE_ASYNC_ORIGIN = (ASYNC_BUILDER == null) || ASYNC_BUILDER.mShowErrorStackTraces; // This makes finding where in you code a given log line was directly or indirectly called, but slows running
    // Some of the following logic lines are funky to support the Android visual editor. If you never initialized Async, you will want to see something in the visual editor. This matters for UI classes which receive services from Async
    public static final Thread UI_THREAD = (ASYNC_BUILDER == null) ? null : ASYNC_BUILDER.mUiThread; // The main system thread for this Context
    public static final boolean FAIL_FAST = (ASYNC_BUILDER == null) || ASYNC_BUILDER.mFailFast; // Default true- stop on the first error in debugOrigin builds to make debugging from the first point of failure easier
    public static volatile boolean SHOW_ERROR_STACK_TRACES = (ASYNC_BUILDER == null) || ASYNC_BUILDER.mShowErrorStackTraces; // For clean unit testing. This can be temporarily turned off for a single threaded system or unit test code block to keep _intentional_ unit test errors from cluttering the stack trace.

    private static final int FAIL_FAST_SLEEP_BEFORE_SYSTEM_EXIT = 5000; // Only if FAIL_FAST is true. The idea is this helps the user and debugger see the issue and logs can catch up before bombing the app a bit too fast to see what was happening
    private static final ImmutableValue<String> DEFAULT_ORIGIN = new ImmutableValue<>("No mOrigin provided in production builds");

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
    public static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Timer"));

    Async() {
    }

    private static void exitWithErrorCode(
            @NonNull @nonnull final String tag,
            @NonNull @nonnull final String message,
            @NonNull @nonnull final Throwable t) {
        final int errorCode = 1;

        // Kill the app hard after some delay. You are not allowed to refire this Intent in some critical phases (Activity startup)
        //TODO let the Activity or Service down slowly and gently with lifecycle callbacks if production build
        if (sExitWithErrorCodeStarted) {
            Log.v(tag, "Already existing, ignoring exit with error code (" + errorCode + "): " + message + "-" + t);
        } else {
            Log.e(tag, "Exit with error code (" + errorCode + "): " + message, t);
            sExitWithErrorCodeStarted = true; // Not a thread-safe perfect lock, but fast and good enough to generally avoid duplicate shutdown messages during debug
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

    private static String tagWithAspectAndThreadName(@NonNull @nonnull final String message) {
        if (!DEBUG || message.contains("at .")) {
            return message;
        }
        final IThreadType threadType = currentThreadType();

        if (threadType != NON_CASCADE_THREAD) {
            return "<" + threadType.getName() + "," + Thread.currentThread().getName() + "> " + message;
        }

        return "<" + Thread.currentThread().getName() + "> " + message;
    }

    /**
     * Log an error. During debugOrigin builds, this will fail-fast end the current {@link android.content.Context}
     * <p>
     * All Exceptions except {@link java.lang.InterruptedException} will cause immediate termination
     * of the application. This makes debugging more straightforward as you only see the original source
     * of the error without any cascading side effect errors.
     * <p>
     * It is strongly recommended that you do NOT <code>interrupt()</code> ongoing operations. Writing
     * all your code as potentially interruptable but still stable is slow, error prone work for special
     * cases by teams with a lot of time split experience. However, if you do interrupt, this will not
     * cause a fail fast.
     * <p>
     * If you do not want fail fast during debugOrigin build, use the normal {@link android.util.Log} routines
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @param t       the {@link Throwable} which triggered this error message
     * @return <code>false</code> always, for simple error chaining without consuming the error
     */
    public static boolean e(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message,
            @NonNull @nonnull final Throwable t) {
        if (DEBUG) {
            if (SHOW_ERROR_STACK_TRACES) {
                log(tag, message, (ta, m) -> {
                    Log.e(getTag(ta), tagWithAspectAndThreadName(m), t);
                    if (FAIL_FAST && !((t instanceof InterruptedException) || (t instanceof CancellationException))) {
                        exitWithErrorCode(getTag(ta), m, t);
                    }
                });
            } else {
                log(tag, message, (ta, m) -> {
                    Log.d(getTag(ta), tagWithAspectAndThreadName(m) + " : " + t);
                    if (FAIL_FAST && !((t instanceof InterruptedException) || (t instanceof CancellationException))) {
                        exitWithErrorCode(getTag(ta), m, t);
                    }
                });
            }
        }

        return false;
    }

    /**
     * Log an error. During debugOrigin builds, this will fail-fast end the current mContext
     * <p>
     * If you do not want fail fast during debugOrigin build, use the normal {@link android.util.Log} routines
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @return <code>false</code> always, for simple error chaining without consuming the error
     */
    @NotCallOrigin
    public static boolean e(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message) {
        if (DEBUG) {
            if (SHOW_ERROR_STACK_TRACES) {
                e(tag, message, new Exception("(Exception created to generate a stack trace)"));
            } else {
                d(tag, message + " !SHOW_ERROR_STACK_TRACES (Exception created to generate a stack trace)");
            }
        }

        return false;
    }

    /**
     * Log a verbose message including the thread name
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     */
    public static void v(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message) {
        log(tag, message, (ta, m) -> {
            Log.v("", tagWithAspectAndThreadName(m + getTag(ta)));
        });
    }

    /**
     * Log a debugOrigin message including the thread name
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     */
    public static void d(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message) {
        log(tag, message, (ta, m) -> {
            Log.d("", tagWithAspectAndThreadName(m + getTag(ta)));
        });
    }

    /**
     * Log an information message including the thread name
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     */
    public static void i(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message) {
        log(tag, message, (ta, m) -> {
            Log.i("", tagWithAspectAndThreadName(m + getTag(ta)));
        });
    }

    @SuppressWarnings("unchecked")
    private static void log(@NonNull @nonnull final Object tag,
                            @NonNull @nonnull final String message,
                            @NonNull final IActionTwo<String, String> action) {
        if (DEBUG) {
            if (tag instanceof ImmutableValue) {
                ((ImmutableValue<String>) tag).then(resolvedTag -> {
                    action.call(resolvedTag, message);
                });
            } else {
                try {
                    action.call(getTag(tag), message);
                } catch (Exception e) {
                    Log.e("Async", "Problem with logging: " + tag + " : " + message, e);
                }
            }
        }
    }

    /**
     * Log at the debug level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param origin  where in your code the <code>Object </code> hosting this message was originally created. This is also included in the log line as a clickable link.
     * @param message a message to display in the debug log
     */
    public static void dd(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final ImmutableValue<String> origin,
            @NonNull @nonnull final String message) {
        if (DEBUG) {
            debugOriginThen(ccOrigin -> {
                origin.then(o -> {
                    d(tag, combineOriginStringsRemoveDuplicates(o, ccOrigin, message));
                });
            });
        }
    }

    /**
     * Log at the debug level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message a message to display in the debug log
     */
    public static void dd(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message) {
        if (DEBUG) {
            debugOriginThen(origin -> d(tag, message + origin));
        }
    }

    //TODO If the object requesting this debug line implements INamed and (create) IOrigin and (create) IReasonCancelled, add these decorations automatically
    //TODO Shift to StringBuilder to reduce heavy logging overhead (mostly reflection, but...)

    /**
     * Log at the verbose level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param origin  Where in your code the <code>Object </code> hosting this message was originally created. This is also included in the log line as a clickable link.
     * @param message a message to display in the verbose log
     */
    public static void vv(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final ImmutableValue<String> origin,
            @NonNull @nonnull final String message) {
        if (DEBUG) {
            debugOriginThen(ccOrigin -> {
                origin.then(o -> {
                    v(tag, combineOriginStringsRemoveDuplicates(o, ccOrigin, message));
                });
            });
        }
    }

    /**
     * Log at the verbose level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message a message to display in the verbose log
     */
    public static void vv(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message) {
        if (DEBUG) {
            debugOriginThen(origin -> v(tag, message + origin));
        }
    }

    /**
     * Log at the error level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param origin  Where in your code the <code>Object </code> hosting this message was originally created. This is also included in the log line as a clickable link.
     * @param message a message to display in the error log
     * @param t       the throwable which triggered this error
     * @return <code>false</code> always, for simple error chaining without consuming the error, see {@link IOnErrorAction}
     */
    public static boolean ee(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final ImmutableValue<String> origin,
            @NonNull @nonnull final String message,
            @NonNull @nonnull final Throwable t) {
        if (DEBUG) {
            if (SHOW_ERROR_STACK_TRACES) {
                debugOriginThen(ccOrigin -> {
                    origin.then(o -> {
                        e(tag, combineOriginStringsRemoveDuplicates(o, ccOrigin, message), t);
                    });
                });
            } else {
                debugOriginThen(ccOrigin -> {
                    origin.then(o -> {
                        d(tag, combineOriginStringsRemoveDuplicates(o, ccOrigin, message + " " + t));
                    });
                });
            }
        }

        return false;
    }

    /**
     * Log at the error level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message a message to display in the error log
     * @param t       the throwable which triggered this error
     * @return <code>false</code> always, for simple error chaining without consuming the error, see {@link IOnErrorAction}
     */
    public static boolean ee(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message,
            @NonNull @nonnull final Throwable t) {
        if (DEBUG) {
            if (SHOW_ERROR_STACK_TRACES) {
                debugOriginThen(origin -> e(tag, message + origin, t));
            } else {
                debugOriginThen(origin -> d(tag, message + " " + t + origin));
            }
        }

        return false;
    }

    /**
     * Log at the information level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param origin  Where in your code the <code>Object </code> hosting this message was originally created. This is also included in the log line as a clickable link.
     * @param message a message to display in the info log
     */
    public static void ii(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final ImmutableValue<String> origin,
            @NonNull @nonnull final String message) {
        if (DEBUG) {
            debugOriginThen(ccOrigin -> {
                origin.then(o -> {
                    i(tag, combineOriginStringsRemoveDuplicates(o, ccOrigin, message));
                });
            });
        }
    }

    /**
     * Log at the information level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message a message to display in the info log
     */
    public static void ii(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message) {
        if (DEBUG) {
            debugOriginThen(origin -> i(tag, message + origin));
        }
    }

    private static String combineOriginStringsRemoveDuplicates(
            @NonNull @nonnull final String origin1,
            @NonNull @nonnull final String origin2,
            @NonNull @nonnull final String message) {
        if (origin1.equals(origin2)) {
            return message + origin1;
        }

        return message + origin1 + origin2;
    }

    /**
     * A nicely printable object INamed:name or classname or the provided string to add to the log
     *
     * @param tag a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @return a string representation of the object, ideally in a clear form such as the developer-assigned name
     */
    @NonNull
    private static String getTag(@NonNull @nonnull final Object tag) {
        if (tag instanceof String) {
            return (String) tag;
        }
        if (tag instanceof INamed) {
            return tag.getClass().getSimpleName() + '-' + ((INamed) tag).getName();
        }
//        if (tag instanceof ImmutableValue) {
//            return ((ImmutableValue) tag).get().toString();
//        }

        return tag.getClass().getSimpleName();
    }

//    /**
//     * Pass a signal to the JavaScript visualizer client running on the LAN
//     *
//     * @param tag
//     * @param value
//     * @param extraInfo
//     */
//    public static void visualize(Object tag, String value, String extraInfo) {
//        if (signalVisualizerClient != null) {
//            signalVisualizerClient.sendEventMessage(getTag(tag), System.currentTimeMillis(), value, extraInfo);
//        }
//    }


//    /**
//     * Pass a signal to the JavaScript visualizer client running on the LAN
//     *
//     * @param tag
//     * @param value
//     * @param extraInfo
//     */
//    public static void visualize(Object tag, long value, String extraInfo) {
//        if (signalVisualizerClient != null) {
//            signalVisualizerClient.sendEventMessage(getTag(tag), System.currentTimeMillis(), value, extraInfo);
//        }
//    }

//    /**
//     * Pass a signal to the JavaScript visualizer client running on the LAN
//     *
//     * @param tag
//     * @param value
//     * @param extraInfo
//     */
//    public static void visualize(Object tag, JSONObject value, String extraInfo) {
//        if (signalVisualizerClient != null) {
//            signalVisualizerClient.sendEventMessage(getTag(tag), System.currentTimeMillis(), value.toString(), extraInfo);
//        }
//    }

    /**
     * Generate an easy-to-debug stop signal at this point in a debug build
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @throws RuntimeException
     */
    public static void throwIllegalStateException(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message)
            throws RuntimeException {
        throwRuntimeException(tag, message, new IllegalStateException(message));
    }

    /**
     * Generate an easy-to-debug stop signal at this point in a debug build
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param origin  a link to the point from which the object throwing the exception was called in the application code
     * @param message to display and help the developer resolve the issue
     * @throws RuntimeException
     */
    public static void throwIllegalStateException(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final ImmutableValue<String> origin,
            @NonNull @nonnull final String message)
            throws RuntimeException {
        throwRuntimeException(tag, origin, message, new IllegalStateException(message));
    }

    /**
     * Generate an easy-to-debug stop signal at this point in a debug build
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @throws RuntimeException
     */
    public static void throwIllegalArgumentException(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message)
            throws RuntimeException {
        throwRuntimeException(tag, message, new IllegalArgumentException(message));
    }

    /**
     * Generate an easy-to-debug stop signal at this point in a debug build
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param origin  the point from which the object throwing the exception was called
     * @param message to display and help the developer resolve the issue
     * @throws RuntimeException
     */
    public static void throwIllegalArgumentException(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final ImmutableValue<String> origin,
            @NonNull @nonnull final String message)
            throws RuntimeException {
        throwRuntimeException(tag, origin, message, new IllegalArgumentException(message));
    }

    /**
     * Create a detailed log with a {@link RuntimeException} thrown at the current code point
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @param t       the throwable which triggered this new {@link RuntimeException}
     * @throws RuntimeException
     */
    public static void throwRuntimeException(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message,
            @NonNull @nonnull final Throwable t)
            throws RuntimeException {
        RuntimeException e;

        if (t instanceof RuntimeException) {
            e = (RuntimeException) t;
        } else {
            e = new RuntimeException(message, t);
        }
        ee(tag, message, t);
        throw e;
    }

    /**
     * Create a detailed log with a {@link TimeoutException} thrown at the current code point
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @throws RuntimeException
     */
    public static void throwTimeoutException(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final String message)
            throws RuntimeException {
        TimeoutException e = new TimeoutException(message);
        ee(tag, message, e);
        throw new RuntimeException(e);
    }

    /**
     * Create a detailed log with a {@link RuntimeException} thrown at the current code point
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param origin  a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @param t       the throwable which triggered this new {@link RuntimeException}
     * @throws RuntimeException
     */
    public static void throwRuntimeException(
            @NonNull @nonnull final Object tag,
            @NonNull @nonnull final ImmutableValue<String> origin,
            @NonNull @nonnull final String message,
            @NonNull @nonnull final Throwable t)
            throws RuntimeException {
        RuntimeException e;

        if (t instanceof RuntimeException) {
            e = (RuntimeException) t;
        } else {
            e = new RuntimeException(message, t);
        }
        ee(tag, origin, message, t);
        throw e;
    }

    /**
     * If there is an error, where did this asynchronous operation originally start? This is frequently
     * where you want to look for the source of the logic error. It is a rapid development style to reduce
     * workflow complexity and focus on the originAsync of the problem while avoiding the long stack trace of
     * useless jumbly$$lambda@anonymous#Unknown$undocumented-lazy
     * <p>
     * This affects debugOrigin builds only. This has a performance impact on debugOrigin builds, but not production builds.
     * <p>
     * Search the current call stack for the "most relevant" line to remember at the point where we create an
     * asynchronous operation.
     * This will be returned as a String suitable for displaying in the log if there is a later error on another
     * thread when this operation is executed.
     * <p>
     * Note that if this is is the first
     * such string in a line displayed in the Android log, it will be clickable to return to the
     * "most relevant" line.
     * <p>
     * This is used to make it easy to debugOrigin when cause and effect are likely separated. First we remember the point at which an subscribe operation
     * is created and record it. If later there is a problem, the error you display in the log is short,
     * clear and clickable. Most importantly, it shows you not a vomit of call stack but only where the errant active chain
     * started such as in your user or library code.
     * <p>
     * This information is frequently displayed along with the full stack trace at the point the error
     * manifests itself. First look at what when wrong at that point, subscribe work backwards to how you
     * created the mess. Yes, it is always your fault. Or my fault. Nah.
     *
     * @return a string holder that will be populated in the background on a WORKER thread (only in {@link #DEBUG} builds)
     */
    @NonNull
    @nonnull
    @CheckResult(suggest = "<local variable> mOrigin =")
    public static ImmutableValue<String> originAsync() {
        //FIXME Example static iniailiazer object is not a clickable link: .<init>(ServiceSingleton.java:59))  DO STRING REPLACE on <init>
        if (!TRACE_ASYNC_ORIGIN) {
            return DEFAULT_ORIGIN;
        }

        final StackTraceElement[] traceElementsArray = Thread.currentThread().getStackTrace();
        final ImmutableValue<String> immutableValue = new ImmutableValue<>();

        if (WORKER != null) {
            WORKER.run(() -> immutableValue.set(prettyFormat(origin(traceElementsArray).get(0).stackTraceElement)));
        } else {
            // During bootstrapping of the ThreadTypes
            final List<StackTaceLine> list = origin(traceElementsArray);
            immutableValue.set(prettyFormat(list.get(0).stackTraceElement));
        }

        return immutableValue;
    }

    /**
     * Extract from the current stack trace the most interesting "mOrigin" line from which this was
     * called. Once this is done on a background thread, pass this short text to action
     *
     * @param action to be performed when the current stack trace is resolved asynchronously
     */
    private static void debugOriginThen(@NonNull @nonnull final IActionOne<String> action) {
        try {
            if (TRACE_ASYNC_ORIGIN && WORKER != null) {
                originAsync().then(action);
            } else {
                action.call("");
            }
        } catch (Exception e) {
            Log.e(Async.class.getSimpleName(), "Problem in debugOriginThen()", e);
        }
    }

//    /**
//     * Perform an action once both the async-resolved object creation call stack mOrigin and
//     * async current call stack mOrigin are settled
//     *
//     * @param objectCreationOrigin a text pointer to the line where the calling object as originally constructed
//     * @param action               to be performed when the objectCreationOrigin is resolved (possibly not yet and on a concurrent thread)
//     */
//    private static void debugOriginThen(
//            @NonNull @nonnull final ImmutableValue<String> objectCreationOrigin,
//            @NonNull @nonnull final IActionTwo<String, String> action) {
//        if (TRACE_ASYNC_ORIGIN) {
//            final ImmutableValue<String> currentCallOrigin = originAsync();
//            objectCreationOrigin.then(
//                    creationOrigin -> {
//                        currentCallOrigin.then(
//                                callOrigin -> action.call(creationOrigin, callOrigin));
//                    });
//        } else if (DEBUG) {
//            try {
//                action.call("", "");
//            } catch (Exception e) {
//                Log.e(Async.class.getSimpleName(), "Problem in debugOriginThen()", e);
//            }
//        }
//    }

    @NonNull
    @nonnull
    private static List<StackTaceLine> origin(@NonNull @nonnull final StackTraceElement[] traceElementsArray) {
        final List<StackTraceElement> allStackTraceElements = new ArrayList<>(traceElementsArray.length - 3);

        allStackTraceElements.addAll(Arrays.asList(traceElementsArray).subList(3, traceElementsArray.length));

        // Remove uninteresting stack trace elements in least-interesting-removed-first order, but step back to the previous state if everything is removed by one of these filters
        List<StackTaceLine> previousList = findClassAndMethod(allStackTraceElements);

        try {
            previousList = filterListByClass(previousList, claz -> claz != Async.class);
            previousList = filterListByClass(previousList, claz -> claz != AbstractThreadType.class);
            previousList = filterListByPackage(previousList, packag -> !packag.startsWith("dalvik"));
            previousList = filterListByPackage(previousList, packag -> !packag.startsWith("java"));
            previousList = filterListByPackage(previousList, packag -> !packag.startsWith("com.sun"));
            previousList = filterListByPackage(previousList, packag -> !packag.startsWith("android"));
            previousList = filterListByPackage(previousList, packag -> !packag.startsWith("com.android"));
            previousList = filterListByMethod(previousList, method -> !method.isAnnotationPresent(NotCallOrigin.class));
            previousList = filterListByClassAnnotation(previousList, NotCallOrigin.class, true);
        } catch (Exception e) {
            e(Async.class.getSimpleName(), "Problem filtering stack chain", e);
        }
        return previousList;
    }

    @NonNull
    @nonnull
    private static List<StackTaceLine> findClassAndMethod(@NonNull @nonnull final List<StackTraceElement> stackTraceElementList) {
        final List<StackTaceLine> lines = new ArrayList<>(stackTraceElementList.size());

        for (final StackTraceElement ste : stackTraceElementList) {
            final String s = ste.toString();
            if (!s.contains("Native Method") && !s.contains("Unknown Source")) {
                try {
                    lines.add(new StackTaceLine(ste));
                } catch (ClassNotFoundException e) {
                    ee(Async.class.getSimpleName(), "Can not find method " + ste.getMethodName() + " when introspecting stack trace class: " + ste.getClassName(), e);
                }
            }
        }

        return lines;
    }

    @NonNull
    @nonnull
    private static List<StackTaceLine> filterListByClass(
            @NonNull @nonnull final List<StackTaceLine> list,
            @NonNull @nonnull final IActionOneR<Class, Boolean> classFilter) throws
            Exception {
        final List<StackTaceLine> filteredList = new ArrayList<>(list.size());

        for (final StackTaceLine line : list) {
            if (classFilter.call(line.claz)) {
                filteredList.add(line);
            }
        }
        if (filteredList.size() > 0) {
            return filteredList;
        }

        return list;
    }

    @NonNull
    @nonnull
    private static List<StackTaceLine> filterListByClassAnnotation(
            @NonNull @nonnull final List<StackTaceLine> list,
            @NonNull @nonnull final Class<? extends Annotation> annotation,
            final boolean mustBeAbsent)
            throws Exception {
        final List<StackTaceLine> filteredList = new ArrayList<>(list.size());

        for (final StackTaceLine line : list) {
            if (line.claz.isAnnotationPresent(annotation) ^ mustBeAbsent) {
                filteredList.add(line);
            }
        }
        if (filteredList.size() > 0) {
            return filteredList;
        }

        return list;
    }

    @NonNull
    @nonnull
    private static List<StackTaceLine> filterListByMethod(
            @NonNull @nonnull final List<StackTaceLine> list,
            @NonNull @nonnull final IActionOneR<Method, Boolean> methodFilter)
            throws Exception {
        final List<StackTaceLine> filteredList = new ArrayList<>(list.size());

        for (final StackTaceLine line : list) {
            final Method m = line.method.get();
            if (m != null && methodFilter.call(m)) {
                filteredList.add(line);
            }
        }
        if (filteredList.size() > 0) {
            return filteredList;
        }

        return list;
    }

    @NonNull
    @nonnull
    private static List<StackTaceLine> filterListByPackage(
            @NonNull @nonnull final List<StackTaceLine> list,
            @NonNull @nonnull final IActionOneR<String, Boolean> packageFilter)
            throws Exception {
        final List<StackTaceLine> filteredList = new ArrayList<>(list.size());

        for (final StackTaceLine line : list) {
            if (packageFilter.call(line.claz.getPackage().getName())) {
                filteredList.add(line);
            }
        }
        if (filteredList.size() > 0) {
            return filteredList;
        }

        return list;
    }

    @NonNull
    @nonnull
    private static String prettyFormat(@NonNull @nonnull final StackTraceElement stackTraceElement) {
        final String s = stackTraceElement.toString();
        final int i = stackTraceElement.getClassName().length();

        return '\n' + s.substring(i);
    }

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
    @nonnull
    public static IThreadType currentThreadType() {
        final Thread thread = Thread.currentThread();
        IThreadType threadType = NON_CASCADE_THREAD;

        if (thread instanceof TypedThread) {
            threadType = ((TypedThread) thread).getThreadType();
        } else if (isUiThread()) {
            threadType = UI;
        }

        return threadType;
    }

    /**
     * Check if currently running on the main system or "user interface" thread
     *
     * @return <code>true</code> if this is the one true system thread for the current {@link android.content.Context}
     */
    public static boolean isUiThread() {
        return Thread.currentThread() == UI_THREAD;
    }

    /**
     * In DEBUG builds only, check the condition specified. If that is not satisfied, abort the current
     * active chain by throwing an {@link java.lang.IllegalStateException} with the explanation errorMessage.
     *
     * @param errorMessage a message to display when the assertion fails. It should indicate the
     *                     reason which was not true and, if possible, the likely corrective action
     * @param testResult   the result of the test, <code>true</code> if the assertion condition is met
     */
    @NotCallOrigin
    public static void assertTrue(
            @NonNull @nonnull final String errorMessage,
            final boolean testResult) {
        if (DEBUG && !testResult) {
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * In DEBUG builds only, check the condition specified. If that is not satisfied, abort the current
     * active chain by throwing an {@link java.lang.IllegalStateException} with the explanation  error message
     *
     * @param expected
     * @param actual
     * @param <T>
     */
    @NotCallOrigin
    public static <T> void assertEqual(
            @Nullable @nullable final T expected,
            @Nullable @nullable final T actual) {
        if (DEBUG
                && actual != expected
                && ((actual == null && !expected.equals(actual)) || !actual.equals(expected))) {
            throw new IllegalStateException("assertEqual failed: expected ´'" + expected + "' but was '" + actual + "'");
        }
    }

    /**
     * In DEBUG builds only, check the condition specified. If that is not satisfied, abort the current
     * active chain by throwing an {@link java.lang.IllegalStateException} with the explanation  error message
     *
     * @param expected
     * @param actual
     * @param <T>
     */
    @NotCallOrigin
    public static <T> void assertNotEqual(
            @Nullable @nullable final T expected,
            @Nullable @nullable final T actual) {
        if (DEBUG
                && actual == expected
                && ((actual == null && expected.equals(actual)) || actual.equals(expected))) {
            throw new IllegalStateException("assertEqual failed: expected ´'" + expected + "' but was '" + actual + "'");
        }
    }

    /**
     * In debug and production builds, throw {@link NullPointerException} if the argumen is null
     *
     * @param t   the argument
     * @param <T> the type
     * @return the value, guaranteed to be non-null and annotated at <code>@NonNull @nonnull</code> for rapidly catching errors in the IDE
     */
    @NonNull
    @nonnull
    @NotCallOrigin
    public static <T> T assertNotNull(@Nullable @nullable final T t) {
        if (t == null) {
            throw new NullPointerException();
        }
        return t;
    }

    /**
     * A marker value returned when a thread is not part of Cascade
     */
    public static final IThreadType NON_CASCADE_THREAD = new IThreadType() {
        @Override
        public final boolean isInOrderExecutor() {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @Override
        public final <IN> void execute(@NonNull @nonnull IAction<IN> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @Override
        public final void run(@NonNull @nonnull Runnable runnable) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @Override
        public final <IN> void run(@NonNull @nonnull IAction<IN> action, @NonNull @nonnull IOnErrorAction onErrorAction) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @Override
        public final <IN> void runNext(@NonNull @nonnull IAction<IN> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @Override
        public final void runNext(@NonNull @nonnull Runnable runnable) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @Override
        public final boolean moveToHeadOfQueue(@NonNull @nonnull Runnable runnable) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @Override
        public final <IN> void runNext(@NonNull @nonnull IAction<IN> action, @NonNull @nonnull IOnErrorAction onErrorAction) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        public final <IN> Runnable wrapActionWithErrorProtection(@NonNull @nonnull IAction<IN> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        public final <IN> Runnable wrapActionWithErrorProtection(@NonNull @nonnull IAction<IN> action, @NonNull @nonnull IOnErrorAction onErrorAction) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        public final <IN> IAltFuture<IN, IN> then(@NonNull @nonnull IAction<IN> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        public final <IN> IAltFuture<IN, IN> then(@NonNull @nonnull IActionOne<IN> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        @SafeVarargs
        public final <IN> List<IAltFuture<IN, IN>> then(@NonNull @nonnull IAction<IN>... actions) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        public final <IN> IAltFuture<?, IN> from(@NonNull @nonnull IN value) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        public final <IN, OUT> IAltFuture<IN, OUT> then(@NonNull @nonnull IActionR<IN, OUT> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        @SafeVarargs
        public final <IN, OUT> List<IAltFuture<IN, OUT>> then(@NonNull @nonnull IActionR<IN, OUT>... actions) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        public final <IN, OUT> IAltFuture<IN, OUT> map(@NonNull @nonnull IActionOneR<IN, OUT> action) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        @SafeVarargs
        public final <IN, OUT> List<IAltFuture<IN, OUT>> map(@NonNull @nonnull IActionOneR<IN, OUT>... actions) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @Override
        public final <IN, OUT> void fork(@NonNull @nonnull IRunnableAltFuture<IN, OUT> runnableAltFuture) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        public <IN> Future<Boolean> shutdown(long timeoutMillis, @Nullable @nullable IAction<IN> afterShutdownAction) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        public final <IN> List<Runnable> shutdownNow(@NonNull @nonnull String reason, @Nullable @nullable IAction<IN> actionOnDedicatedThreadAfterAlreadyStartedTasksComplete, @Nullable @nullable IAction<IN> actionOnDedicatedThreadIfTimeout, long timeoutMillis) {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }

        @NonNull
        @nonnull
        @Override
        public final String getName() {
            throw new UnsupportedOperationException("NON_CASCADE_THREAD is a marker and does not support execution");
        }
    };

    private static final class StackTaceLine {

        final Class<?> claz;
        final ImmutableValue<Method> method;
        final StackTraceElement stackTraceElement;

        StackTaceLine(@NonNull @nonnull final StackTraceElement stackTraceElement) throws ClassNotFoundException {
            this.stackTraceElement = stackTraceElement;
            final String className = stackTraceElement.getClassName();
            Class<?> c = sClassNameMap.get(className);

            if (c == null) {
                c = Class.forName(className);
                sClassNameMap.putIfAbsent(className, c);
            }
            this.claz = c;
            final String methodName = stackTraceElement.getMethodName();
            final String key = className + methodName;
            this.method = new ImmutableValue<>(
                    () -> {
                        Method meth = sMethodNameMap.get(key);

                        if (meth == null) {
                            final Method[] methods = claz.getMethods();
                            for (final Method m : methods) {
                                sMethodNameMap.putIfAbsent(key, m);
                                if (m.getName().equals(methodName)) {
                                    meth = m;
                                    break;
                                }
                            }
                        }
                        return meth;
                    });
        }
    }
}
