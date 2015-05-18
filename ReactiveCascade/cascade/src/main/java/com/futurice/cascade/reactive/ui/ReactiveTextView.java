package com.futurice.cascade.reactive.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.TextView;

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.reactive.IReactiveSource;
import com.futurice.cascade.reactive.ReactiveValue;

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

    //    private final ImmutableValue<String> origin = isInEditMode() ? null : originAsync();
//    private final CopyOnWriteArrayList<IReactiveSource<String>> reactiveSources = new CopyOnWriteArrayList<>();
    private final ImmutableValue<String> origin = isInEditMode() ? null : originAsync();
    private IReactiveSource<String> reactiveSource; // Access only from UI thread
    private volatile ReactiveValue<String> reactiveValue = isInEditMode() ? null : new ReactiveValue<>(UI, getName()); // Change only from UI thread

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

    public ReactiveTextView(Context context) {
        super(context);

//        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.add(origin);
//        }
        reactiveValue.set(getText().toString());
    }

    public ReactiveTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

///        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.add(origin);
//        }
        reactiveValue.set(getText().toString());
    }

    public ReactiveTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

//        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.add(origin);
//        }
        reactiveValue.set(getText().toString());
    }

    @TargetApi(21)
    public ReactiveTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        reactiveValue.set(getText().toString());
    }

    public String getName() {
        return "ReactiveEditText" + getId();
    }

    //    @Override // IReactiveTarget
//    @NotCallOrigin
//    public void fire(String s, IThreadType currentThreadType, IReactiveSource<String> reactiveSource) {
//        try {
//            dd(this, origin, "ReactiveTextView updated, text=" + s);
//            if (currentThreadType == UI) {
//                setText(s);
//            } else {
//                UI.execute(
//                        () -> setText(s));
//            }
//        } catch (Throwable t) {
//            final String str = "Can not receiveFire " + s + " from " + reactiveSource;
//            ee(this, str, t);
//            unsubscribeSource(str, reactiveSource);
//        }
//    }
//
//    @Override // IReactiveTarget
//    @NotCallOrigin
//    public void subscribeSource(String reason, IReactiveSource<String> reactiveSource) {
//        assertNotNull("reactiveSource must be non-null", reactiveSource);
//
//        dd(this, origin, "Subscribing ReactiveTextView, reason=" + reason + " reactiveSource=" + reactiveSource.getName());
//        if (reactiveSources.addIfAbsent(reactiveSource)) {
//            vv(this, origin, reactiveSource.getName() + " subscribed to this: reason=" + reason);
//        } else {
//            dd(this, origin, reactiveSource.getName() + " subscribed to this: reason=" + reason + ", but we already have a hello from \"" + reactiveSource.getName() + "\" at \"" + getName() + "\"  Are you _SURE_ you want to change value based on two different data sources?");
//        }
//    }
//
//    @Override // IReactiveTarget
//    @NotCallOrigin
//    public void unsubscribeSource(String reason, IReactiveSource<String> reactiveSource) {
//        if (reactiveSource == null) {
//            dd(this, origin, "Ignoring unsubscribing null ReactiveTextView: " + reason);
//            return;
//        }
//
//        if (reactiveSources.remove(reactiveSource)) {
//            dd(this, origin, "Unsubscribing ReactiveTextView: " + reason);
//            reactiveSource.unsubscribe(reason, this);
//        } else {
//            throwIllegalStateException(this, "Can not remove unknown reactive reactiveSource: " + reactiveSource);
//        }
//        ;
//    }
//
//    @Override // IReactiveTarget
//    public void unsubscribeAllSources(String reason) {
//        final Iterator<IReactiveSource<String>> iterator = reactiveSources.iterator();
//
//        while (iterator.hasNext()) {
//            iterator.next().unsubscribeAll(reason);
//        }
//    }

    @Override // View
    public void onDetachedFromWindow() {
        if (reactiveSource != null) {
            reactiveValue.unsubscribeSource("onDetachedFromWindow", reactiveSource);
            reactiveSource = null;
        }
    }

    @Override // View
    public void onAttachedToWindow() {
        onDetachedFromWindow();
        subscribe();
    }

    private void subscribe() {
        reactiveSource = reactiveValue.subscribe(UI, text -> {
            setText(text);
        });
    }

    /**
     * Change the view model used to set the display
     *
     * @param reactiveValue the new view model
     * @param fire          push the current value of the view model to the screen after this action completes on the UI thread
     */
    public void setReactiveValue(@NonNull ReactiveValue<String> reactiveValue, boolean fire) {
        final String s = "setReactiveValue(" + reactiveValue.getName() + ")";

        UI.execute((IAction)() -> {
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
