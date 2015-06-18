package com.futurice.cascade.reactive.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.annotation.UiThread;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.reactive.ReactiveValue;

import static com.futurice.cascade.Async.UI;
import static com.futurice.cascade.Async.assertNotNull;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.vv;

/**
 * An {@link EditText} which can be manipulated and which in turn manipulates a supporting
 * {@link ReactiveValue<String>} which reflects the current on-screen value
 *
 * FIXME The cursor position on screen is not maintained nicely when values update
 *
 * Created by Paul Houghton on 12-03-2015.
 */
@NotCallOrigin
public class ReactiveEditText extends EditText implements INamed {
    @Nullable
    private final ImmutableValue<String> origin = isInEditMode() ? null : originAsync();
    public volatile ReactiveValue<String> reactiveValue;
    private TextWatcher textWatcher;

//TODO Constructors which support a string validator (example: trim whitespace or fix capitalization as you type)

    public ReactiveEditText(@NonNull final Context context) {
        super(context);

        reactiveValue = new ReactiveValue<>(getName(), getText().toString());
    }

    public ReactiveEditText(
            @NonNull final Context context,
            @NonNull final ReactiveValue<String> reactiveValue) {
        super(context);

        this.reactiveValue = reactiveValue;
    }

    public ReactiveEditText(
            @NonNull final Context context,
            @NonNull final AttributeSet attrs) {
        super(context, attrs);

        reactiveValue = new ReactiveValue<>(getName(), getText().toString());
    }

    public ReactiveEditText(
            @NonNull final Context context,
            @NonNull final AttributeSet attrs,
            @NonNull final ReactiveValue<String> reactiveValue) {
        super(context, attrs);

        this.reactiveValue = reactiveValue;
    }

    public ReactiveEditText(
            @NonNull final Context context,
            @NonNull final AttributeSet attrs,
            @AttrRes
            final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        reactiveValue = new ReactiveValue<>(getName(), getText().toString());
    }

    public ReactiveEditText(
            @NonNull final Context context,
            @NonNull final AttributeSet attrs,
            @AttrRes
            final int defStyleAttr,
            @NonNull final ReactiveValue<String> reactiveValue) {
        super(context, attrs, defStyleAttr);

        this.reactiveValue = reactiveValue;
    }

    @TargetApi(21)
    public ReactiveEditText(
            @NonNull final String name,
            @NonNull final Context context,
            @NonNull final AttributeSet attrs,
            @AttrRes
            final int defStyleAttr,
            @StyleRes
            final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        reactiveValue = new ReactiveValue<>(getName(), getText().toString());
    }

    @TargetApi(21)
    public ReactiveEditText(
            @NonNull final String name,
            @NonNull final Context context,
            @NonNull final AttributeSet attrs,
            @AttrRes
            final int defStyleAttr,
            @StyleRes
            final int defStyleRes,
            @NonNull final ReactiveValue<String> reactiveValue) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.reactiveValue = reactiveValue;
    }

    /**
     * Set the reactive view model associated with this view
     *
     * @param reactiveValue
     * @param fire
     * @return
     */
    @NonNull
    public ReactiveValue<String> setReactiveValue(
            @NonNull final ReactiveValue<String> reactiveValue,
            final boolean fire) {
        UI.run(() -> {
            this.reactiveValue = reactiveValue;
            if (fire) {
                reactiveValue.fire();
            }
        });

        return reactiveValue;
    }

    @Override // INamed
    @NonNull
    public String getName() {
        return "ReactiveEditText" + getId();
    }

    @Override // View
    @UiThread
    public void onAttachedToWindow() {
        assertNotNull(origin);
        final String currentText = reactiveValue.get();
        setText(currentText);

        vv(this, origin, "onAttachedToWindow " + getName() + ", value=" + currentText);

        reactiveValue.subscribe(UI, this::setText);

        if (textWatcher == null) {
            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    reactiveValue.set(s.toString());
                }
            };
            this.addTextChangedListener(textWatcher);
        }

        super.onAttachedToWindow();
    }

    @Override // View
    @UiThread
    public void onDetachedFromWindow() {
        assertNotNull(origin);
        vv(this, origin, "onDetachedFromWindow " + getName() + ", current value=" + getText());
        reactiveValue.unsubscribeAll("onDetachedFromWindow");

        super.onDetachedFromWindow();
    }
}
