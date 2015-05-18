package com.futurice.cascade.reactive.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.reactive.ReactiveValue;

import static com.futurice.cascade.Async.UI;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.vv;

/**
 * Created by Paul Houghton on 12-03-2015.
 */
@NotCallOrigin
public class ReactiveEditText extends EditText implements INamed {
    private final ImmutableValue<String> origin = isInEditMode() ? null : originAsync();
    public volatile ReactiveValue<String> reactiveValue;
    private TextWatcher textWatcher;

    public ReactiveEditText(Context context) {
        super(context);

        reactiveValue = new ReactiveValue<>(UI, getName(), getText().toString());
    }

    public ReactiveEditText(Context context, ReactiveValue<String> reactiveValue) {
        super(context);

        this.reactiveValue = reactiveValue;
    }

    public ReactiveEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        reactiveValue = new ReactiveValue<>(UI, getName(), getText().toString());
    }

    public ReactiveEditText(Context context, AttributeSet attrs, ReactiveValue<String> reactiveValue) {
        super(context, attrs);

        this.reactiveValue = reactiveValue;
    }

    public ReactiveEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        reactiveValue = new ReactiveValue<>(UI, getName(), getText().toString());
    }

    public ReactiveEditText(Context context, AttributeSet attrs, int defStyleAttr, ReactiveValue<String> reactiveValue) {
        super(context, attrs, defStyleAttr);

        this.reactiveValue = reactiveValue;
    }

    @TargetApi(21)
    public ReactiveEditText(String name, Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        reactiveValue = new ReactiveValue<>(UI, getName(), getText().toString());
    }

    @TargetApi(21)
    public ReactiveEditText(String name, Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, ReactiveValue<String> reactiveValue) {
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
    public ReactiveValue<String> setReactiveValue(ReactiveValue<String> reactiveValue, boolean fire) {
        UI.execute((IAction)() -> {
            this.reactiveValue = reactiveValue;
            if (fire) {
                reactiveValue.fire();
            }
        });

        return reactiveValue;
    }

    @Override // INamed
    public String getName() {
        return "ReactiveEditText" + getId();
    }

    @Override // View
    public void onAttachedToWindow() {
        final String currentText = reactiveValue.get();
        setText(currentText);

        vv(this, origin, "onAttachedToWindow " + getName() + ", value=" + currentText);

        reactiveValue.subscribe(UI, (IActionOne<String>) this::setText);

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
    public void onDetachedFromWindow() {
        vv(this, origin, "onDetachedFromWindow " + getName() + ", current value=" + getText());
        reactiveValue.unsubscribeAll("onDetachedFromWindow");
    }
}
