/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.reactive;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.IActionOneR;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.util.AssertUtil;
import com.futurice.cascade.util.RCLog;

import java.util.Locale;

/**
 * A {@link String} which can be updated with various string operations in an atomic, thread-safe manner.
 * <p>
 * This is similar to an {@link java.util.concurrent.atomic.AtomicInteger} with reactive bindings to
 * get and set the from in reactive chains (function sequences that can fire multiple times).
 * <p>
 * Created by phou on 30-04-2015.
 */
public class ReactiveString extends ReactiveValue<String> {
    /**
     * Create a new atomic integer
     *
     * @param name
     * @param initialValue
     */
    public ReactiveString(
            @NonNull final String name,
            @Nullable final String initialValue) {
        super(name, initialValue);
    }

    /**
     * Create a new atomic integer
     *
     * @param threadType
     * @param name
     * @param initialValue
     * @param onFireAction a mapping for incoming values, for example <code>i -> Math.max(0, i)</code>
     * @param onError action
     */
    public ReactiveString(
            @NonNull final IThreadType threadType,
            @NonNull final String name,
            @Nullable final String initialValue,
            @Nullable final IActionOneR<String, String> onFireAction,
            @NonNull final IActionOne<Exception> onError) {
        super(name, initialValue, threadType, onFireAction, onError);
    }

    /**
     * Mutates this string by concatenating a string to the end of the current from in a thread-safe manner
     *
     * @param string to be concatenated
     * @return the concatenated result
     */
    @CallSuper
    public String concat(@NonNull final String string) {
        while (true) {
            final String currentValue = get();
            AssertUtil.assertNotNull("String from not yet asserted", currentValue);
            final String concat = currentValue.concat(string);

            if (compareAndSet(currentValue, concat)) {
                return concat;
            }
            RCLog.d(this, "Collision in concurrent concat(" + string + "), will try again: " + currentValue);
        }
    }

    /**
     * Mutates this string after replacing occurrences of the given {@code char} with another.
     *
     * @param oldChar to be replaced
     * @param newChar replacement from
     * @return the replaced mutated from
     */
    @CallSuper
    public String replace(final char oldChar,
                          final char newChar) {
        while (true) {
            final String currentValue = get();
            AssertUtil.assertNotNull("String from not yet asserted", currentValue);
            final String replace = currentValue.replace(oldChar, newChar);

            if (compareAndSet(currentValue, replace)) {
                return replace;
            }
            RCLog.d(this, "Collision in concurrent replace(" + oldChar + ", " + newChar + "), will try again: " + currentValue);
        }
    }

    /**
     * Mutates this string after replacing occurrences of the given {@link CharSequence} with another.
     *
     * @param oldChars to be replaced
     * @param newChars replacement from
     * @return the replaced mutated from
     */
    @CallSuper
    public String replace(final CharSequence oldChars,
                          final CharSequence newChars) {
        while (true) {
            final String currentValue = get();
            AssertUtil.assertNotNull("String from not yet asserted", currentValue);
            final String replace = currentValue.replace(oldChars, newChars);

            if (compareAndSet(currentValue, replace)) {
                return replace;
            }
            RCLog.d(this, "Collision in concurrent replace(" + oldChars + ", " + newChars + "), will try again: " + currentValue);
        }
    }

    /**
     * Mutates this string to lower case, using the rules of {@code locale}.
     *
     * @param locale for case conversion
     * @return the lower case mutated state
     */
    @CallSuper
    public String toLowerCase(@NonNull final Locale locale) {
        while (true) {
            final String currentValue = get();
            AssertUtil.assertNotNull("String from not yet asserted", currentValue);
            final String lowerCase = currentValue.toLowerCase(locale);

            if (compareAndSet(currentValue, lowerCase)) {
                return lowerCase;
            }
            RCLog.d(this, "Collision in concurrent toLowerCase(" + locale + "), will try again: " + currentValue);
        }
    }
}
