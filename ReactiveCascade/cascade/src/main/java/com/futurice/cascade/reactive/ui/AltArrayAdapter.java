/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.reactive.ui;

import android.content.Context;
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

import com.futurice.cascade.Async;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.active.RunnableAltFuture;
import com.futurice.cascade.i.IAltFuture;
import com.futurice.cascade.i.IAsyncOrigin;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.util.CLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.futurice.cascade.Async.UI;

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
 *               .subscribe( ..dosomething ..)
 *               ; // Returned from completed synchronously, will wait for the UI thread to catch up if not called from the UI thread
 *     </code>
 * </pre>
 */
@NotCallOrigin
public class AltArrayAdapter<T> extends ArrayAdapter<T> implements IAsyncOrigin {
    private final ImmutableValue<String> mOrigin = CLog.originAsync();

    public AltArrayAdapter(@NonNull final Context context,
                           @LayoutRes final int resource) {
        super(context, resource, 0, new ArrayList<>());
    }

    public AltArrayAdapter(@NonNull final Context context,
                           @LayoutRes final int resource,
                           @IdRes final int textViewResourceId) {
        super(context, resource, textViewResourceId, new ArrayList<>());
    }

    public AltArrayAdapter(@NonNull final Context context,
                           @LayoutRes final int resource,
                           @NonNull final T[] objects) {
        super(context, resource, 0, Arrays.asList(objects));
    }

    public AltArrayAdapter(@NonNull final Context context,
                           @LayoutRes final int resource,
                           @IdRes final int textViewResourceId,
                           @NonNull final T[] objects) {
        super(context, resource, textViewResourceId, Arrays.asList(objects));
    }

    public AltArrayAdapter(@NonNull final Context context,
                           @LayoutRes final int resource,
                           @NonNull final List<T> objects) {
        super(context, resource, 0, objects);
    }

    public AltArrayAdapter(@NonNull final Context context,
                           @LayoutRes final int resource,
                           @IdRes final int textViewResourceId,
                           @NonNull final List<T> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @NonNull
    public static AltArrayAdapter<CharSequence> createFromResource(
            @NonNull final Context context,
            @LayoutRes final int textArrayResId,
            @IdRes final int textViewResId) {
        final CharSequence[] strings = context.getResources().getTextArray(textArrayResId);

        return new AltArrayAdapter<>(context, textViewResId, strings);
    }

    @CallSuper
    @Override
    @UiThread
    public void add(@NonNull final T value) {
        super.add(value);
        CLog.v(mOrigin, "Add to AltArrayAdapter: " + value);

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
    public IAltFuture<?, T> addAsync(@NonNull final T value,
                                     final boolean ifAbsent) {
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
    public void remove(@NonNull final T value) {
        super.remove(value);

        CLog.v(mOrigin, "Remove from AltArrayAdapter: " + value);

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
    public IAltFuture<?, T> removeAsync(@NonNull final T object) {
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
    public <A> IAltFuture<A, A> sortAsync(@NonNull final Comparator<? super T> comparator) {
        CLog.v(mOrigin, "Sort AltArrayAdapter: " + comparator);

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
    public <A, TT extends T> IAltFuture<A, A> addAllAsync(@NonNull final Collection<TT> collection,
                                                          final boolean addIfUnique) {
        CLog.v(mOrigin, "Add all async to AltArrayAdapter: addCount=" + collection.size());
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
    public final <TT extends T> IAltFuture<?, List<TT>> addAllAsync(@NonNull final TT... items) {
        final ArrayList<TT> list = new ArrayList<>(items.length);

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
        final int n = getCount();
        final List<T> list = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            list.add(getItem(i));
        }

        return list;
    }

    @CallSuper
    @NonNull
    public T getItem(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position) {
        return super.getItem(position);
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, T> getItemAsync(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position) {
        return new RunnableAltFuture<>(UI, () ->
                getItem(position));
    }

    @CallSuper
    @IntRange(from = -1, to = Integer.MAX_VALUE)
    public int getPosition(@NonNull final T item) {
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
    public IAltFuture<?, Integer> getPositionAsync(@NonNull final T item) {
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
    public IAltFuture<?, Long> getItemIdAsync(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position) {
        return new RunnableAltFuture<>(UI, () ->
                getItemId(position));
    }

    @CallSuper
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    @Override
    public void setDropDownViewResource(@LayoutRes final int resource) {
        super.setDropDownViewResource(resource);
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public <A> IAltFuture<A, A> setDropDownViewResourceAsync(@LayoutRes final int resource) {
        return new RunnableAltFuture<>(UI, () ->
                setDropDownViewResource(resource));
    }

    @CallSuper
    @NonNull
    @Override
    public View getView(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position,
                        @Nullable final View convertView,
                        @NonNull final ViewGroup parent) {
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
    public <A> IAltFuture<A, View> getViewAsync(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position,
                                                @NonNull final View convertView,
                                                @NonNull final ViewGroup parent) {
        return new RunnableAltFuture<>(UI, () ->
                getView(position, convertView, parent));
    }

    @CallSuper
    @NonNull
    @Override
    public View getDropDownView(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position,
                                @NonNull final View convertView,
                                @NonNull final ViewGroup parent) {
        return super.getDropDownView(position, convertView, parent);
    }

    @CallSuper
    @NonNull
    @CheckResult(suggest = IAltFuture.CHECK_RESULT_SUGGESTION)
    public IAltFuture<?, View> getDropDownViewAsync(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position,
                                                    @NonNull final View convertView,
                                                    @NonNull final ViewGroup parent) {
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
