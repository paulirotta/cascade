/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.reactive.ui;

import android.content.Context;
import android.support.annotation.ArrayRes;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.reactivecascade.functional.ImmutableValue;
import com.reactivecascade.functional.RunnableAltFuture;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IAsyncOrigin;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.RCLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.reactivecascade.Async.UI;

/**
 * Manipulate an {@link android.widget.ArrayAdapter} safely from any thread
 * <p>
 * If a method is thread safe, it in __Async alternative is not provided and you should use the default
 * one. If an __Async version is provided here, use that from your single-threaded or multi-threaded model code.
 * <p>
 * When you add, remove etc the actions are completed in serial call order on the UI thread
 * <p>
 * Each method call will complete synchronously if called from the UI thread.
 * <p>
 * Each  method call will return immediately and complete asynchronously if there is no return from.
 * The exception handling in these cases is asynchronous, a message to the system log.
 * <p>
 * Each method call will complete synchronously if there is a return from and it is not called
 * from the UI thread. If there is an exception such as {@link java.util.concurrent.TimeoutException}
 * because the UI thread is not responsive within the time limit specified for this <code>ConcurrentListAdapter</code>,
 * a {@link java.lang.RuntimeException} will be thrown.
 * <p>
 * Example:
 * <pre>
 *     <code>
 *         myConcurrentListAdapter.addAsync(from)
 *               ; // Return immediately, completed asynchronously on UI thread or synchronously if called from UI thread
 *         myConcurrentListAdapter.getItemAsync(0)
 *               .sub( ..dosomething ..)
 *               ; // Returned from completed synchronously, will wait for the UI thread to catch up if not called from the UI thread
 *     </code>
 * </pre>
 */
@NotCallOrigin
public class AltArrayAdapter<T> extends ArrayAdapter<T> implements IAsyncOrigin {
    private final ImmutableValue<String> mOrigin = RCLog.originAsync();

    public AltArrayAdapter(@NonNull Context context,
                           @LayoutRes int resource) {
        super(context, resource, 0, new ArrayList<>());
    }

    public AltArrayAdapter(@NonNull Context context,
                           @LayoutRes int resource,
                           @IdRes int textViewResourceId) {
        super(context, resource, textViewResourceId, new ArrayList<>());
    }

    public AltArrayAdapter(@NonNull Context context,
                           @LayoutRes int resource,
                           @NonNull T[] objects) {
        super(context, resource, 0, Arrays.asList(objects));
    }

    public AltArrayAdapter(@NonNull Context context,
                           @LayoutRes int resource,
                           @IdRes int textViewResourceId,
                           @NonNull T[] objects) {
        super(context, resource, textViewResourceId, Arrays.asList(objects));
    }

    public AltArrayAdapter(@NonNull Context context,
                           @LayoutRes int resource,
                           @NonNull List<T> objects) {
        super(context, resource, 0, objects);
    }

    public AltArrayAdapter(@NonNull Context context,
                           @LayoutRes int resource,
                           @IdRes int textViewResourceId,
                           @NonNull List<T> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @NonNull
    public static AltArrayAdapter<CharSequence> createFromResource(
            @NonNull Context context,
            @ArrayRes int textArrayResId,
            @LayoutRes int textViewResId) {
        CharSequence[] strings = context.getResources().getTextArray(textArrayResId);

        return new AltArrayAdapter<>(context, textViewResId, strings);
    }

    @CallSuper
    @Override
    @UiThread
    public void add(@NonNull T value) {
        super.add(value);

        RCLog.v(mOrigin, "Add to AltArrayAdapter: " + value);
    }

    /**
     * Add the from to the end of the list
     *
     * @param value
     * @return
     */
    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, T> addAsync(@NonNull T value,
                                     boolean ifAbsent) {
        return UI.then(() -> {
            if (ifAbsent) {
                remove(value);
            }
            add(value);

            return value;
        });
    }

    @CallSuper
    @NotCallOrigin
    public void remove(@NonNull T value) {
        super.remove(value);

        RCLog.v(mOrigin, "Remove from AltArrayAdapter: " + value);

    }

    /**
     * Remove the item from the list
     *
     * @param object
     * @return
     */
    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, T> removeAsync(@NonNull T object) {
        return new RunnableAltFuture<>(UI, () -> {
            remove(object);

            return object;
        });
    }

    /**
     * Sort the display list
     *
     * @param comparator
     * @param <A>
     * @return
     */
    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <A> IAltFuture<A, A> sortAsync(@NonNull Comparator<? super T> comparator) {
        RCLog.v(mOrigin, "Sort AltArrayAdapter: " + comparator);

        return new RunnableAltFuture<>(UI, () ->
                sort(comparator));
    }

    /**
     * Although the underlying call is thread safe to call directly, we always want this to
     * run after any pending changes on the UI thread.
     *
     * @param <A> the upstream output from type
     * @return the output from from upstream
     */
    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <A> IAltFuture<A, A> notifyDataSetChangedAsync() {
        return new RunnableAltFuture<>(UI,
                this::notifyDataSetChanged);
    }

    /**
     * Although the underlying call is thread safe to call directly, we always want this to
     * run after any pending changes on the UI thread.
     *
     * @param <A> the upstream output from type
     * @return the output from from upstream
     */
    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <A> IAltFuture<A, A> notifyDataSetInvalidatedAsync() {
        return new RunnableAltFuture<>(UI,
                this::notifyDataSetInvalidated);
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <A> IAltFuture<A, A> setNotifyOnChangeAsync(final boolean notifyOnChange) {
        return new RunnableAltFuture<>(UI, () ->
                setNotifyOnChange(notifyOnChange));
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <A, TT extends T> IAltFuture<A, A> addAllAsync(@NonNull Collection<TT> collection,
                                                          boolean addIfUnique) {
        RCLog.v(mOrigin, "Add all async to AltArrayAdapter: addCount=" + collection.size());
        if (addIfUnique) {
            return new RunnableAltFuture<>(UI, () -> {
                for (final TT t : collection) {
                    remove(t);
                    add(t);
                }
            });
        }

        return new RunnableAltFuture<>(UI, () ->
                addAll(collection));
    }

    @SafeVarargs
    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public final <TT extends T> IAltFuture<?, List<TT>> addAllAsync(@NonNull TT... items) {
        ArrayList<TT> list = new ArrayList<>(items.length);

        for (final TT item : items) {
            add(item);
        }

        return addAllAsync(list, false);
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <A> IAltFuture<A, A> clearAsync() {
        return new RunnableAltFuture<>(UI,
                this::clear);
    }

    @CallSuper
    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public int getCount() {
        return super.getCount();
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, Integer> getCountAsync() {
        return new RunnableAltFuture<>(UI,
                this::getCount);
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, List<T>> getAllAsync() {
        return new RunnableAltFuture<>(UI,
                this::getAll);
    }

    @NonNull
    @UiThread
    public List<T> getAll() {
        int n = getCount();
        List<T> list = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            list.add(getItem(i));
        }

        return list;
    }

    @CallSuper
    @NonNull
    public T getItem(@IntRange(from = 0, to = Integer.MAX_VALUE) int position) {
        return super.getItem(position);
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, T> getItemAsync(@IntRange(from = 0, to = Integer.MAX_VALUE) int position) {
        return new RunnableAltFuture<>(UI, () ->
                getItem(position));
    }

    @CallSuper
    @IntRange(from = -1, to = Integer.MAX_VALUE)
    public int getPosition(@NonNull T item) {
        return super.getPosition(item);
    }

    /**
     * Warning: inherently not thread safe. There may be changes to the model which render the
     * returned index obsolete before you get a chance to use it.
     *
     * @param item the object to be found in the list
     * @return the position where the item was found, or -1 if not found
     */
    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, Integer> getPositionAsync(@NonNull T item) {
        return new RunnableAltFuture<>(UI, () ->
                getPosition(item));
    }

    @CallSuper
    @NonNull
    public Filter getFilter() {
        return super.getFilter();
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, Filter> getFilterAsync() {
        return new RunnableAltFuture<>(UI,
                this::getFilter);
    }

    @CallSuper
    public long getItemId(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position) {
        return super.getItemId(position);
    }

    /**
     * Warning: inherently not thread safe. There may be changes to the model which render the
     * returned index obsolete before you get a chance to use it.
     *
     * @param position
     * @return
     */
    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, Long> getItemIdAsync(@IntRange(from = 0, to = Integer.MAX_VALUE) int position) {
        return new RunnableAltFuture<>(UI, () ->
                getItemId(position));
    }

    @CallSuper
    @Override
    public void setDropDownViewResource(@LayoutRes int resource) {
        super.setDropDownViewResource(resource);
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <A> IAltFuture<A, A> setDropDownViewResourceAsync(@LayoutRes int resource) {
        return new RunnableAltFuture<>(UI, () ->
                setDropDownViewResource(resource));
    }

    @CallSuper
    @NonNull
    @Override
    public View getView(@IntRange(from = 0, to = Integer.MAX_VALUE) int position,
                        @Nullable View convertView,
                        @NonNull ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }

    /**
     * Warning: inherently not thread safe. There may be changes to the model which render the
     * returned index obsolete before you get a chance to use it.
     *
     * @param position
     * @param convertView
     * @param parent
     * @param <A>
     * @return
     */
    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <A> IAltFuture<A, View> getViewAsync(@IntRange(from = 0, to = Integer.MAX_VALUE) int position,
                                                @NonNull View convertView,
                                                @NonNull ViewGroup parent) {
        return new RunnableAltFuture<>(UI, () ->
                getView(position, convertView, parent));
    }

    @CallSuper
    @NonNull
    @Override
    public View getDropDownView(@IntRange(from = 0, to = Integer.MAX_VALUE) int position,
                                @NonNull View convertView,
                                @NonNull ViewGroup parent) {
        return super.getDropDownView(position, convertView, parent);
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, View> getDropDownViewAsync(@IntRange(from = 0, to = Integer.MAX_VALUE) int position,
                                                    @NonNull View convertView,
                                                    @NonNull ViewGroup parent) {
        return new RunnableAltFuture<>(UI, () ->
                getDropDownView(position, convertView, parent));
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, Boolean> isEmptyAsync() {
        return new RunnableAltFuture<>(UI,
                this::isEmpty);
    }

    @NonNull
    @Override
    public ImmutableValue<String> getOrigin() {
        return mOrigin;
    }
}
