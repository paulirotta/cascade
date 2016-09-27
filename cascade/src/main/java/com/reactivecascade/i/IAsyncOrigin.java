/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

import android.support.annotation.NonNull;

import com.reactivecascade.functional.ImmutableValue;

/**
 * An extendable convenience for debugging asynchronous multithreaded code.
 * <p>
 * During debug builds, a stack trace will be generated and parsed asynchronously at the time this
 * object is created
 */
public interface IAsyncOrigin {
    IAsyncOrigin ORIGIN_NOT_SET = new IAsyncOrigin() {
        private final ImmutableValue<String> originNotSet = new ImmutableValue<>("Please set the origin to make debugging easier");

        @NonNull
        @Override
        public ImmutableValue<String> getOrigin() {
            return originNotSet;
        }
    };

    /**
     * Asynchronous debugging has traditionally been rather difficult.
     * Temporal and spatial complexity increase when the
     * <em>cause->effect</em> relationship between <em>bug-origin->bug-discovery</em> increases.
     * The {@link IAsyncOrigin} and default implementation {@link com.reactivecascade.util.Origin}
     * significantly simplify debugging by showing where in user code a currently visible bug
     * was set in motion. The {@link IAsyncOrigin} can be passed as the first parameter to the
     * logging methods in {@link com.reactivecascade.util.RCLog} to automatically inject clickable
     * links back to the point of asychrononus creation creation and invocation. The resulting
     * logs focus debugging attention by providing contextual information without the visible clutter
     * of a full stack trace.
     * <p>
     * There are often
     * two "points of origin" of interest for debugging purposes because they set up the conditions
     * which led to the failure. These are the point in the appliation code
     * where an asynchronous lambda expression was launched, and the point
     * of creation of the parent object launching that lambda expression.
     * <p>
     * Return a reference to a string extracted from the stack trace at the point
     * an object was created. The string is such that it can be displayed in the
     * log as a clickable link to the nearest point in user code (if possible)
     * indicating where the object was created. Either implement this method or
     * use the {@link com.reactivecascade.util.Origin} convenience implementation.
     * <p>
     * If the string returned is pointing to a part of that call trace stack
     * which is not interesting for debugging purposes, then use {@link CallOrigin}
     * and {@link NotCallOrigin} annotations on your classes and methods prefer to be
     * listed or not listed as the origin. For example, utility library classes
     * may be usefully annotated as  {@link NotCallOrigin} to remove them.
     * <p>
     * The origin functionality is very expensive at runtime due to the large amount
     * of reflective code. This work is done in the {@link com.reactivecascade.Async#WORKER}
     * context on multiple threads, so most applications will not notice any
     * of this additional work load. Because of the way {@link ImmutableValue#get()}
     * works, you can safely get this value from any thread even if the relatively heavy
     * background stack parsing has not yet cought up to the origin point you are looking for.
     * <p>
     * By default, the origin functionality is enabled on debug builds and disabled
     * on release builds. This behaviour can be changed by calling {@link com.reactivecascade.AsyncBuilder#traceAsyncOrigin}
     * when initializing the Cascade library.
     *
     * @return a pointer to a string indicating a class and line number. This information
     * is historic but for debugging is of complimentary interest to the current call
     * stack trace
     */
    @NonNull
    ImmutableValue<String> getOrigin();
}
