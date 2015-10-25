/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.reactive.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.IReactiveSource;
import com.futurice.cascade.i.IReactiveTarget;

import java.util.concurrent.CopyOnWriteArrayList;

import static com.futurice.cascade.Async.UI;
import static com.futurice.cascade.Async.assertNotNull;
import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.isUiThread;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.throwIllegalStateException;
import static com.futurice.cascade.Async.vv;

/**
 * An ImageView which can be directly bound to change the screen when an up-chain value changes
 */
public class ReactiveImageView extends ImageView implements IReactiveTarget<Bitmap> {
    @Nullable
    private final ImmutableValue<String> mOrigin = isInEditMode() ? null : originAsync();
    private final CopyOnWriteArrayList<IReactiveSource<Bitmap>> mReactiveSources = new CopyOnWriteArrayList<>();

    public ReactiveImageView(@NonNull  final Context context) {
        super(context);
    }

    public ReactiveImageView(
            @NonNull  final Context context,
            @NonNull  final AttributeSet attrs) {
        super(context, attrs);
    }

    public ReactiveImageView(
            @NonNull  final Context context,
            @NonNull  final AttributeSet attrs,
            @StyleRes final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void fire(@NonNull  final Bitmap bitmap) {
        assertNotNull(mOrigin);
        dd(this, mOrigin, "fire bitmap=");

        if (isUiThread()) {
            setImageBitmap(bitmap);
        } else {
            UI.execute(() -> setImageBitmap(bitmap));
        }
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void fireNext(@NonNull  final Bitmap bitmap) {
        //FIXME is not really fire next

        fire(bitmap);
    }

    @Override
    public void setImageURI(@NonNull  final Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void subscribeSource(
            @NonNull  final String reason,
            @NonNull  final IReactiveSource<Bitmap> reactiveSource) {
        assertNotNull(mOrigin);
        vv(this, mOrigin, "Subscribing ReactiveImageView: reason=" + reason + " source=" + reactiveSource.getName());

        if (mReactiveSources.addIfAbsent(reactiveSource)) {
            vv(this, mOrigin, reactiveSource.getName() + " says hello: reason=" + reason);
        } else {
            dd(this, mOrigin, "Did you say hello several times or create some other mess? Upchain says hello, but we already have a hello value \"" + reactiveSource.getName() + "\" at \"" + getName() + "\"");
        }
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void unsubscribeSource(
            @NonNull  final String reason,
            @NonNull  final IReactiveSource<Bitmap> reactiveSource) {
        assertNotNull(mOrigin);
        if (mReactiveSources.remove(reactiveSource)) {
            vv(this, mOrigin, "Upchain says goodbye: reason=" + reason + " reactiveSource=" + reactiveSource.getName());
            reactiveSource.unsubscribe(reason, this);
        } else {
            throwIllegalStateException(this, mOrigin, "Upchain says goodbye, reason=" + reason + ", but upchain \"" + reactiveSource.getName() + "\" is not currently subscribed to \"" + getName() + "\"");
        }
    }

    @Override // IReactiveTarget
    public void unsubscribeAllSources(@NonNull  final String reason) {

        for (IReactiveSource<Bitmap> reactiveSource : mReactiveSources) {
            reactiveSource.unsubscribeAll(reason);
        }
    }

    @Override // INamed
    @NonNull
    public String getName() {
        return "ReactiveTextView-" + getId();
    }

    @Override // View
    @UiThread
    public void onDetachedFromWindow() {
        unsubscribeAllSources("onDetachedFromWindow");
        super.onDetachedFromWindow();
    }
}
