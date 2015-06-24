package com.futurice.cascade.reactive.ui;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.futurice.cascade.Async.*;

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
 * Each  method call will return immediately and complete asynchronously if there is no return value.
 * The exception handling in these cases is asynchronous, a message to the system log.
 * <p>
 * Each method call will complete synchronously if there is a return value and it is not called
 * from the UI thread. If there is an exception such as {@link java.util.concurrent.TimeoutException}
 * because the UI thread is not responsive within the time limit specified for this <code>ConcurrentListAdapter</code>,
 * a {@link java.lang.RuntimeException} will be thrown.
 * <p>
 * Example:
 * <pre>
 *     <code>
 *         myConcurrentListAdapter.addAsync(value)
 *               .fork(); // Return immediately, completed asynchronously on UI thread or synchronously if called from UI thread
 *         myConcurrentListAdapter.getItemAsync(0)
 *               .subscribe( ..dosomething ..)
 *               .fork(); // Returned value completed synchronously, will wait for the UI thread to catch up if not called from the UI thread
 *     </code>
 * </pre>
 */
public class AltArrayAdapter<T> extends ArrayAdapter<T> {
    protected final ImmutableValue<String> mOrigin;

    //TODO Add full coverage and remove trivial implementations that are thread safe
    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource) {
        super(context, resource, 0, new ArrayList<>());
        mOrigin = originAsync();
    }

    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource,
            @LayoutRes final int textViewResourceId) {
        super(context, resource, textViewResourceId, new ArrayList<>());
        mOrigin = originAsync();
    }

    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource,
            @NonNull @nonnull final T[] objects) {
        super(context, resource, 0, Arrays.asList(objects));
        mOrigin = originAsync();
    }

    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource,
            @LayoutRes final int textViewResourceId,
            @NonNull @nonnull final T[] objects) {
        super(context, resource, textViewResourceId, Arrays.asList(objects));
        mOrigin = originAsync();
    }

    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource,
            @NonNull @nonnull final List<T> objects) {
        super(context, resource, 0, objects);
        mOrigin = originAsync();
    }

    //FIXME Is this really both @LayoutRes ?
    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource,
            @LayoutRes final int textViewResourceId,
            @NonNull @nonnull final List<T> objects) {
        super(context, resource, textViewResourceId, objects);
        mOrigin = originAsync();
    }

    @NonNull
    @nonnull
    public static AltArrayAdapter<CharSequence> createFromResource(
            @NonNull @nonnull final Context context,
            @LayoutRes final int textArrayResId,
            @LayoutRes final int textViewResId) {
        final CharSequence[] strings = context.getResources().getTextArray(textArrayResId);

        return new AltArrayAdapter<>(context, textViewResId, strings);
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @Override
    public void add(@NonNull @nonnull final T value) {
        super.add(value);
    }

    /**
     * Add the value to the end of the list
     *
     * @param value
     * @return
     */
    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, T> addAsync(@NonNull @nonnull final T value, final boolean ifAbsent) {
        return UI.then(
                () -> {
                    if (ifAbsent) {
                        remove(value);
                    }
                    add(value);
                    return value;
                });
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    public void remove(@NonNull @nonnull final T object) {
        super.remove(object);
    }

    /**
     * Remove the item from the list
     *
     * @param object
     * @return
     */
    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, T> removeAsync(@NonNull @nonnull final T object) {
        return UI.then(() -> {
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
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <A> IAltFuture<A, A> sortAsync(@NonNull @nonnull final Comparator<? super T> comparator) {
        return UI.then(() -> sort(comparator));
    }

    /**
     * Although the underlying call is thread safe to call directly, we always want this to
     * run after any pending changes on the UI thread.
     *
     * @param <A> the upstream output value type
     * @return the output value from upstream
     */
    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <A> IAltFuture<A, A> notifyDataSetChangedAsync() {
        return UI.then(this::notifyDataSetChanged);
    }

    /**
     * Although the underlying call is thread safe to call directly, we always want this to
     * run after any pending changes on the UI thread.
     *
     * @param <A> the upstream output value type
     * @return the output value from upstream
     */
    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <A> IAltFuture<A, A> notifyDataSetInvalidatedAsync() {
        return UI.then(this::notifyDataSetInvalidated);
    }

    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <A> IAltFuture<A, A> setNotifyOnChangeAsync(final boolean notifyOnChange) {
        return UI.then(() -> setNotifyOnChange(notifyOnChange));
    }

    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <A, TT extends T> IAltFuture<A, A> addAllAsync(@NonNull @nonnull final Collection<TT> collection, final boolean addIfUnique) {
        if (addIfUnique) {
            return UI.then(() -> {
                for (TT t : collection) {
                    remove(t);
                    add(t);
                }
            });
        }
        return UI.then(() -> addAll(collection));
    }

    @SafeVarargs
    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public final <TT extends T> IAltFuture<?, List<TT>> addAllAsync(@NonNull @nonnull final TT... items) {
        final ArrayList<TT> list = new ArrayList<>(items.length);
        for (TT item : items) {
            add(item);
        }
        return addAllAsync(list, false);
    }

    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <A> IAltFuture<A, A> clearAsync() {
        return UI.then(this::clear);
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public int getCount() {
        return super.getCount();
    }

    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Integer> getCountAsync() {
        return UI.then(this::getCount);
    }

    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, List<T>> getAllAsync() {
        return UI.then(() -> {
            final int n = getCount();
            final List<T> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(getItem(i));
            }

            return list;
        });
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @NonNull
    @nonnull
    public T getItem(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position) {
        return super.getItem(position);
    }

    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, T> getItemAsync(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position) {
        return UI.then(() -> getItem(position));
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @IntRange(from = -1, to = Integer.MAX_VALUE)
    public int getPosition(@NonNull @nonnull final T item) {
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
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Integer> getPositionAsync(@NonNull @nonnull final T item) {
        return UI.then(() -> getPosition(item));
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @NonNull
    @nonnull
    public Filter getFilter() {
        return super.getFilter();
    }

    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Filter> getFilterAsync() {
        return UI.then(this::getFilter);
    }

    /**
     * {@inheritDoc}
     */
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
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Long> getItemIdAsync(@IntRange(from = 0, to = Integer.MAX_VALUE) final int position) {
        return UI.then(() -> getItemId(position));
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @CheckResult(suggest = "IAltFuture#fork()")
    @Override
    public void setDropDownViewResource(@LayoutRes final int resource) {
        super.setDropDownViewResource(resource);
    }

    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <A> IAltFuture<A, A> setDropDownViewResourceAsync(@LayoutRes final int resource) {
        return UI.then(() -> setDropDownViewResource(resource));
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @NonNull
    @nonnull
    @Override
    public View getView(
            @IntRange(from = 0, to = Integer.MAX_VALUE) final int position,
            @NonNull @nonnull final View convertView,
            @NonNull @nonnull final ViewGroup parent) {
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
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public <A> IAltFuture<A, View> getViewAsync(
            @IntRange(from = 0, to = Integer.MAX_VALUE) final int position,
            @NonNull @nonnull final View convertView,
            @NonNull @nonnull final ViewGroup parent) {
        return UI.then(() -> getView(position, convertView, parent));
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @NonNull
    @nonnull
    @Override
    public View getDropDownView(
            @IntRange(from = 0, to = Integer.MAX_VALUE) final int position,
            @NonNull @nonnull final View convertView,
            @NonNull @nonnull final ViewGroup parent) {
        return super.getDropDownView(position, convertView, parent);
    }

    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, View> getDropDownViewAsync(
            @IntRange(from = 0, to = Integer.MAX_VALUE) final int position,
            @NonNull @nonnull final View convertView,
            @NonNull @nonnull final ViewGroup parent) {
        return UI.then(() -> getDropDownView(position, convertView, parent));
    }

    @CallSuper
    @NonNull
    @nonnull
    @CheckResult(suggest = "IAltFuture#fork()")
    public IAltFuture<?, Boolean> isEmptyAsync() {
        return UI.then(this::isEmpty);
    }
}
