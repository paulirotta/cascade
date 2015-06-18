package com.futurice.cascade.reactive.ui;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.futurice.cascade.i.active.IAltFuture;
import com.futurice.cascade.util.nonnull;

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
    //TODO Add full coverage and remove trivial implementations that are thread safe
    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource) {
        super(context, resource, 0, new ArrayList<>());
    }

    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource,
            @LayoutRes final int textViewResourceId) {
        super(context, resource, textViewResourceId, new ArrayList<>());
    }

    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource,
            @NonNull @nonnull final T[] objects) {
        super(context, resource, 0, Arrays.asList(objects));
    }

    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource,
            @LayoutRes final int textViewResourceId,
            @NonNull @nonnull final T[] objects) {
        super(context, resource, textViewResourceId, Arrays.asList(objects));
    }

    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource,
            @NonNull @nonnull final List<T> objects) {
        super(context, resource, 0, objects);
    }

    //FIXME Is this really both @LayoutRes ?
    public AltArrayAdapter(
            @NonNull @nonnull final Context context,
            @LayoutRes final int resource,
            @LayoutRes final int textViewResourceId,
            @NonNull @nonnull final List<T> objects) {
        super(context, resource, textViewResourceId, objects);
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
     * Add the value to the end of the list
     *
     * @param value
     * @return
     */
    @CallSuper
    @NonNull
    @nonnull
    public IAltFuture<?, T> addAsync(@NonNull @nonnull final T value) {
        return UI.then(
                () -> {
                    add(value);
                    return value;
                });
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
    public <A> IAltFuture<A, A> notifyDataSetInvalidatedAsync() {
        return UI.then(this::notifyDataSetInvalidated);
    }

    @CallSuper
    @NonNull
    @nonnull
    public <A> IAltFuture<A, A> setNotifyOnChangeAsync(final boolean notifyOnChange) {
        return UI.then(() -> setNotifyOnChange(notifyOnChange));
    }

    @CallSuper
    @NonNull
    @nonnull
    public <A> IAltFuture<A, A> addAllAsync(@NonNull @nonnull final Collection<T> collection) {
        return UI.then(() -> addAll(collection));
    }

    @SafeVarargs
    @CallSuper
    @NonNull
    @nonnull
    public final IAltFuture<?, T[]> addAllAsync(@NonNull @nonnull final T... items) {
        //TODO Not an atomic add operation
        return UI.then(() -> {
            for (T item : items) {
                add(item);
            }
            return items;
        });
    }

    @CallSuper
    @NonNull
    @nonnull
    public <A> IAltFuture<A, A> clearAsync() {
        return UI.then(this::clear);
    }

    @CallSuper
    @NonNull
    @nonnull
    public IAltFuture<?, Integer> getCountAsync() {
        return UI.then(this::getCount);
    }

    @CallSuper
    @NonNull
    @nonnull
    public IAltFuture<?, T> getItemAsync(final int position) {
        return UI.then(() -> getItem(position));
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
    public IAltFuture<?, Integer> getPositionAsync(@NonNull @nonnull final T item) {
        return UI.then(() -> getPosition(item));
    }

    @CallSuper
    @NonNull
    @nonnull
    public IAltFuture<?, Filter> getFilterAsync(@NonNull @nonnull final T item) {
        return UI.then(this::getFilter);
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
    public IAltFuture<?, Long> getItemIdAsync(final int position) {
        return UI.then(() -> getItemId(position));
    }

    @CallSuper
    @NonNull
    @nonnull
    public <A> IAltFuture<A, A> setDropDownViewResourceAsync(@LayoutRes final int resource) {
        return UI.then(() -> setDropDownViewResource(resource));
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
    public <A> IAltFuture<A, View> getViewAsync(
            final int position,
            @NonNull @nonnull final View convertView,
            @NonNull @nonnull final ViewGroup parent) {
        return UI.then(() -> getView(position, convertView, parent));
    }

    @CallSuper
    @NonNull
    @nonnull
    public IAltFuture<?, View> getDropDownViewAsync(
            final int position,
            @NonNull @nonnull final View convertView,
            @NonNull @nonnull final ViewGroup parent) {
        return UI.then(() -> getDropDownView(position, convertView, parent));
    }

    @CallSuper
    @NonNull
    @nonnull
    public IAltFuture<?, Boolean> isEmptyAsync() {
        return UI.then(this::isEmpty);
    }
}
