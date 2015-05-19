package com.futurice.cascade.reactive.ui;

import android.content.Context;

import com.futurice.cascade.Async;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.rest.MirrorService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Houghton on 20-Oct-14.
 */
public class MirrorArrayAdapter<V> extends AltArrayAdapter<V> {
    public final MirrorDelegate mirrorDelegate = new MirrorDelegate("MirrorDelegate", Async.UI, Async.UI);

    public MirrorArrayAdapter(Context context, int resource) {
        super(context, resource);
    }

    public MirrorArrayAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public MirrorArrayAdapter(Context context, int resource, V[] objects) {
        super(context, resource, objects);
    }

    public MirrorArrayAdapter(Context context, int resource, int textViewResourceId, V[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public MirrorArrayAdapter(Context context, int resource, List objects) {
        super(context, resource, objects);
    }

    public MirrorArrayAdapter(Context context, int resource, int textViewResourceId, List objects) {
        super(context, resource, textViewResourceId, objects);
    }

    /**
     * This can be attached to an upstream {@link com.futurice.cascade.rest.MirrorService} to copy
     * changes in the data model onto the screen. The idea it to manipulate the model (add remove sort)
     * and let the UI copy that. The filter mechanism in {@link android.widget.ArrayAdapter} should
     * be sufficient for reactive searching within the displayed items.
     *
     * It is recommended to do manipulation indirectly through the up-mirror model and no direct
     * changes to this view model. If you do need to change the model directly, subscribe changes to the
     * <code>MirrorDelegate</code> would be preferred as they keep open the possibility of
     * subscribing other {@link com.futurice.cascade.reactive.ui.MirrorArrayAdapter}s to this one and they
     * receive the changes also (such as {@link android.widget.Filter}. Changes made for example
     * through {@link com.futurice.cascade.reactive.ui.MirrorArrayAdapter#add(Object)} are not thread safe and
     * must be done from the UI thread. Changes made for example
     * through {@link com.futurice.cascade.reactive.ui.MirrorArrayAdapter#addAsync(Object)} are safe from any
     * thread, but will not propagate to a downstream {@link com.futurice.cascade.rest.MirrorService}.
     *
     * It is possible to subscribe for example several different mirrors for several different tabs on screen
     * of other UI components. At the moment the use for this is to maintain 4 differently-sorted and
     * differently-filtered views of the same list of cards.
     */
    public class MirrorDelegate extends MirrorService<Integer, V> {

        public MirrorDelegate(String name, IThreadType readIThreadType, IThreadType writeIThreadType) {
            super(name, readIThreadType, writeIThreadType);
        }

        @Override
        public List<Integer> index() throws IOException {
            Async.assertUIThread();

            final int count = MirrorArrayAdapter.this.getCount();
            final ArrayList<Integer> index = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                index.add(i);
            }

            return index;
        }

        @Override
        public V get(Integer key) throws IOException {
            Async.assertUIThread();

            // There is no downstream publish for a "get()", it bounces off this mirror layer
            return MirrorArrayAdapter.this.getItem(key);
        }

        @Override
        public void put(Integer key, V value) throws Exception {
            Async.assertUIThread();
            MirrorArrayAdapter.this.insert(value, key);

            // Publish the changes to any downstream mirror. Probably there aren't any
            super.put(key, value);
        }

        @Override
        public boolean delete(Integer key) throws Exception {
            Async.assertUIThread();

            boolean deleted = MirrorArrayAdapter.this.getCount() > key;
            if (deleted) {
                MirrorArrayAdapter.this.remove(get(key));

                // Publish the changes to any downstream mirror. Probably there aren't any
                super.delete(key);
            }

            return deleted;
        }

        /**
         * This onFireAction really is atomic because both the MirrorArrayAdapter and MirrorDelegate are limited
         * to the UI thread.
         *
         * @param key
         * @param value
         * @param expectedValue
         * @return
         * @throws Exception
         */
        @Override
        public boolean replace(Integer key, V value, V expectedValue) throws Exception {
            Async.assertUIThread();
            boolean actionPerformed = expectedValue.equals(MirrorArrayAdapter.this.getItem(key));

            if (actionPerformed) {
                MirrorArrayAdapter.this.remove(expectedValue);
                MirrorArrayAdapter.this.insert(value, key);
                // Publish the changes to any downstream mirror. Probably there aren't any
                super.replace(key, value, expectedValue);
            }

            return actionPerformed;
        }
    }
}
