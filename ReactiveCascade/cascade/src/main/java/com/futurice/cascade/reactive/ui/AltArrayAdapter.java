package com.futurice.cascade.reactive.ui;

import android.content.*;
import android.view.*;
import android.widget.*;

import com.futurice.cascade.i.action.*;
import com.futurice.cascade.i.functional.*;

import java.util.*;

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
    public AltArrayAdapter(Context context, int resource) {
        super(context, resource, 0, new ArrayList<T>());
    }

    public AltArrayAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId, new ArrayList<T>());
    }

    public AltArrayAdapter(Context context, int resource, T[] objects) {
        super(context, resource, 0, Arrays.asList(objects));
    }

    public AltArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
        super(context, resource, textViewResourceId, Arrays.asList(objects));
    }

    public AltArrayAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, 0, objects);
    }

    public AltArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public static AltArrayAdapter<CharSequence> createFromResource(Context context, int textArrayResId, int textViewResId) {
        CharSequence[] strings = context.getResources().getTextArray(textArrayResId);
        return new AltArrayAdapter<>(context, textViewResId, strings);
    }

    public <A> IAltFuture<A, T> addAsync(T value) {
        return UI.then(
                () -> {
                    add(value);
                    return value;
                });
    }

    public <A> IAltFuture<A, T> insertAsync(T object, int index) {
        return UI.then(() -> {
            insert(object, index);
            return object;
        });
    }

    public <A> IAltFuture<A, T> removeAsync(T object) {
        return UI.then(() -> {
            remove(object);
            return object;
        });
    }

    public <A> IAltFuture<A, A> sortAsync(Comparator<? super T> comparator) {
        return UI.then(() -> sort(comparator));
    }

    /**
     * Although the underlying call is thread safe to call directly, we always want this to
     * run after any pending changes on the UI thread.
     *
     * @param <A>
     * @return
     */
    public <A> IAltFuture<A, A> notifyDataSetChangedAsync() {
        return UI.then(() -> notifyDataSetChanged());
    }

    /**
     * Although the underlying call is thread safe to call directly, we always want this to
     * run after any pending changes on the UI thread.
     *
     * @param <A>
     * @return
     */
    public <A> IAltFuture<A, A> notifyDataSetInvalidatedAsync() {
        return UI.then(() -> notifyDataSetInvalidated());
    }

    public <A> IAltFuture<A, A> setNotifyOnChangeAsync(boolean notifyOnChange) {
        return UI.then(() -> setNotifyOnChange(notifyOnChange));
    }

    public <A> IAltFuture<A, A> addAllAsync(Collection<T> collection) {
        return UI.then(() -> addAll(collection));
    }

    public <A> IAltFuture<A, A> addAllAsync(T... items) {
        //TODO Not an atomic add operation
        return UI.then(() -> {
            for (T item : items) {
                add(item);
            }
        });
    }

    public <A> IAltFuture<A, A> clearAsync() {
        return UI.then(() -> clear());
    }

    public <A> IAltFuture<A, Integer> getCountAsync() {
        return UI.then((IActionR) () -> getCount());
    }

    public <A> IAltFuture<A, T> getItemAsync(int position) {
        return UI.then((IActionR) () -> getItem(position));
    }

    /**
     * Warning: inherently not thread safe. There may be changes to the model which render the
     * returned index obsolete before you get a chance to use it.
     *
     * @param item
     * @param <A>
     * @return
     */
    public <A> IAltFuture<A, Integer> getPositionAsync(T item) {
        return UI.then((IActionR) () -> getPosition(item));
    }

    public <A> IAltFuture<A, Filter> getFilterAsync(T item) {
        return UI.then((IActionR) () -> getFilter());
    }

    /**
     * Warning: inherently not thread safe. There may be changes to the model which render the
     * returned index obsolete before you get a chance to use it.
     *
     * @param position
     * @param <A>
     * @return
     */
    public <A> IAltFuture<A, Long> getItemIdAsync(int position) {
        return UI.then((IActionR) () -> getItemId(position));
    }

    public <A> IAltFuture<A, A> setDropDownViewResourceAsync(int resource) {
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
    public <A> IAltFuture<A, View> getViewAsync(int position, View convertView, ViewGroup parent) {
        return UI.then((IActionR) () -> getView(position, convertView, parent));
    }

    public <A> IAltFuture<A, View> getDropDownViewAsync(int position, View convertView, ViewGroup parent) {
        return UI.then((IActionR) () -> getDropDownView(position, convertView, parent));
    }

    public <A> IAltFuture<A, Boolean> isEmptyAsync() {
        return UI.then((IActionR) () -> isEmpty());
    }
}
