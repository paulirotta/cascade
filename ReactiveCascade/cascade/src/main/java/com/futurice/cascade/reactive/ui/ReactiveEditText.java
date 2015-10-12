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
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.cascade.reactive.ReactiveValue;

import static com.futurice.cascade.Async.UI;
import static com.futurice.cascade.Async.assertNotNull;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.vv;

/**
 * An {@link EditText} which can be manipulated and which in turn manipulates a supporting
 * {@link ReactiveValue<String>} which reflects the current on-screen value
 *
 * Created by Paul Houghton on 12-03-2015.
 */
@NotCallOrigin
public class ReactiveEditText extends EditText implements INamed {
    @Nullable
    @nullable
    private final ImmutableValue<String> mOrigin = isInEditMode() ? null : originAsync();
    public volatile ReactiveValue<String> mReactiveValue;
    private TextWatcher mTextWatcher;

//TODO Constructors which support a string validator (example: trim whitespace or fix capitalization as you type)

    public ReactiveEditText(@NonNull @nonnull final Context context) {
        super(context);

        mReactiveValue = new ReactiveValue<>(getName(), getText().toString());
    }

    public ReactiveEditText(
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final ReactiveValue<String> reactiveValue) {
        super(context);

        this.mReactiveValue = reactiveValue;
    }

    public ReactiveEditText(
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs) {
        super(context, attrs);

        mReactiveValue = new ReactiveValue<>(getName(), getText().toString());
    }

    public ReactiveEditText(
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs,
            @NonNull @nonnull final ReactiveValue<String> reactiveValue) {
        super(context, attrs);

        this.mReactiveValue = reactiveValue;
    }

    public ReactiveEditText(
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs,
            @AttrRes
            final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mReactiveValue = new ReactiveValue<>(getName(), getText().toString());
    }

    public ReactiveEditText(
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs,
            @AttrRes
            final int defStyleAttr,
            @NonNull @nonnull final ReactiveValue<String> reactiveValue) {
        super(context, attrs, defStyleAttr);

        this.mReactiveValue = reactiveValue;
    }

    @TargetApi(21)
    public ReactiveEditText(
            @NonNull @nonnull final String name,
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs,
            @AttrRes
            final int defStyleAttr,
            @StyleRes
            final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mReactiveValue = new ReactiveValue<>(getName(), getText().toString());
    }

    @TargetApi(21)
    public ReactiveEditText(
            @NonNull @nonnull final String name,
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs,
            @AttrRes
            final int defStyleAttr,
            @StyleRes
            final int defStyleRes,
            @NonNull @nonnull final ReactiveValue<String> reactiveValue) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.mReactiveValue = reactiveValue;
    }

    /**
     * Set the reactive view model associated with this view
     *
     * @param reactiveValue
     * @param fire
     * @return
     */
    @NonNull
    @nonnull
    public ReactiveValue<String> setReactiveValue(
            @NonNull @nonnull final ReactiveValue<String> reactiveValue,
            final boolean fire) {
        UI.run(() -> {
            this.mReactiveValue = reactiveValue;
            if (fire) {
                reactiveValue.fire();
            }
        });

        return reactiveValue;
    }

    @Override // INamed
    @NonNull
    @nonnull
    public String getName() {
        return "ReactiveEditText" + getId();
    }

    @Override // View
    @UiThread
    public void onAttachedToWindow() {
        assertNotNull(mOrigin);
        final String currentText = mReactiveValue.get();
        setText(currentText);

        vv(this, mOrigin, "onAttachedToWindow " + getName() + ", value=" + currentText);

        mReactiveValue.subscribe(UI, this::setText);

        if (mTextWatcher == null) {
            mTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    setSelection(s.length());
                    mReactiveValue.set(s.toString());
                }
            };
            this.addTextChangedListener(mTextWatcher);
        }

        super.onAttachedToWindow();
    }

    @Override // View
    @UiThread
    public void onDetachedFromWindow() {
        assertNotNull(mOrigin);
        vv(this, mOrigin, "onDetachedFromWindow " + getName() + ", current value=" + getText());
        mReactiveValue.unsubscribeAll("onDetachedFromWindow");

        super.onDetachedFromWindow();
    }
}
