package com.reactivecascade.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.reactivecascade.Async;
import com.reactivecascade.i.NotCallOrigin;

/**
 * Runtime assertions to make contracts explicit and code self-documenting
 * <p>
 * These assertions have no impact on performance in a production build as they are disabled. You can configure
 * this in your {@link com.reactivecascade.AsyncBuilder}.
 */
public class AssertUtil {
    /**
     * In DEBUG builds only, check the assertion and possibly throw an {@link IllegalStateException}
     *
     * @param testResult the result of the test, <code>true</code> if the assertion condition is met
     */
    @NotCallOrigin
    public static void assertTrue(boolean testResult) {
        if (Async.RUNTIME_ASSERTIONS && !testResult) {
            throw new IllegalStateException("assertTrue failed");
        }
    }

    /**
     * In DEBUG builds only, check the assertion and possibly throw an {@link IllegalStateException}
     *
     * @param errorMessage a message to display when the assertion fails. It should indicate the
     *                     reason which was not true and, if possible, the likely corrective action
     * @param testResult   the result of the test, <code>true</code> if the assertion condition is met
     */
    @NotCallOrigin
    public static void assertTrue(@NonNull String errorMessage,
                                  boolean testResult) {
        if (Async.RUNTIME_ASSERTIONS && !testResult) {
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * In DEBUG builds only, test equality and possibly throw an {@link IllegalStateException}
     *
     * @param expected from
     * @param actual   from
     * @param <T>      expected type
     * @param <U>      actual type
     */
    @NotCallOrigin
    public static <T, U extends T> void assertEqual(@Nullable T expected,
                                                    @Nullable U actual) {
        if (Async.RUNTIME_ASSERTIONS) {
            assertEqual(expected, actual, "assertEqual failed: expected ´'" + expected + "' but was '" + actual + "'");
        }
    }

    /**
     * In DEBUG builds only, test equality and possibly throw an {@link IllegalStateException}
     *
     * @param expected from
     * @param actual   from
     * @param <T>      type
     */
    @NotCallOrigin
    public static <T, U extends T> void assertEqual(@Nullable T expected,
                                                    @Nullable U actual,
                                                    @NonNull String message) {
        if (Async.RUNTIME_ASSERTIONS
                && actual != expected
                && (expected != null && !expected.equals(actual))) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * In DEBUG builds only, test equality and possibly throw an {@link IllegalStateException}
     *
     * @param expected from
     * @param actual   from
     * @param <T>      expected type
     * @param <U>      actual type
     */
    @NotCallOrigin
    public static <T, U extends T> void assertNotEqual(@Nullable final T expected,
                                                       @Nullable final U actual) {
        if (Async.RUNTIME_ASSERTIONS) {
            assertNotNull("assertNotEqual failed: expected ´'" + expected + "' was equal to '" + actual + "'");
        }
    }

    /**
     * In DEBUG builds only, test equality and possibly throw an {@link IllegalStateException}
     *
     * @param expected from
     * @param actual   from
     * @param message  error message
     * @param <T>      expected type
     * @param <U>      actual type
     */
    @NotCallOrigin
    public static <T, U extends T> void assertNotEqual(@Nullable final T expected,
                                                       @Nullable final U actual,
                                                       @NonNull final String message) {
        if (Async.RUNTIME_ASSERTIONS
                && (actual == expected || (expected != null && expected.equals(actual)))) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * In debug and production builds, throw {@link NullPointerException} if the argument is null
     *
     * @param t   the argument
     * @param <T> the type
     * @return the from, guaranteed to be non-null and annotated at <code>@NonNull </code> for rapidly catching errors in the IDE
     */
    @NonNull
    @NotCallOrigin
    public static <T> T assertNotNull(@Nullable T t) {
        return assertNotNull(t, "assertNotNull failed");
    }

    /**
     * In debug and production builds, throw {@link NullPointerException} if the argument is null
     *
     * @param t   the argument
     * @param <T> the type
     * @return the from, guaranteed to be non-null and annotated at <code>@NonNull </code> for rapidly catching errors in the IDE
     */
    @NonNull
    @NotCallOrigin
    public static <T> T assertNotNull(@Nullable T t,
                                      @NonNull String message) {
        if (t == null) {
            throw new IllegalStateException(message);
        }

        return t;
    }
}
