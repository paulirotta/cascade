package com.futurice.cascade.reactive.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.widget.TextView;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.reactive.IReactiveSource;
import com.futurice.cascade.reactive.ReactiveValue;
import com.futurice.cascade.util.nonnull;
import com.futurice.cascade.util.nullable;

import static com.futurice.cascade.Async.*;

/**
 * Change the displayed value from any thread by calling <code>view.bindable.set(String value)</code>
 * <p>
 * Examples:
 * <pre><code>
 *     AtomicTextView view = new AtomicTextView(Activity.this);
 * <p>
 *     // Inject shared concurrent data from your model
 *     AtomicValue<String> statusAtomicValue = new AtomicValue<>();
 *     AtomicTextView liveView = new AtomicTextView(Activity.this, statusAtomicValue);
 *     statusAtomicView.set("Something updated live to the screen");
 * </code></pre>
 */
@NotCallOrigin
public class ReactiveTextView extends TextView implements INamed {
//    private static ConcurrentLinkedQueue<ImmutableValue<String>> INSTANCE_ORIGINS;

//    static {
//        try {
//            INSTANCE_ORIGINS = DEBUG ? new ConcurrentLinkedQueue<>() : null;
//        } catch (Throwable e) {
//            // Ignore in visual editor which lacks the path. No good way to check isInEditMode() at static level but we want this code removed by Proguard
//        }
//    }


    //    private final CopyOnWriteArrayList<IReactiveSource<String>> reactiveSources = new CopyOnWriteArrayList<>();
    @Nullable
    @nullable
    private final ImmutableValue<String> origin = isInEditMode() ? null : originAsync();
    private IReactiveSource<String> reactiveSource; // Access only from UI thread
    private volatile ReactiveValue<String> reactiveValue = isInEditMode() ? null : new ReactiveValue<>(getName(), ""); // Change only from UI thread

    //TODO Memory test, see that everything is cleaned up after multiple app cycles and bad real world events
//    public static String getInstanceOrigins() {
//        if (!DEBUG) {
//            return "(instances not tracked in production builds)";
//        }
//
//        final StringBuffer sb = new StringBuffer();
//        int i = 0;
//
//        for (ImmutableValue<String> origin : INSTANCE_ORIGINS) {
//            final String s = "Found instance of " + TAG + " created at " + origin;
//            sb.append(s);
//            sb.append('\n');
//            dd(TAG, s);
//            i++;
//        }
//        final String s = "Found " + i + " instance(s) of " + TAG;
//        sb.append('\n');
//        sb.append(s);
//        dd(TAG, s);
//
//        return sb.toString();
//    }

    public ReactiveTextView(@NonNull @nonnull final Context context) {
        super(context);

//        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.add(origin);
//        }
        reactiveValue.set(getText().toString());
    }

    public ReactiveTextView(
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs) {
        super(context, attrs);

///        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.add(origin);
//        }
        reactiveValue.set(getText().toString());
    }

    public ReactiveTextView(
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs,
            @StyleRes final int defStyle) {
        super(context, attrs, defStyle);

//        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.add(origin);
//        }
        reactiveValue.set(getText().toString());
    }

    @TargetApi(21)
    public ReactiveTextView(
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs,
            @AttrRes final int defStyleAttr,
            @StyleRes final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        reactiveValue.set(getText().toString());
    }

    @NonNull
    @nonnull
    public String getName() {
        return "ReactiveEditText" + getId();
    }

    @Override // View
    @UiThread
    public void onDetachedFromWindow() {
        if (reactiveSource != null) {
            reactiveValue.unsubscribeSource("onDetachedFromWindow", reactiveSource);
            reactiveSource = null;
        }
        super.onDetachedFromWindow();
    }

    @Override // View
    @UiThread
    public void onAttachedToWindow() {
        onDetachedFromWindow();
        subscribe();
        super.onAttachedToWindow();
    }

    private void subscribe() {
        reactiveSource = reactiveValue.subscribe(UI, this::setText);
    }

    /**
     * Change the view model used to set the display
     *
     * @param reactiveValue the new view model
     * @param fire          push the current value of the view model to the screen after this action completes on the UI thread
     */
    public void setReactiveValue(@NonNull @nonnull final ReactiveValue<String> reactiveValue, final boolean fire) {
        assertNotNull(origin);
        final String s = "setReactiveValue(" + reactiveValue.getName() + ")";

        UI.execute(() -> {
            if (reactiveSource != null) {
                reactiveValue.unsubscribeSource(s, reactiveSource);
            }
            vv(this, origin, s);
            this.reactiveValue = reactiveValue;
            subscribe();
            if (fire) {
                reactiveValue.fire();
            }
        });
    }

    @NonNull
    @nonnull
    public ReactiveValue<String> getReactiveValue() {
        return this.reactiveValue;
    }

//    @Override // Object
//    public void finalize() {
//        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.remove(this);
//        }
//    }
}
