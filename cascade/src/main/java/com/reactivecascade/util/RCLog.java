/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.util.Log;

import com.reactivecascade.Async;
import com.reactivecascade.BuildConfig;
import com.reactivecascade.functional.ImmutableValue;
import com.reactivecascade.i.CallOrigin;
import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IActionOneR;
import com.reactivecascade.i.IActionTwo;
import com.reactivecascade.i.IAsyncOrigin;
import com.reactivecascade.i.INamed;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.i.NotCallOrigin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Cascade logging utilities
 * <p>
 * Complexity is the observer having trouble understanding something because the relationship between
 * cause and effect are not clear. Errors in asynchronous systems often appear at a different place
 * and time than where they were caused.
 * <p>
 * This spatio-temporal complexity is reduced and your productivity
 * increased by logging with these routines. They provide a clickable link in the log back to the origin
 * point in your code where the object triggering the logged event was created and the point at which this
 * method were called.
 */
public class RCLog {
    /**
     * A null object indicating the origin is not available
     */
    public static final ImmutableValue<String> DEFAULT_ORIGIN = new ImmutableValue<>("No Origin provided in production builds");
    private static final ConcurrentHashMap<String, Class> sClassNameMap = new ConcurrentHashMap<>(); // "classname" -> Class. Used by DEBUG builds to more quickly trace mOrigin of a log message back into your code
    private static final ConcurrentHashMap<String, Method> sMethodNameMap = new ConcurrentHashMap<>(); // "classname-methodname" -> Method. Used by DEBUG builds to more quickly trace mOrigin of a log message back into your code

    @NonNull
    private static String tagWithAspectAndThreadName(@NonNull String message) {
        if (!BuildConfig.DEBUG || message.contains("at .")) {
            return message;
        }

        final IThreadType threadType = Async.currentThreadType();

        if (threadType != Async.NON_CASCADE_THREAD) {
            return "<" + threadType.getName() + "," + Thread.currentThread().getName() + "> " + message;
        }

        return "<" + Thread.currentThread().getName() + "> " + message;
    }

    /**
     * Log an error. During debugOrigin builds, this will fail-fast end the current context
     * <p>
     * If you do not want fail fast during debugOrigin build, use the normal {@link android.util.Log} routines
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     */
    @NotCallOrigin
    public static void e(@NonNull Object tag,
                         @NonNull String message) {
        if (BuildConfig.DEBUG) {
            e(tag, message, new Exception("(Exception created to generate a stack trace)"));
        }
    }

    /**
     * Log an error. If {@link Async#FAIL_FAST} during a {@link BuildConfig#DEBUG} all Exceptions except
     * {@link InterruptedException} or {@link CancellationException} will terminate the application.
     * This makes debugging more straightforward as you only see the original source
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
     *                {@link ImmutableValue <String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @param t       the {@link Throwable} which triggered this error message
     */
    public static void e(@NonNull Object tag,
                         @NonNull String message,
                         @NonNull Throwable t) {
        if (BuildConfig.DEBUG) {
            if (tag instanceof IAsyncOrigin) {
                e(tag, ((IAsyncOrigin) tag).getOrigin(), message, t);
            } else {
                log(tag, message, (ta, m) -> {
                    if (Async.SHOW_ERROR_STACK_TRACES) {
                        Log.e(ta, m, t);
                    } else {
                        Log.d(ta, m + " : " + t);
                    }
                    if (Async.FAIL_FAST && !((t instanceof InterruptedException) || (t instanceof CancellationException))) {
                        Async.exitWithErrorCode(getTag(ta), m, t);
                    }
                });
            }
        }
    }

    private static void e(@NonNull Object tag,
                          @NonNull ImmutableValue<String> origin,
                          @NonNull String message,
                          @NonNull Throwable t) {
        AssertUtil.assertTrue(BuildConfig.DEBUG);

        debugOriginThen(ccOrigin -> {
            origin.then(o -> {
                if (Async.SHOW_ERROR_STACK_TRACES) {
                    try {
                        Log.e(getTag(tag), tagWithAspectAndThreadName(message), t);
                    } catch (Exception e) {
                        Log.e("Async", "Problem with logging: " + tag + " : " + message, e);
                    }
                } else {
                    log(tag, combineOriginStringsRemoveDuplicates(o, ccOrigin, message + " " + t), Log::e);
                }
                if (Async.FAIL_FAST && !((t instanceof InterruptedException) || (t instanceof CancellationException))) {
                    Async.exitWithErrorCode(getTag(tag), message, t);
                }
            });
        });
    }

    /**
     * Log a verbose message including the thread name
     *
     * @param tag     a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     */
    public static void v(@NonNull Object tag,
                         @NonNull String message) {
        if (BuildConfig.DEBUG) {
            if (tag instanceof IAsyncOrigin) {
                v(tag, ((IAsyncOrigin) tag).getOrigin(), message);
            } else {
                log(tag, message, Log::v);
            }
        }
    }

    private static void v(@NonNull Object tag,
                          @NonNull ImmutableValue<String> origin,
                          @NonNull String message) {
        AssertUtil.assertTrue(BuildConfig.DEBUG);

        debugOriginThen(
                ccOrigin -> {
                    origin.then(
                            o -> {
                                log(tag, combineOriginStringsRemoveDuplicates(o, ccOrigin, message), Log::v);
                            });
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
    public static void d(@NonNull Object tag,
                         @NonNull String message) {
        if (BuildConfig.DEBUG) {
            if (tag instanceof IAsyncOrigin) {
                d(tag, ((IAsyncOrigin) tag).getOrigin(), message);
            } else {
                log(tag, message, Log::d);
            }
        }
    }

    private static void d(@NonNull Object tag,
                          @NonNull ImmutableValue<String> origin,
                          @NonNull String message) {
        AssertUtil.assertTrue(BuildConfig.DEBUG);

        debugOriginThen(
                ccOrigin -> {
                    origin.then(o -> {
                        log(tag, combineOriginStringsRemoveDuplicates(o, ccOrigin, message), Log::d);
                    });
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
    public static void i(@NonNull Object tag,
                         @NonNull String message) {
        if (BuildConfig.DEBUG) {
            if (tag instanceof IAsyncOrigin) {
                i(tag, ((IAsyncOrigin) tag).getOrigin(), message);
            } else {
                log(tag, message, Log::i);
            }
        }
    }

    private static void i(@NonNull Object tag,
                          @NonNull ImmutableValue<String> origin,
                          @NonNull String message) {
        AssertUtil.assertTrue(BuildConfig.DEBUG);

        debugOriginThen(
                ccOrigin -> {
                    origin.then(
                            o -> {
                                log(tag, combineOriginStringsRemoveDuplicates(o, ccOrigin, message), Log::i);
                            });
                });
    }

    @SuppressWarnings("unchecked")
    private static void log(@NonNull Object tag,
                            @NonNull String message,
                            @NonNull IActionTwo<String, String> action) {
        AssertUtil.assertTrue(BuildConfig.DEBUG);

        try {
            action.call(getTag(tag), tagWithAspectAndThreadName(message));
        } catch (Exception e) {
            Log.e("Async", "Problem with logging: " + tag + " : " + message, e);
        }
    }

    private static String combineOriginStringsRemoveDuplicates(@NonNull String origin1,
                                                               @NonNull String origin2,
                                                               @NonNull String message) {
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
    private static String getTag(@NonNull Object tag) {
        if (tag instanceof String) {
            return (String) tag;
        }
        if (tag instanceof INamed) {
            return tag.getClass().getSimpleName() + '-' + ((INamed) tag).getName();
        }
        if (tag instanceof Class) {
            return ((Class) tag).getSimpleName();
        }

        return tag.getClass().getSimpleName();
    }

    /**
     * Generate an easy-to-debug stop signal at this point in a debug build
     *
     * @param origin  a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @throws RuntimeException
     */
    public static void throwIllegalStateException(@NonNull IAsyncOrigin origin,
                                                  @NonNull String message) throws RuntimeException {
        throwRuntimeException(origin, message, new IllegalStateException(message));
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
    public static void throwIllegalStateException(@NonNull Object tag,
                                                  @NonNull ImmutableValue<String> origin,
                                                  @NonNull String message) throws RuntimeException {
        throwRuntimeException(tag, origin, message, new IllegalStateException(message));
    }

    /**
     * Generate an easy-to-debug stop signal at this point in a debug build
     *
     * @param origin  the object causing this exception to be thrown
     * @param message to display and help the developer resolve the issue
     * @throws RuntimeException
     */
    public static void throwIllegalArgumentException(@NonNull IAsyncOrigin origin,
                                                     @NonNull String message) throws RuntimeException {
        throwRuntimeException(origin, message, new IllegalArgumentException(message));
    }

    /**
     * Create a detailed log with a {@link RuntimeException} thrown at the current code point
     *
     * @param origin  a log line to aid with filtering such as the mOrigin from which the object throwing
     *                the exception was created. This may be a {@link String}, {@link INamed},
     *                {@link ImmutableValue<String>} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @param e       the exception  which triggered this new {@link RuntimeException}
     * @throws RuntimeException
     */
    public static void throwRuntimeException(@NonNull IAsyncOrigin origin,
                                             @NonNull String message,
                                             @NonNull Exception e) throws RuntimeException {
        RuntimeException runtimeException;

        if (e instanceof RuntimeException) {
            runtimeException = (RuntimeException) e;
        } else {
            runtimeException = new RuntimeException(message, e);
        }
        e(origin, message, e);
        throw runtimeException;
    }

    /**
     * Create a detailed log with a {@link TimeoutException} thrown at the current code point
     *
     * @param tag     a {@link String}, {@link INamed} or other {@link Object} used to categorize this log line
     * @param message to display and help the developer resolve the issue
     * @throws RuntimeException
     */
    public static void throwTimeoutException(@NonNull Object tag,
                                             @NonNull String message) throws RuntimeException {
        final TimeoutException e = new TimeoutException(message);
        e(tag, message, e);
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
     * @param e       the exception which triggered this new {@link RuntimeException}
     * @throws RuntimeException
     */
    public static void throwRuntimeException(@NonNull Object tag,
                                             @NonNull ImmutableValue<String> origin,
                                             @NonNull String message,
                                             @NonNull Exception e) throws RuntimeException {
        RuntimeException runtimeException;

        if (e instanceof RuntimeException) {
            runtimeException = (RuntimeException) e;
        } else {
            runtimeException = new RuntimeException(message, e);
        }
        e(tag, origin, message, e);
        throw runtimeException;
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
     * @return a string holder that will be populated in the background on a WORKER thread (only in <code>{@link BuildConfig#DEBUG}=true</code> builds)
     */
    @NonNull
    @CheckResult(suggest = "<local variable> mOrigin =")
    public static ImmutableValue<String> originAsync() {
        if (!Async.TRACE_ASYNC_ORIGIN) {
            return DEFAULT_ORIGIN;
        }

        StackTraceElement[] traceElementsArray = Thread.currentThread().getStackTrace();
        ImmutableValue<String> immutableValue = new ImmutableValue<>();

        if (Async.WORKER != null) {
            Async.WORKER.run(() -> {
                List<StackTraceLine> o = origin(traceElementsArray);
                AssertUtil.assertTrue(o.size() > 0);
                StackTraceLine line = o.get(0);
                String s = prettyFormat(line.stackTraceElement);
                immutableValue.set(s);
            });
        } else {
            // During bootstrapping of the ThreadTypes
            List<StackTraceLine> list = origin(traceElementsArray);
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
    @NotCallOrigin
    private static void debugOriginThen(@NonNull IActionOne<String> action) {
        try {
            if (Async.TRACE_ASYNC_ORIGIN && Async.WORKER != null) {
                originAsync().then(action);
            } else {
                action.call("");
            }
        } catch (Exception e) {
            Log.e(Async.class.getSimpleName(), "Problem in debugOriginThen()", e);
        }
    }

    @NonNull
    private static List<StackTraceLine> origin(@NonNull StackTraceElement[] traceElementsArray) {
        final List<StackTraceElement> allStackTraceElements = new ArrayList<>(traceElementsArray.length);

        allStackTraceElements.addAll(Arrays.asList(traceElementsArray).subList(3, traceElementsArray.length));

        // Remove uninteresting stack trace elements in least-interesting-removed-first order, but step back to the previous state if everything is removed by one of these filters
        List<StackTraceLine> previousList = findClassAndMethod(allStackTraceElements);

        try {
            previousList = filterListByClass(previousList, claz ->
                    claz != Async.class);
            previousList = filterListByClass(previousList, claz ->
                    claz != AbstractThreadType.class);
            previousList = filterListByPackage(previousList, packag ->
                    !packag.startsWith("dalvik"));
            previousList = filterListByPackage(previousList, packag ->
                    !packag.startsWith("java"));
            previousList = filterListByPackage(previousList, packag ->
                    !packag.startsWith("com.sun"));
            previousList = filterListByPackage(previousList, packag ->
                    !packag.startsWith("android"));
            previousList = filterListByPackage(previousList, packag ->
                    !packag.startsWith("com.android"));
            previousList = filterListPreferCallOriginMethodAnnotation(previousList);
            previousList = filterListByClassAnnotation(previousList, NotCallOrigin.class, true);
        } catch (Exception e) {
            e(Async.class.getSimpleName(), "Problem filtering stack chain", e);
        }
        return previousList;
    }

    @NonNull
    private static List<StackTraceLine> findClassAndMethod(@NonNull List<StackTraceElement> stackTraceElementList) {
        final List<StackTraceLine> lines = new ArrayList<>(stackTraceElementList.size());

        for (StackTraceElement ste : stackTraceElementList) {
            String s = ste.toString();

            if (!s.contains("Native Method") && !s.contains("Unknown Source")) {
                try {
                    lines.add(new StackTraceLine(ste));
                } catch (ClassNotFoundException e) {
                    e(Async.class.getSimpleName(), "Can not find method " + ste.getMethodName() + " when introspecting stack trace class: " + ste.getClassName(), e);
                }
            }
        }

        return lines;
    }

    @NonNull
    private static List<StackTraceLine> filterListByClass(@NonNull List<StackTraceLine> list,
                                                          @NonNull IActionOneR<Class, Boolean> classFilter) throws Exception {
        final List<StackTraceLine> filteredList = new ArrayList<>(list.size());

        for (final StackTraceLine line : list) {
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
    private static List<StackTraceLine> filterListByClassAnnotation(@NonNull List<StackTraceLine> list,
                                                                    @NonNull Class<? extends Annotation> annotation,
                                                                    boolean mustBeAbsent) throws Exception {
        List<StackTraceLine> filteredList = new ArrayList<>(list.size());

        for (final StackTraceLine line : list) {
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
    private static List<StackTraceLine> filterListPreferCallOriginMethodAnnotation(@NonNull List<StackTraceLine> list) throws Exception {
        final List<StackTraceLine> filteredList = new ArrayList<>(list.size());

        for (final StackTraceLine line : list) {
            if (line.isAnnotated) {
                filteredList.add(line);
            }
        }
        if (filteredList.size() > 0) {
            return filteredList;
        }

        return list;
    }

    @NonNull
    private static List<StackTraceLine> filterListByPackage(@NonNull List<StackTraceLine> list,
                                                            @NonNull IActionOneR<String, Boolean> packageFilter) throws Exception {
        List<StackTraceLine> filteredList = new ArrayList<>(list.size());

        for (final StackTraceLine line : list) {
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
    private static String prettyFormat(@NonNull StackTraceElement stackTraceElement) {
        String s = stackTraceElement.toString();
        int i = stackTraceElement.getClassName().length();

        return '\n' + s.substring(i);
    }


    private static final class StackTraceLine {
        Class<?> claz;
        StackTraceElement stackTraceElement;
        boolean isAnnotated;

        StackTraceLine(@NonNull StackTraceElement stackTraceElement) throws ClassNotFoundException {
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

            Method meth = sMethodNameMap.get(key);
            if (meth == null) {
                final Method[] methods = claz.getMethods();

                for (final Method m : methods) {
                    if (m.getName().equals(methodName)) {
                        sMethodNameMap.putIfAbsent(key, m);
                        meth = m;
                        break;
                    }
                }
            }
            this.isAnnotated = meth != null && meth.isAnnotationPresent(CallOrigin.class);
        }
    }
}
