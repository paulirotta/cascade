/**
 * Copyright (c) 2015 Futurice GmbH. All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.cascade.reactive.IReactiveSource;
import com.futurice.cascade.reactive.IReactiveTarget;

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
    @nullable
    private final ImmutableValue<String> mOrigin = isInEditMode() ? null : originAsync();
    private final CopyOnWriteArrayList<IReactiveSource<Bitmap>> mReactiveSources = new CopyOnWriteArrayList<>();

    public ReactiveImageView(@NonNull @nonnull final Context context) {
        super(context);
    }

    public ReactiveImageView(
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs) {
        super(context, attrs);
    }

    public ReactiveImageView(
            @NonNull @nonnull final Context context,
            @NonNull @nonnull final AttributeSet attrs,
            @StyleRes final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void fire(@NonNull @nonnull final Bitmap bitmap) {
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
    public void fireNext(@NonNull @nonnull final Bitmap bitmap) {
        //FIXME is not really fire next

        fire(bitmap);
    }

    @Override
    public void setImageURI(@NonNull @nonnull final Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void subscribeSource(
            @NonNull @nonnull final String reason,
            @NonNull @nonnull final IReactiveSource<Bitmap> reactiveSource) {
        assertNotNull(mOrigin);
        vv(this, mOrigin, "Subscribing ReactiveImageView: reason=" + reason + " source=" + reactiveSource.getName());

        if (mReactiveSources.addIfAbsent(reactiveSource)) {
            vv(this, mOrigin, reactiveSource.getName() + " says hello: reason=" + reason);
        } else {
            dd(this, mOrigin, "Did you say hello several times or create some other mess? Upchain says hello, but we already have a hello from \"" + reactiveSource.getName() + "\" at \"" + getName() + "\"");
        }
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void unsubscribeSource(
            @NonNull @nonnull final String reason,
            @NonNull @nonnull final IReactiveSource<Bitmap> reactiveSource) {
        assertNotNull(mOrigin);
        if (mReactiveSources.remove(reactiveSource)) {
            vv(this, mOrigin, "Upchain says goodbye: reason=" + reason + " reactiveSource=" + reactiveSource.getName());
            reactiveSource.unsubscribe(reason, this);
        } else {
            throwIllegalStateException(this, mOrigin, "Upchain says goodbye, reason=" + reason + ", but upchain \"" + reactiveSource.getName() + "\" is not currently subscribed to \"" + getName() + "\"");
        }
    }

    @Override // IReactiveTarget
    public void unsubscribeAllSources(@NonNull @nonnull final String reason) {

        for (IReactiveSource<Bitmap> reactiveSource : mReactiveSources) {
            reactiveSource.unsubscribeAll(reason);
        }
    }

    @Override // INamed
    @NonNull
    @nonnull
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
