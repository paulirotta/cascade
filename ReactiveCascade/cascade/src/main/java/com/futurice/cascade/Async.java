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

import android.support.annotation.NonNull;
import android.util.Log;

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.action.IActionOneR;
import com.futurice.cascade.i.action.IActionR;
import com.futurice.cascade.i.action.IActionTwo;
import com.futurice.cascade.i.action.IBaseAction;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.rest.RESTService;
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
    private static final ConcurrentHashMap<String, Class> classNameMap = new ConcurrentHashMap<>(); // "classname" -> Class. Used by DEBUG builds to more quickly trace origin of a log message back into your code
    private static final ConcurrentHashMap<String, Method> methodNameMap = new ConcurrentHashMap<>(); // "classname-methodname" -> Method. Used by DEBUG builds to more quickly trace origin of a log message back into your code
    private static volatile boolean exitWithErrorCodeStarted = false;

    static {
        if (!AsyncBuilder.isInitialized()) {
            Exception e = new IllegalStateException(AsyncBuilder.NOT_INITIALIZED);
            Log.e(Async.class.getSimpleName(), AsyncBuilder.NOT_INITIALIZED, e);
        }
    }

    private static final AsyncBuilder ASYNC_BUILDER = AsyncBuilder.asyncBuilder; // The builder used to create the _first_ instance of ThreadType, the one which receives convenient static bindings of commonly used features
    public static final boolean DEBUG = (ASYNC_BUILDER == null) || ASYNC_BUILDER.debug; //BuildConfig.DEBUG; // true in debugOrigin builds, false in production builds, determined at build time to help JAVAC and PROGUARD clean out debugOrigin-only support code for speed and size
    //TODO Periodically check if recent Android updates have fixed this gradle bug, https://code.google.com/p/android/issues/detail?id=52962
    //TODO Manual gradle work-around, https://gist.github.com/almozavr/d59e770d2a6386061fcb
    //TODO Add a flag for independently enabling or disable runtime assertions and tests at Gradle level. Currently many optimistic assumptions that can make the error show up only later are made when DEBUG==false, but this aggressive optimization might need to be switched off independently to verify if the code path difference is the problem or help when the problem is speed sensitive
    //    public static final boolean VISUALIZE = false;
    public static final boolean TRACE_ASYNC_ORIGIN = (ASYNC_BUILDER == null) || ASYNC_BUILDER.showErrorStackTraces; // This makes finding where in you code a given log line was directly or indirectly called, but slows running
    // Some of the following logic lines are funky to support the Android visual editor. If you never initialized Async, you will want to see something in the visual editor. This matters for UI classes which receive services from Async
    public static final Thread UI_THREAD = (ASYNC_BUILDER == null) ? null : ASYNC_BUILDER.uiThread; // The main system thread for this Context
    public static final boolean FAIL_FAST = (ASYNC_BUILDER == null) || ASYNC_BUILDER.failFast; // Default true- stop on the first error in debugOrigin builds to make debugging from the first point of failure easier
    public static volatile boolean SHOW_ERROR_STACK_TRACES = (ASYNC_BUILDER == null) || ASYNC_BUILDER.showErrorStackTraces; // For clean unit testing. This can be temporarily turned off for a single threaded system or unit test code block to keep _intentional_ unit test errors from cluttering the stack trace.

    private static final int FAIL_FAST_SLEEP_BEFORE_SYSTEM_EXIT = 5000; // Only if FAIL_FAST is true. The idea is this helps the user and debugger see the issue and logs can catch up before bombing the app a bit too fast to see what was happening
    private static final ImmutableValue<String> DEFAULT_ORIGIN = new ImmutableValue<>("No origin provided in production builds");

    /**
     * The default {@link com.futurice.cascade.i.IThreadType} implementation. Usually you can call for
     * guaranteed asynchronous operations that will cooperate (queue) when all device cores are busy.
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

    Async() {
    }

    private static void exitWithErrorCode(String tag, String message, Throwable t) {
        final int errorCode = -Math.abs(t.hashCode());

        // Kill the app hard after some delay. You are not allowed to refire this Intent in some critical phases (Activity startup)
        //TODO let the Activity or Service down slowly and gently with lifecycle callbacks if production build
        if (exitWithErrorCodeStarted) {
            Log.v(tag, "Already existing, ignoring exit with error code (" + errorCode + "): " + message + "-" + t);
        } else {
            Log.e(tag, "Exit with error code (" + errorCode + "): " + message, t);
            exitWithErrorCodeStarted = true; // Not a thread-safe perfect lock, but fast and good enough to generally avoid duplicate shutdown messages during debug
            WORKER.shutdownNow("exitWithErrorCode: " + message, null, null, 0);
            NetThreadType.netReadThreadType.shutdownNow("exitWithErrorCode: " + message, null, null, 0);
            NetThreadType.netWriteThreadType.shutdownNow("exitWithErrorCode: " + message, null, null, 0);
            FILE_READ.shutdownNow("exitWithErrorCode: " + message, null, null, 0);
            FILE_WRITE.shutdownNow("exitWithErrorCode: " + message, null, null, 0);
            new Thread(() -> {
                try {
                    // Give the user time to see a popup split adb time to receive the error messages from this process before it dies
                    Thread.sleep(FAIL_FAST_SLEEP_BEFORE_SYSTEM_EXIT);
                } catch (Exception e2) {
                    Log.d(tag, "Problem while pausing before failfast system exit due to " + t, e2);
                }
                System.exit(errorCode);
            }
                    , "FailFastDelayThread")
                    .start();
        }
    }

    private static String tagWithAspectAndThreadName(String message) {
        if (!DEBUG || message.contains("at .")) {
            return message;
        }
        final IThreadType threadType = currentThreadType();

        if (threadType != null) {
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
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message
     * @param t
     * @return <code>false</code> always, for simple error chaining without consuming the error
     */
    public static boolean e(@NonNull Object tag, @NonNull String message, @NonNull Throwable t) {
        if (DEBUG && !SHOW_ERROR_STACK_TRACES) {
            Log.d(getTag(tag), tagWithAspectAndThreadName(message) + " : " + t);
        } else {
            Log.e(getTag(tag), tagWithAspectAndThreadName(message), t);
        }
        if (FAIL_FAST && !((t instanceof InterruptedException) || (t instanceof CancellationException))) {
            exitWithErrorCode(getTag(tag), message, t);
        }

        return false;
    }

    /**
     * Log an error. During debugOrigin builds, this will fail-fast end the current applicationContext
     * <p>
     * If you do not want fail fast during debugOrigin build, use the normal {@link android.util.Log} routines
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message
     * @return <code>false</code> always, for simple error chaining without consuming the error
     */
    @NotCallOrigin
    public static boolean e(@NonNull Object tag, @NonNull String message) {
        if (DEBUG && !SHOW_ERROR_STACK_TRACES) {
            d(getTag(tag), message + " !SHOW_ERROR_STACK_TRACES (Exception created to generate a stack trace)");
        } else {
            e(getTag(tag), message, new Exception("(Exception created to generate a stack trace)"));
        }

        return false;
    }

    /**
     * Log a verbose message including the thread name
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message
     */
    public static void v(@NonNull Object tag, @NonNull String message) {
        Log.v(getTag(tag), tagWithAspectAndThreadName(message));
    }

    /**
     * Log a debugOrigin message including the thread name
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message
     */
    public static void d(@NonNull Object tag, @NonNull String message) {
        Log.d(getTag(tag), tagWithAspectAndThreadName(message));
    }

    /**
     * Log an information message including the thread name
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message
     */
    public static void i(@NonNull Object tag, @NonNull String message) {
        Log.i(getTag(tag), tagWithAspectAndThreadName(message));
    }

    /**
     * Log at the debug level, including where in your code this line was called from.
     *
     * @param tag        a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param callOrigin where in your code the <code>Object </code> hosting this message was originally created. This is also included in the log line as a clickable link.
     * @param message    a message to display in the debug log
     */
    public static void dd(@NonNull Object tag, @NonNull ImmutableValue<String> callOrigin, @NonNull String message) {
        debugOriginThen(callOrigin, (objectCreationOrigin, ccOrigin) ->
                d(tag, combineOriginStringsRemoveDuplicates(objectCreationOrigin, ccOrigin, message)));
    }

    /**
     * Log at the debug level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message a message to display in the debug log
     */
    public static void dd(@NonNull Object tag, @NonNull String message) {
        debugOriginThen(origin ->
                d(tag, message + origin));
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
    public static void vv(@NonNull Object tag, @NonNull ImmutableValue<String> origin, @NonNull String message) {
        debugOriginThen(origin,
                (origin1, origin2) ->
                        v(tag, combineOriginStringsRemoveDuplicates(origin1, origin2, message)));
    }

    /**
     * Log at the verbose level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message a message to display in the verbose log
     */
    public static void vv(@NonNull Object tag, @NonNull String message) {
        debugOriginThen(origin ->
                v(tag, message + origin));
    }

    /**
     * Log at the error level, including where in your code this line was called from.
     *
     * @param tag               a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param currentCallOrigin Where in your code the <code>Object </code> hosting this message was originally created. This is also included in the log line as a clickable link.
     * @param message           a message to display in the error log
     * @param t                 the throwable which triggered this error
     * @return <code>false</code> always, for simple error chaining without consuming the error, see {@link IOnErrorAction}
     */
    public static boolean ee(@NonNull Object tag, @NonNull ImmutableValue<String> currentCallOrigin, @NonNull String message, @NonNull Throwable t) {
        if (DEBUG && !SHOW_ERROR_STACK_TRACES) {
            debugOriginThen(currentCallOrigin,
                    (objectCreationOrigin, ccOrigin) ->
                            d(tag, combineOriginStringsRemoveDuplicates(objectCreationOrigin, ccOrigin, message + " " + t)));
        } else {
            debugOriginThen(currentCallOrigin,
                    (objectCreationOrigin, ccOrigin) ->
                            e(tag, combineOriginStringsRemoveDuplicates(objectCreationOrigin, ccOrigin, message), t));
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
    public static boolean ee(@NonNull Object tag, @NonNull String message, @NonNull Throwable t) {
        if (DEBUG && !SHOW_ERROR_STACK_TRACES) {
            debugOriginThen(origin ->
                    d(tag, message + " " + t + origin));
        } else {
            debugOriginThen(origin ->
                    e(tag, message + origin, t));
        }

        return false;
    }

    /**
     * Log at the information level, including where in your code this line was called from.
     *
     * @param tag               a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param currentCallOrigin Where in your code the <code>Object </code> hosting this message was originally created. This is also included in the log line as a clickable link.
     * @param message           a message to display in the info log
     */
    public static void ii(@NonNull Object tag, @NonNull ImmutableValue<String> currentCallOrigin, @NonNull String message) {
        debugOriginThen(currentCallOrigin,
                (objectCreationOrigin, ccOrigin) ->
                        i(tag, combineOriginStringsRemoveDuplicates(objectCreationOrigin, ccOrigin, message)));
    }

    /**
     * Log at the information level, including where in your code this line was called from.
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message a message to display in the info log
     */
    public static void ii(@NonNull Object tag, @NonNull String message) {
        debugOriginThen(origin ->
                i(tag, message + origin));
    }

    private static String combineOriginStringsRemoveDuplicates(@NonNull String origin1, @NonNull String origin2, @NonNull String message) {
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
    private static String getTag(Object tag) {
        if (tag instanceof String) {
            return (String) tag;
        }
        if (tag instanceof INamed) {
            return tag.getClass().getSimpleName() + '-' + ((INamed) tag).getName();
        }

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

//    /**
//     * Shorten the name of an Object's toString() results if it is the name of a Lambda expression
//     * <p>
//     * This helps keeps the debugOrigin text logs lighter and more readable and clickable
//     * <p>
//     * TODO Is this obsolete with the other debugging features? Consider removing
//     *
//     * @param o
//     * @return
//     */
//    public static String lambdaToString(Object o) {
//        if (!DEBUG || o == null) {
//            return "";
//        }
//        String s = o.toString();
//        int i = s.indexOf("$$Lambda$");
//
//        if (i >= 0) {
//            return s.substring(i);
//        }
//
//        return s;
//    }

    public static void throwIllegalStateException(Object tag, String message) throws RuntimeException {
        throwRuntimeException(tag, message, new IllegalStateException(message));
    }

    public static void throwIllegalStateException(Object tag, ImmutableValue<String> origin, String message) throws RuntimeException {
        throwRuntimeException(tag, origin, message, new IllegalStateException(message));
    }

    public static void throwIllegalArgumentException(Object tag, String message) throws RuntimeException {
        throwRuntimeException(tag, message, new IllegalArgumentException(message));
    }

    public static void throwIllegalArgumentException(Object tag, ImmutableValue<String> origin, String message) throws RuntimeException {
        throwRuntimeException(tag, origin, message, new IllegalArgumentException(message));
    }

    /**
     * Create a detailed log with a {@link RuntimeException} thrown at the current code point
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message
     * @param t
     * @throws RuntimeException
     */
    public static void throwRuntimeException(Object tag, String message, Throwable t) throws RuntimeException {
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
     * @param message
     * @throws TimeoutException
     */
    public static void throwTimeoutException(Object tag, String message) throws TimeoutException {
        TimeoutException e = new TimeoutException(message);
        ee(tag, message, e);
        throw e;
    }

    /**
     * Create a detailed log with a {@link RuntimeException} thrown at the current code point
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param origin
     * @param message
     * @param t
     * @throws RuntimeException
     */
    public static void throwRuntimeException(Object tag, ImmutableValue<String> origin, String message, Throwable t) throws RuntimeException {
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
     * clear and clickable. Most importantly, it shows you not a vomit of call stack but only where the errant functional chain
     * started such as in your user or library code.
     * <p>
     * This information is frequently displayed along with the full stack trace at the point the error
     * manifests itself. First look at what when wrong at that point, subscribe work backwards to how you
     * created the mess. Yes, it is always your fault. Or my fault. Nah.
     *
     * @return a string holder that will be populated in the background on a WORKER thread (only in {@link #DEBUG} builds)
     */
    @NonNull
    public static ImmutableValue<String> originAsync() {
        if (!TRACE_ASYNC_ORIGIN) {
            return DEFAULT_ORIGIN;
        }

        final StackTraceElement[] traceElementsArray = Thread.currentThread().getStackTrace();
        final ImmutableValue<String> immutableValue = new ImmutableValue<>();

        if (WORKER != null) {
            WORKER.execute((Runnable) () ->
                            immutableValue.set(prettyFormat(origin(traceElementsArray).get(0).stackTraceElement))
            );
        } else {
            // During bootstrapping of the ThreadTypes
            final List<StackTaceLine> list = origin(traceElementsArray);
            immutableValue.set(prettyFormat(list.get(0).stackTraceElement));
        }

        return immutableValue;
    }

    /**
     * Extract from the current stack trace the most interesting "origin" line from which this was
     * called. Once this is done on a background thread, pass this short text to action
     *
     * @param action
     */
    private static void debugOriginThen(@NonNull final IActionOne<String> action) {
        if (TRACE_ASYNC_ORIGIN && WORKER != null) {
            originAsync().then(action);
        } else if (DEBUG) {
            try {
                action.call("");
            } catch (Exception e) {
                Log.e(Async.class.getSimpleName(), "Problem in debugOriginThen()", e);
            }
        }
    }

    /**
     * Perform an action once both the async-resolved object creation call stack origin and
     * async current call stack origin are settled
     *
     * @param objectCreationOrigin
     * @param action
     */
    private static void debugOriginThen(@NonNull final ImmutableValue<String> objectCreationOrigin, @NonNull final IActionTwo<String, String> action) {
        if (TRACE_ASYNC_ORIGIN) {
            final ImmutableValue<String> currentCallOrigin = originAsync();
            objectCreationOrigin.then(
                    creationOrigin -> {
                        currentCallOrigin.then(
                                callOrigin -> action.call(creationOrigin, callOrigin));
                    });
        } else if (DEBUG) {
            try {
                action.call("", "");
            } catch (Exception e) {
                Log.e(Async.class.getSimpleName(), "Problem in debugOriginThen()", e);
            }
        }
    }

    private static List<StackTaceLine> origin(final StackTraceElement[] traceElementsArray) {
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
//            previousList = filterListByMethod(previousList, method -> {
//                return method.isAnnotationPresent(CallOrigin.class);
//            });
//            previousList = filterListByClassAnnotation(previousList, CallOrigin.class, false);
//            previousList = filterListByClass(previousList, claz -> claz.getName().indexOf('$') >= 0); // Prefer lambdas and anonymous inner classes
            previousList = filterListByMethod(previousList, method -> !method.isAnnotationPresent(NotCallOrigin.class));
            previousList = filterListByClassAnnotation(previousList, NotCallOrigin.class, true);
        } catch (Exception e) {
            e(Async.class.getSimpleName(), "Problem filtering stack chain", e);
        }
        return previousList;
    }

    //TODO Cleanup and make this more generally applicable for other items with (new interface) ICancellationReason items
    public static String addOriginToReason(@NonNull final String reason, @NonNull final ImmutableValue<String> origin) {
        //TODO Make it delay if the reason is not yet available
        if (!(Async.TRACE_ASYNC_ORIGIN) || origin.isSet() || reason != null && reason.contains(".java")) {
            return reason; // A code path is already provided, or no new data available to add synchronously
        }

        return origin + " - " + reason; //TODO Not fully async. Not critical, but can sometimes race to a non-set origin
    }

    private static final class StackTaceLine {
        final Class claz;
        final ImmutableValue<Method> method;
        final StackTraceElement stackTraceElement;

        StackTaceLine(final StackTraceElement stackTraceElement) throws ClassNotFoundException {
            this.stackTraceElement = stackTraceElement;
            final String className = stackTraceElement.getClassName();
            Class c = classNameMap.get(className);
            if (c == null) {
                c = Class.forName(className);
                classNameMap.putIfAbsent(className, c);
            }
            this.claz = c;
            final String methodName = stackTraceElement.getMethodName();
            final String key = className + methodName;
            this.method = new ImmutableValue<>(
                    () -> {
                        Method meth = methodNameMap.get(key);

                        if (meth == null) {
                            final Method[] methods = claz.getMethods();
                            for (final Method m : methods) {
                                methodNameMap.putIfAbsent(key, m);
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

    private static List<StackTaceLine> findClassAndMethod(
            final List<StackTraceElement> stackTraceElementList) {
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

    private static List<StackTaceLine> filterListByClass
            (List<StackTaceLine> list, IActionOneR<Class, Boolean> classFilter) throws
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

    @SuppressWarnings("unchecked")
    private static List<StackTaceLine> filterListByClassAnnotation
            (List<StackTaceLine> list, Class<? extends Annotation> annotation,
             boolean mustBeAbsent) throws Exception {
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

    private static List<StackTaceLine> filterListByMethod
            (List<StackTaceLine> list, IActionOneR<Method, Boolean> methodFilter) throws
            Exception {
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

    private static List<StackTaceLine> filterListByPackage
            (List<StackTaceLine> list, IActionOneR<String, Boolean> packageFilter) throws
            Exception {
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

    private static String prettyFormat(@NonNull final StackTraceElement stackTraceElement) {
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
     *
     * @return the current ThreadType if it can be determined (no use of one Thread for multiple ThreadTypes), or null if it can not be deetermined
     */
    public static IThreadType currentThreadType() {
        if (!DEBUG) {
            return null;
        }

        Thread thread = Thread.currentThread();
        IThreadType threadType = null;

        if (thread instanceof TypedThread) {
            threadType = ((TypedThread) thread).getThreadType();
        } else {
            if (isUiThread()) {
                threadType = UI;
            }
        }

        return threadType;
    }

    public static boolean isUiThread() {
        return Thread.currentThread() == UI_THREAD;
    }

    public static boolean isWorkerThread() {
        final Thread thread = Thread.currentThread();
        //TODO Test. This may not be reliable if getAsect() is not reliable when the thread is part of multiple ThreadTypes
        return thread instanceof TypedThread && ((TypedThread) thread).getThreadType() == WORKER;
    }

    /**
     * A runtime assertTrue you may insert at the beginning of code to ensure your design does
     * not do code requiring the UI thread when not on the UI thread.
     * <p>
     * For performance, the assertTrue is not performed in production builds, only debugOrigin builds.
     */
    public static void assertUIThread() {
        if (DEBUG && !isUiThread()) {
            throwIllegalStateException(Async.class.getSimpleName(), "assertUIThread() but actually running on " + Thread.currentThread().getName());
        }
    }

    public static void assertWorkerThread() {
        if (DEBUG && !isWorkerThread()) {
            throwIllegalStateException(Async.class.getSimpleName(), "assertWorkerThread() but actually running on " + Thread.currentThread().getName());
        }
    }

    //TODO Replace all specific assertions with just assert(boolean) and assert(message, boolean) variants

    /**
     * In DEBUG builds only, check the condition specified. If that is not satisfied, abort the current
     * functional chain by throwing an {@link java.lang.IllegalStateException} with the explanation errorMessage provided.
     *
     * @param errorMessage
     * @param testResult
     */
    public static void assertTrue(String errorMessage, boolean testResult) {
        if (DEBUG && !testResult) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public static void assertNull(String errorMessage, Object o) {
        if (DEBUG && o != null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static <T> void assertEquals(String errorMessage, T t1, T t2) {
        if (DEBUG && !(!(t1 == null ^ t2 == null) && (t1 == null || t1.equals(t2)))) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static <T> void assertNotEquals(String errorMessage, T t1, T t2) {
        if (DEBUG && !(t1 == null ^ t2 == null) && (t1 == null || t1.equals(t2))) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static <IN, OUT> OUT call(IN in, IBaseAction<IN>
            action) throws Exception {
        if (action instanceof IAction) {
            ((IAction) action).call();
            return null;
        } else if (action instanceof IActionOne) {
            ((IActionOne<IN>) action).call(in);
            return null;
        } else if (action instanceof IActionOneR) {
            return ((IActionOneR<IN, OUT>) action).call(in);
        } else if (action instanceof IActionR) {
            return ((IActionR<IN, OUT>) action).call();
        }
        throw new UnsupportedOperationException("Not sure how to call this IBaseAction type: " + action.getClass());
    }

    /**
     * A runtime pre-condition for following logic that says it must be execute on one of the threads
     * created for an {@link com.futurice.cascade.i.IThreadType}, specifically a {@link TypedThread}
     * and not the system main thread ("UI thread").
     * <p>
     * Logically you may use this as assert-not-ui-thread. Creating threads which are not <code>ThreadTypeThread</code>s
     * would require you to provide a different assertion.
     */
    public static void assertTypedThread() {
        if (DEBUG && !(Thread.currentThread() instanceof TypedThread)) {
            throwIllegalStateException(Async.class.getSimpleName(), "assertTypedThread() but actually running on " + Thread.currentThread().getName());
        }
    }

    public static final IThreadType FILE_READ = ASYNC_BUILDER.getFileReadThreadType();
    public static final IThreadType FILE_WRITE = ASYNC_BUILDER.getFileWriteThreadType();

    public static final class NetThreadType {
        /**
         * {@link com.futurice.cascade.rest.NetRESTService} singleton tuned to the currently available bandwidth
         * <p>
         * Use this instead of the more general purpose WORKER methods if you want to limit the resource
         * contention of concurrent network operations for better average throughput
         */
        public static final IThreadType netReadThreadType = ASYNC_BUILDER.getNetReadThreadType();
        public static final IThreadType netWriteThreadType = ASYNC_BUILDER.getNetWriteThreadType();
        public static final RESTService netRESTService = ASYNC_BUILDER.getNetRESTService();
    }
}
