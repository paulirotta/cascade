/*
The MIT License (MIT)

Copyright (c) 2015 Futurice Oy and individual contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.futurice.cascade.reactive;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.futurice.cascade.AltWeakReference;
import com.futurice.cascade.DefaultThreadType;
import com.futurice.cascade.functional.AltFutureFuture;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IOnErrorAction;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.futurice.cascade.Async.*;

/**
 * A {@link com.futurice.cascade.reactive.ReactiveValue} which retains state between stops and
 * starts of the application.
 * <p>
 * You must provide a unique name. PersistentValue enforces a singletone per name. If any other
 * persistent value has the same name, they will share value and be the same object. This is an
 * intentional dependency state injection in a flat name space, so pick a naming convention that will
 * make your app debuggable and maintainable.
 * <p>
 * TODO Support JSON and/or Serializable and Lists of such arbitrary types
 * TODO Support null as a persisted value by storing a special marker to indicate NOT_ASSERTED and using that to trigger accepting the default passed in. Or something simpler
 */
public class PersistentValue<T> extends ReactiveValue<T> {
    private static final String TAG = PersistentValue.class.getSimpleName();
    private static final int INIT_READ_TIMEOUT_SECONDS = 10;

    private static final ConcurrentHashMap<String, AltWeakReference<PersistentValue<?>>> PERSISTENT_VALUES = new ConcurrentHashMap<>();
    // The SharedPreferences type is not thread safe, so all operations are done from this thread. Note also that we want an uncluttered queue so we can read and write things as quickly as possible.
    private static final IThreadType persistentValueThreadType = new DefaultThreadType("PersistentValueThreadType", Executors.newSingleThreadExecutor(), new LinkedBlockingQueue<>());
    private static final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferencesListener = (sharedPreference, key) -> {
        final WeakReference<PersistentValue<?>> wr = PERSISTENT_VALUES.get(key);

        if (wr == null) {
            vv(TAG, "SharedPreference " + key + " has changed, but it is not recognized as a PersistentValue. You can ignore this warning if you use SharedPreferences without other than as a PersistentValue");
            return;
        }
        final PersistentValue<?> persistentValue = wr.get();
        if (persistentValue == null) {
            dd(TAG, "SharedPreference " + key + " has changed, but the PersistentValue is an expired WeakReference. Probably this is PersistentValue which has gone out of scope before the value persisted. Ignoring this change");
            return;
        }
        persistentValue.onSharedPreferenceChanged();
    };


    protected final SharedPreferences sharedPreferences; // Once changes from an Editor are committed, they are guaranteed to be written even if the parent Context starts to go down
    protected final String key;
    protected final Class classOfPersistentValue;
    protected final T defaultValue;

    private static String getKey(Class claz, String name) {
        return claz.getPackage().getName() + name;
    }

    private static String getKey(Context context, String name) {
        return getKey(context.getClass(), name);
    }

    private static <TT> PersistentValue<TT> getAlreadyInitializedPersistentValue(@NonNull Context context, @NonNull String name, @NonNull IOnErrorAction onErrorAction) {
        final AltWeakReference<PersistentValue<?>> wr = PERSISTENT_VALUES.get(getKey(context, name));
        if (wr == null) {
            return null;
        }

        final PersistentValue<TT> pv = (PersistentValue<TT>) wr.get();
        if (pv == null) {
            return null;
        }

        if (!pv.onError.equals(onErrorAction)) {
            ii(pv, pv.origin, "WARNING: PersistentValue is accessed two places with different onErrorAction. The first onError set will be used.\nConsider creating your onErrorAction only once or changing how you access this PersistentValue.");
        }

        return pv;
    }

    private static final IOnErrorAction defaultOnErrorAction = e -> {
        ee(PersistentValue.class.getSimpleName(), "Internal error", e);
        return false;
    };

    public static <TT> PersistentValue<TT> getPersistentValue(
            @NonNull final Context context,
            @NonNull final IThreadType threadType,
            @NonNull final String name,
            @NonNull final TT defaultValueIfNoPersistedValue) {
        return getPersistentValue(context, threadType, null, name, defaultValueIfNoPersistedValue, defaultOnErrorAction);
    }

    public static synchronized <TT> PersistentValue<TT> getPersistentValue(
            @NonNull final Context context,
            @NonNull final IThreadType threadType,
            @NonNull final SharedPreferences sharedPreferences,
            @NonNull final String name,
            @NonNull final TT defaultValueIfNoPersistedValue,
            IOnErrorAction onError) {
        if (onError == null) {
            onError = defaultOnErrorAction;
        }

        PersistentValue<TT> persistentValue = getAlreadyInitializedPersistentValue(context, name, onError);

        if (persistentValue == null) {
            persistentValue = new PersistentValue<>(context, threadType, name, defaultValueIfNoPersistedValue, sharedPreferences, onError);
        } else {
            final TT v = persistentValue.get();
            if (v == null) {
                vv(TAG, persistentValue.origin, "Found existing PersistentValue name=" + name + " with existing value=null. Setting initial value=" + defaultValueIfNoPersistedValue);
                persistentValue.set(defaultValueIfNoPersistedValue);
            } else {
                vv(TAG, persistentValue.origin, "Found existing PersistentValue name=" + name + " with existing value " + v);
            }
        }

        return persistentValue;
    }

//    public static synchronized <TT> PersistentValue<TT> getPersistentValue(
//            final Context context,
//            final IThreadType threadType,
//            final String name,
//            final TT defaultValueIfNoPersistedValue,
//            final SharedPreferences sharedPreferences,
//            final IOnErrorAction onError) {
//        assertNotNull("name must be non-null", name);
//        assertNotNull("context must be non-null", context);
//        assertNotNull("threadType must be non-null", threadType);
//        assertNotNull("defaultValueIfNoPersistedValue must be non-null", defaultValueIfNoPersistedValue);
//
//        PersistentValue<TT> persistentValue = loadPersistentValueFromCacheOrFlash(context, name, defaultValueIfNoPersistedValue);
//        if (persistentValue == null) {
//            vv(TAG, "PersistentValue name=" + name + " has not previously been instantiated. Constructing with asserted value=" + defaultValueIfNoPersistedValue);
//            persistentValue = new PersistentValue<TT>(context, threadType, name, defaultValueIfNoPersistedValue, sharedPreferences, onError);
//        }
//
//        return persistentValue;
//    }

    private static SharedPreferences getSharedPreferences(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
        if (sharedPreferences != null) {
            return sharedPreferences;
        }

        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized <TT> PersistentValue<TT> getPersistentValue(
            Context context,
            IThreadType threadType,
            String name,
            TT defaultValueIfNoPersistedValue,
            IOnErrorAction onError) {
        return getPersistentValue(context, threadType, null, name, defaultValueIfNoPersistedValue, onError);
    }

    protected PersistentValue(
            @NonNull Context context,
            @NonNull IThreadType threadType,
            @NonNull String name,
            @NonNull T defaultValueIfNoPersistedValue,
            @NonNull SharedPreferences sharedPreferences,
            @NonNull IOnErrorAction onError) {
        super(threadType, name, onError);
        this.defaultValue = defaultValueIfNoPersistedValue;
        this.classOfPersistentValue = defaultValueIfNoPersistedValue.getClass();
        this.sharedPreferences = getSharedPreferences(context, sharedPreferences);
        this.key = getKey(context, name);

        try {
            init(context, defaultValueIfNoPersistedValue);
        } catch (Exception e) {
            ee(this, origin, "Can not initialize", e);
            try {
                threadType.then(e)
                        .then(e2 -> onError);
            } catch (Exception e2) {
                ee(this, origin, "Can not call onError after failure to initialize: " + e, e2);
            }
        }
    }

    protected void onSharedPreferenceChanged() {
        vv(this, origin, "PersistentValue is about to change because the underlying SharedPreferences notify that it has changed");
        if (classOfPersistentValue == String.class) {
            super.set((T) sharedPreferences.getString(key, (String) defaultValue));
        } else if (classOfPersistentValue == Integer.class) {
            super.set((T) Integer.valueOf(sharedPreferences.getInt(key, (Integer) defaultValue)));
        } else if (classOfPersistentValue == int[].class) {
            super.set((T) toIntegerArray(sharedPreferences.getString(key, toStringSet((int[]) defaultValue))));
        } else if (classOfPersistentValue == Long.class) {
            super.set((T) Long.valueOf(sharedPreferences.getLong(key, (Long) defaultValue)));
        } else if (classOfPersistentValue == long[].class) {
            super.set((T) toLongArray(sharedPreferences.getString(key, toStringSet((long[]) defaultValue))));
        } else if (classOfPersistentValue == Boolean.class) {
            super.set((T) Boolean.valueOf(sharedPreferences.getBoolean(key, (Boolean) defaultValue)));
        } else if (classOfPersistentValue == boolean[].class) {
            super.set((T) toBooleanArray(sharedPreferences.getString(key, toStringSet((boolean[]) defaultValue))));
        } else if (classOfPersistentValue == Float.class) {
            super.set((T) Float.valueOf(sharedPreferences.getFloat(key, (Float) defaultValue)));
        } else if (classOfPersistentValue == float[].class) {
            super.set((T) toFloatArray(sharedPreferences.getString(key, toStringSet((float[]) defaultValue))));
        } else {
            throw new UnsupportedOperationException("Only native types and arrays like String and int[] are supported in PersistentValue. You could override set(), compareAndSet() and get()...");
        }
    }

    private void init(final Context context, T defaultValueIfNoPersistedValue) throws InterruptedException, ExecutionException, TimeoutException {
        // Always access SharedPreferences from the same thread
        // Convert async operation into blocking synchronous so that the ReactiveValue will be initialized before the constructor returns
        new AltFutureFuture<>(persistentValueThreadType.then(() -> {
            final AltWeakReference<PersistentValue<?>> previouslyInitializedPersistentValue = PERSISTENT_VALUES.putIfAbsent(getKey(context, getName()), new AltWeakReference<>(this));
            if (previouslyInitializedPersistentValue != null) {
                ii(this, origin, "WARNING: PersistentValue has already been initialized, a possible race condition resulting in indeterminate initial value may exist");
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener);

            if (sharedPreferences.contains(key)) {
                vv(this, origin, "Newly instantiated PersistentValue is being loaded with a persistent value from flash memory");
                onSharedPreferenceChanged();
            } else {
                vv(this, origin, "Newly instantiated PersistentValue is not found in flash memory");
                set(defaultValueIfNoPersistedValue);
            }
        })
                .onError(onError)
                .fork()
        )
                .get(INIT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static String toStringSet(final long[] value) {
        final StringBuffer sb = new StringBuffer();

        for (int i = 0; i < value.length; i++) {
            sb.append(value[i]);
            if (i < value.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static long[] toLongArray(String value) {
        if (value.trim().length() == 0) {
            return new long[0];
        }

        final String[] vals = value.split("\n");
        final long[] longs = new long[vals.length];
        int i = 0;

        for (String v : vals) {
            longs[i++] = Long.parseLong(v);
        }

        return longs;
    }

    private static String toStringSet(final int[] value) {
        final StringBuffer sb = new StringBuffer();

        for (int i = 0; i < value.length; i++) {
            sb.append(value[i]);
            if (i < value.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static int[] toIntegerArray(String value) {
        if (value.trim().length() == 0) {
            return new int[0];
        }

        final String[] vals = value.split("\n");
        final int[] ints = new int[vals.length];
        int i = 0;

        for (String v : vals) {
            ints[i++] = Integer.parseInt(v);
        }

        return ints;
    }

    private static String toStringSet(final boolean[] value) {
        final StringBuffer sb = new StringBuffer();

        for (int i = 0; i < value.length; i++) {
            sb.append(value[i]);
            if (i < value.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static boolean[] toBooleanArray(String value) {
        if (value.trim().length() == 0) {
            return new boolean[0];
        }

        final String[] vals = value.split(",");
        final boolean[] bools = new boolean[vals.length];
        int i = 0;

        for (String v : vals) {
            bools[i++] = Boolean.parseBoolean(v);
        }

        return bools;
    }

    private static String toStringSet(final float[] value) {
        final StringBuffer sb = new StringBuffer();

        for (int i = 0; i < value.length; i++) {
            sb.append(value[i]);
            if (i < value.length - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    private static float[] toFloatArray(String value) {
        if (value.trim().length() == 0) {
            return new float[0];
        }

        final String[] vals = value.split(",");
        final float[] floats = new float[vals.length];
        int i = 0;

        for (String v : vals) {
            floats[i++] = Float.parseFloat(v);
        }

        return floats;
    }

    @NotCallOrigin
    @Override
    public boolean set(@NonNull final T value) {
        final boolean valueChanged = super.set(value);

        vv(this, origin, "PersistentValue persist soon, value=" + value);
        persistentValueThreadType.then(() -> {
            final SharedPreferences.Editor editor = sharedPreferences.edit();

            if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof long[]) {
                editor.putString(key, toStringSet((long[]) value));
            } else if (value instanceof int[]) {
                editor.putString(key, toStringSet((int[]) value));
            } else if (value instanceof boolean[]) {
                editor.putString(key, toStringSet((boolean[]) value));
            } else if (value instanceof float[]) {
                editor.putString(key, toStringSet((float[]) value));
            } else {
                throw new UnsupportedOperationException("Only native types like String are supported in PersistentValue. You could override set(), compareAndSet() and get()...");
            }
            if (!editor.commit()) {
                throw new RuntimeException("Failed to commit PersistentValue value=" + value + ". Probably some other thread besides Async.Net.netWriteThreadType is concurrently updating SharedPreferences for this Context");
            }
            vv(this, origin, "Successful PersistentValue persist, value=" + value);
        })
                .onError(onError)
                .fork();

        return valueChanged;
    }
}