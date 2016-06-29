/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.futurice.cascade.reactive.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.widget.TextView;

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.IAsyncOrigin;
import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.IReactiveSource;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.reactive.ReactiveValue;
import com.futurice.cascade.util.AssertUtil;
import com.futurice.cascade.util.RCLog;

import static com.futurice.cascade.Async.UI;

/**
 * Change the displayed from from any thread by calling <code>view.bindable.set(String from)</code>
 * <p>
 * Examples:
 * <pre><code>
 *     AtomicTextView view = new AtomicTextView(Activity.this);
 *
 *     // Inject shared concurrent data from your model
 *     AtomicValue<String> statusAtomicValue = new AtomicValue<>();
 *     AtomicTextView liveView = new AtomicTextView(Activity.this, statusAtomicValue);
 *     statusAtomicView.set("Something updated live to the screen");
 * </code></pre>
 */
@NotCallOrigin
public class ReactiveTextView extends TextView implements INamed, IAsyncOrigin {
    @NonNull
    private final ImmutableValue<String> mOrigin = isInEditMode() ? RCLog.DEFAULT_ORIGIN : RCLog.originAsync();
    private IReactiveSource<String> mReactiveSource; // Access only from UI thread
    private volatile ReactiveValue<String> mReactiveValue = isInEditMode() ? null : new ReactiveValue<>(getName(), ""); // Change only from UI thread

    public ReactiveTextView(@NonNull Context context) {
        super(context);

        mReactiveValue.set(getText().toString());
    }

    public ReactiveTextView(@NonNull Context context,
                            @NonNull AttributeSet attrs) {
        super(context, attrs);

        mReactiveValue.set(getText().toString());
    }

    public ReactiveTextView(@NonNull Context context,
                            @NonNull AttributeSet attrs,
                            @StyleRes int defStyle) {
        super(context, attrs, defStyle);

        mReactiveValue.set(getText().toString());
    }

    @TargetApi(21)
    public ReactiveTextView(@NonNull Context context,
                            @NonNull AttributeSet attrs,
                            @AttrRes int defStyleAttr,
                            @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mReactiveValue.set(getText().toString());
    }

    @NonNull
    public String getName() {
        return "ReactiveEditText" + getId();
    }

    @Override // View
    @UiThread
    public void onDetachedFromWindow() {
        if (mReactiveSource != null) {
            mReactiveValue.unsubscribeSource("onDetachedFromWindow", mReactiveSource);
            mReactiveSource = null;
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
        mReactiveSource = mReactiveValue.subscribe(UI, this::setText);
    }

    /**
     * Change the view model used to set the display
     *
     * @param reactiveValue the new view model
     * @param fire          push the current from of the view model to the screen after this action completes on the UI thread
     */
    public void setReactiveValue(@NonNull ReactiveValue<String> reactiveValue,
                                 boolean fire) {
        AssertUtil.assertNotNull(mOrigin);
        final String s = "setReactiveValue(" + reactiveValue.getName() + ")";

        UI.execute(() -> {
            if (mReactiveSource != null) {
                reactiveValue.unsubscribeSource(s, mReactiveSource);
            }
            RCLog.v(this, s);
            this.mReactiveValue = reactiveValue;
            subscribe();
            if (fire) {
                reactiveValue.fire();
            }
        });
    }

    @NonNull
    public ReactiveValue<String> getReactiveValue() {
        return this.mReactiveValue;
    }

    @NonNull
    @Override
    public ImmutableValue<String> getOrigin() {
        return mOrigin;
    }
}
