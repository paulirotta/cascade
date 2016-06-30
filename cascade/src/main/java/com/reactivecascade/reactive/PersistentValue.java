/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.reactive;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IActionOneR;
import com.reactivecascade.i.IAltFuture;
import com.reactivecascade.i.IThreadType;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.AltFutureFuture;
import com.reactivecascade.util.AssertUtil;
import com.reactivecascade.util.DefaultThreadType;
import com.reactivecascade.util.RCLog;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A {@link com.reactivecascade.reactive.ReactiveValue} which retains state between stops and
 * starts of the application.
 * <p>
 * You must provide a unique name. PersistentValue enforces a singletone per name. If any other
 * persistent from has the same name, they will share from and be the same object. This is an
 * intentional dependency state injection in a flat name space, so pick a naming convention that will
 * make your app debuggable and maintainable.
 * <p>
 * TODO Support JSON and/or Serializable and Lists of such arbitrary types
 * TODO Support null as a persisted from by storing a special marker to indicate NOT_ASSERTED and using that to trigger accepting the default passed in. Or something simpler
 * <p>
 * TODO Eliminate this class, replace with a new @Persist annotation to any IReactiveSource that would like persistent state
 * TODO Create IReactiveBindingContext to allow values to start and stop with fragment and activity resume/pause or other custom cases. null context means forever
 * TODO Persist triggered only in onPause() transition of the IReactiveBindingContext
 */
@NotCallOrigin
public class PersistentValue<T> extends ReactiveValue<T> {
    private static final String TAG = PersistentValue.class.getSimpleName();
    private static final int INIT_READ_TIMEOUT_SECONDS = 3;

    private static final ConcurrentHashMap<String, PersistentValue<?>> PERSISTENT_VALUES = new ConcurrentHashMap<>();
    // The SharedPreferences type is not thread safe, so all operations are done from this thread. Note also that we want an uncluttered queue so we can read and write things as quickly as possible.
    private static final IThreadType persistentValueThreadType = new DefaultThreadType("PersistentValueThreadType", Executors.newSingleThreadExecutor(), new LinkedBlockingQueue<>());
    private static final IActionOne<Exception> defaultOnErrorAction = e ->
            RCLog.e(PersistentValue.class.getSimpleName(), "Internal error", e);
    @NonNull
    protected final Context context; // Once changes from an Editor are committed, they are guaranteed to be written even if the parent Context starts to go down
    @NonNull
//    protected final SharedPreferences sharedPreferences; // Once changes from an Editor are committed, they are guaranteed to be written even if the parent Context starts to go down
    protected final String key;
    protected final Class classOfPersistentValue;
    protected final T defaultValue;
    private static final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferencesListener = (sharedPreference, key) -> {
        if (key == null) {
            return;
        }

        PersistentValue<?> persistentValue = PERSISTENT_VALUES.get(key);
        if (persistentValue == null) {
            RCLog.d(TAG, "SharedPreference " + key + " has changed, but the PersistentValue is an expired WeakReference. Probably this is PersistentValue which has gone out of scope before the from persisted. Ignoring this change");
            return;
        }
        persistentValue.onSharedPreferenceChanged();
    };

    private static String getKey(@NonNull Class claz,
                                 @NonNull String name) {
        return claz.getPackage().getName() + name;
    }

    private static String getKey(@NonNull Context context,
                                 @NonNull String name) {
        return getKey(context.getClass(), name);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <TT> PersistentValue<TT> getAlreadyInitializedPersistentValue(
            @NonNull String name,
            @NonNull Context context,
            @NonNull IActionOne<Exception> onErrorAction) {
        PersistentValue<TT> pv = (PersistentValue<TT>) PERSISTENT_VALUES.get(getKey(context, name));

        if (pv == null) {
            return null;
        }

        if (!pv.mOnError.equals(onErrorAction)) {
            RCLog.i(pv, "WARNING: PersistentValue is accessed two places with different onErrorAction. The first mOnError set will be used.\nConsider creating your onErrorAction only once or changing how you access this PersistentValue.");
        }

        return pv;
    }

    /**
     * Initialize a from, loading it from flash memory if it has been previously saved
     *
     * @param name
     * @param defaultValueIfNoPersistedValue
     * @param threadType
     * @param inputMapping
     * @param onError
     * @param context
     * @param <TT>
     * @return
     */
    public static synchronized <TT> PersistentValue<TT> getPersistentValue(
            @NonNull String name,
            @NonNull TT defaultValueIfNoPersistedValue,
            @NonNull IThreadType threadType,
            @Nullable IActionOneR<TT, TT> inputMapping,
            @Nullable IActionOne<Exception> onError,
            @NonNull Context context) {
        IActionOne<Exception> errorAction = onError != null ? onError : defaultOnErrorAction;

        PersistentValue<TT> persistentValue = getAlreadyInitializedPersistentValue(name, context, errorAction);

        if (persistentValue == null) {
            persistentValue = new PersistentValue<>(name, defaultValueIfNoPersistedValue, threadType, inputMapping, onError, context);
        } else {
            final TT tt = persistentValue.get();
            RCLog.v(persistentValue, "Found existing PersistentValue name=" + name + " with existing from: " + tt);
        }

        return persistentValue;
    }

    private static String toStringSet(@NonNull long[] value) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < value.length; i++) {
            sb.append(value[i]);
            if (i < value.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static long[] toLongArray(@NonNull String value) {
        if (value.trim().length() == 0) {
            return new long[0];
        }

        String[] vals = value.split("\n");
        long[] longs = new long[vals.length];
        int i = 0;

        for (String v : vals) {
            longs[i++] = Long.parseLong(v);
        }

        return longs;
    }

    private static String toStringSet(@NonNull int[] value) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < value.length; i++) {
            sb.append(value[i]);
            if (i < value.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static String[] toStringArray(@NonNull String value) {
        if (value.trim().length() == 0) {
            return new String[0];
        }

        return value.split("\n");
    }

    private static int[] toIntegerArray(@NonNull String value) {
        if (value.trim().length() == 0) {
            return new int[0];
        }

        String[] vals = value.split("\n");
        int[] ints = new int[vals.length];
        int i = 0;

        for (String v : vals) {
            ints[i++] = Integer.parseInt(v);
        }

        return ints;
    }

    private static String toStringSet(@NonNull boolean[] value) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < value.length; i++) {
            sb.append(value[i]);
            if (i < value.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static boolean[] toBooleanArray(@NonNull String value) {
        if (value.trim().length() == 0) {
            return new boolean[0];
        }

        String[] vals = value.split(",");
        boolean[] bools = new boolean[vals.length];
        int i = 0;

        for (String v : vals) {
            bools[i++] = Boolean.parseBoolean(v);
        }

        return bools;
    }

    private static String toStringSet(@NonNull float[] value) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < value.length; i++) {
            sb.append(value[i]);
            if (i < value.length - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    private static String toStringSet(@NonNull String[] value) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < value.length; i++) {
            sb.append(value[i]);
            if (i < value.length - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    private static float[] toFloatArray(@NonNull String value) {
        if (value.trim().length() == 0) {
            return new float[0];
        }

        String[] vals = value.split(",");
        float[] floats = new float[vals.length];
        int i = 0;

        for (final String v : vals) {
            floats[i++] = Float.parseFloat(v);
        }

        return floats;
    }

    protected PersistentValue(
            @NonNull String name,
            @NonNull T defaultValueIfNoPersistedValue,
            @NonNull IThreadType threadType,
            @Nullable IActionOneR<T, T> inputMapping,
            @Nullable IActionOne<Exception> onError,
            @NonNull Context context) {
        super(name, threadType, inputMapping, onError);

        this.defaultValue = defaultValueIfNoPersistedValue;
        this.classOfPersistentValue = defaultValueIfNoPersistedValue.getClass();
        this.context = AssertUtil.assertNotNull(context, "Context can not be null");
//        this.sharedPreferences = AssertUtil.assertNotNull(PreferenceManager.getDefaultSharedPreferences(context), "Shared preferences can not be null");
        this.key = getKey(context, name);

        try {
            final T initialValue = init(context)
                    .get(INIT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (initialValue == IAltFuture.VALUE_NOT_AVAILABLE) {
                fire(defaultValueIfNoPersistedValue);
            }
        } catch (Exception e) {
            RCLog.e(this, "Can not initialize", e);
        }
    }

    @SuppressWarnings("unchecked")
    private AltFutureFuture<T, T> init(Context context) {
        // Always access SharedPreferences from the same thread
        // Convert async operation into blocking synchronous so that the ReactiveValue will be initialized before the constructor returns
        return new AltFutureFuture<T, T>((IAltFuture<T, T>) persistentValueThreadType.then(() -> {
            PersistentValue<T> previouslyInitializedPersistentValue = (PersistentValue<T>) PERSISTENT_VALUES.putIfAbsent(getKey(context, getName()), this);
            SharedPreferences sharedPreferences = AssertUtil.assertNotNull(PreferenceManager.getDefaultSharedPreferences(context), "Shared preferences can not be null");

            if (previouslyInitializedPersistentValue != null) {
                return previouslyInitializedPersistentValue.safeGet();
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener);

            return (T) IAltFuture.VALUE_NOT_AVAILABLE;
//            if (sharedPreferences.contains(key)) {
//                RCLog.v(this, "PersistentValue from loadeded from flash memory");
//                onSharedPreferenceChanged();
//            }
        })
                .fork());
    }

    @CallSuper
    @SuppressWarnings("unchecked")
    protected void onSharedPreferenceChanged() {
        RCLog.v(this, "PersistentValue is about to change because the underlying SharedPreferences notify that it has changed");

        SharedPreferences sharedPreferences = AssertUtil.assertNotNull(PreferenceManager.getDefaultSharedPreferences(context), "Shared preferences can not be null");

        if (classOfPersistentValue == String.class) {
            super.set((T) sharedPreferences.getString(key, (String) defaultValue));
        } else if (classOfPersistentValue == String[].class) {
            super.set((T) toStringArray(sharedPreferences.getString(key, toStringSet((String[]) defaultValue))));
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
            throw new UnsupportedOperationException(classOfPersistentValue + " is not supported. Only native types and arrays like String and int[] are supported in PersistentValue. You could override set(), compareAndSet() and get()...");
        }
    }

    @NotCallOrigin
    @CallSuper
    @Override
    public void set(@NonNull T value) {
        super.set(value);

        RCLog.v(this, "PersistentValue \"" + getName() + "\" persist soon, from=" + value);
        persistentValueThreadType.then(() -> {
            SharedPreferences sharedPreferences = AssertUtil.assertNotNull(PreferenceManager.getDefaultSharedPreferences(context), "Shared preferences can not be null");
            SharedPreferences.Editor editor = AssertUtil.assertNotNull(sharedPreferences, "Shared preferences are null").edit();

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
            } else if (value instanceof String[]) {
                editor.putString(key, toStringSet((String[]) value));
            } else {
                throw new UnsupportedOperationException("Only native types like String are supported in PersistentValue. You could override set(), compareAndSet() and get()...");
            }
            if (!editor.commit()) {
                throw new RuntimeException("Failed to commit PersistentValue from=" + value + ". Probably some other thread besides Async.Net.NET_WRITE is concurrently updating SharedPreferences for this Context");
            }
            RCLog.v(this, "Successful PersistentValue persist, from=" + value);
        })
                .onError(mOnError);
    }
}
