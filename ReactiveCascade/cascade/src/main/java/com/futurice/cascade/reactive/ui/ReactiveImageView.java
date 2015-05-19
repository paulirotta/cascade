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
import android.util.AttributeSet;
import android.widget.ImageView;

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.reactive.IReactiveSource;
import com.futurice.cascade.i.reactive.IReactiveTarget;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.futurice.cascade.Async.*;

/**
 * An ImageView which can be directly bound to change the screen when an up-chain value changes
 */
public class ReactiveImageView extends ImageView implements IReactiveTarget<Bitmap> {
//    private static ConcurrentLinkedQueue<ImmutableValue<String>> INSTANCE_ORIGINS;

//    static {
//        try {
//            INSTANCE_ORIGINS = DEBUG ? new ConcurrentLinkedQueue<>() : null;
//        } catch (Throwable e) {
//            // Ignore in visual editor which lacks the path. No good way to check isInEditMode() at static level but we want this code removed by Proguard
//        }
//    }

    private final ImmutableValue<String> origin = isInEditMode() ? null : originAsync();
    private final CopyOnWriteArrayList<IReactiveSource<Bitmap>> reactiveSources = new CopyOnWriteArrayList<>();

    public ReactiveImageView(Context context) {
        super(context);

//        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.add(origin);
//        }
    }

    public ReactiveImageView(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);

//        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.add(origin);
//        }
    }

    public ReactiveImageView(@NonNull Context context, @NonNull AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

//        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.add(origin);
//        }
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void fire(@NonNull Bitmap bitmap) {
        dd(this, origin, "fire bitmap=");

        if (isUiThread()) {
            setImageBitmap(bitmap);
        } else {
            UI.execute((IAction) () -> setImageBitmap(bitmap));
        }
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void fireNext(@NonNull Bitmap bitmap) {
        //FIXME is not really fire next

        fire(bitmap);
    }

    @Override
    public void setImageURI(@NonNull Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void subscribeSource(@NonNull String reason, @NonNull IReactiveSource<Bitmap> reactiveSource) {
        dd(this, origin, "Subscribing ReactiveImageView: reason=" + reason + " source=" + reactiveSource.getName());
        if (reactiveSources.addIfAbsent(reactiveSource)) {
            vv(this, origin, reactiveSource.getName() + " says hello: reason=" + reason);
        } else {
            dd(this, origin, "Did you say hello several times or create some other mess? Upchain says hello, but we already have a hello from \"" + reactiveSource.getName() + "\" at \"" + getName() + "\"");
        }
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void unsubscribeSource(@NonNull String reason, @NonNull IReactiveSource<Bitmap> reactiveSource) {
        if (reactiveSources.remove(reactiveSource)) {
            vv(this, origin, "Upchain says goodbye: reason=" + reason + " reactiveSource=" + reactiveSource.getName());
            reactiveSource.unsubscribe(reason, this);
        } else {
            throwIllegalStateException(this, origin, "Upchain says goodbye, reason=" + reason + ", but upchain \"" + reactiveSource.getName() + "\" is not currently subscribed to \"" + getName() + "\"");
        }
    }

    @Override // IReactiveTarget
    public void unsubscribeAllSources(@NonNull String reason) {
        final Iterator<IReactiveSource<Bitmap>> iterator = reactiveSources.iterator();

        while (iterator.hasNext()) {
            iterator.next().unsubscribeAll(reason);
        }
    }

    @Override // INamed
    public String getName() {
        return "ReactiveTextView-" + getId();
    }

    @Override // View
    public void onDetachedFromWindow() {
        unsubscribeAllSources("onDetachedFromWindow");
    }

//    @Override // Object
//    public void finalize() {
//        if (INSTANCE_ORIGINS != null) {
//            INSTANCE_ORIGINS.remove(this);
//        }
//    }
}