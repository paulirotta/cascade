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

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.IAsyncOrigin;
import com.futurice.cascade.i.IReactiveSource;
import com.futurice.cascade.i.IReactiveTarget;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.util.AssertUtil;
import com.futurice.cascade.util.RCLog;

import java.util.concurrent.CopyOnWriteArrayList;

import static com.futurice.cascade.Async.UI;
import static com.futurice.cascade.Async.isUiThread;

/**
 * An ImageView which can be directly bound to change the screen when an up-chain from changes
 */
public class ReactiveImageView extends ImageView implements IReactiveTarget<Bitmap>, IAsyncOrigin {
    @NonNull
    private final ImmutableValue<String> mOrigin = isInEditMode() ? RCLog.DEFAULT_ORIGIN : RCLog.originAsync();
    private final CopyOnWriteArrayList<IReactiveSource<Bitmap>> mReactiveSources = new CopyOnWriteArrayList<>();

    public ReactiveImageView(@NonNull Context context) {
        super(context);
    }

    public ReactiveImageView(
            @NonNull Context context,
            @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    public ReactiveImageView(
            @NonNull Context context,
            @NonNull AttributeSet attrs,
            @StyleRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void fire(@NonNull Bitmap bitmap) {
        AssertUtil.assertNotNull(mOrigin);
        RCLog.v(this, "fire bitmap");

        if (isUiThread()) {
            setImageBitmap(bitmap);
        } else {
            UI.execute(() ->
                    setImageBitmap(bitmap));
        }
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void fireNext(@NonNull Bitmap bitmap) {
        //FIXME is not really fire next

        fire(bitmap);
    }

    @Override
    public void setImageURI(@Nullable Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void subscribeSource(
            @NonNull String reason,
            @NonNull IReactiveSource<Bitmap> reactiveSource) {
        RCLog.v(this, "Subscribing ReactiveImageView: reason=" + reason + " source=" + reactiveSource.getName());

        if (mReactiveSources.addIfAbsent(reactiveSource)) {
            RCLog.v(this, reactiveSource.getName() + " says hello: reason=" + reason);
        } else {
            RCLog.d(this, "Did you say hello several times or create some other mess? Upchain says hello, but we already have a hello from \"" + reactiveSource.getName() + "\" at \"" + getName() + "\"");
        }
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void unsubscribeSource(
            @NonNull String reason,
            @NonNull IReactiveSource<Bitmap> reactiveSource) {
        AssertUtil.assertNotNull(mOrigin);

        if (mReactiveSources.remove(reactiveSource)) {
            RCLog.v(this, "Upchain says goodbye: reason=" + reason + " reactiveSource=" + reactiveSource.getName());
            reactiveSource.unsubscribe(reason, this);
        } else {
            RCLog.throwIllegalStateException(this, "Upchain says goodbye, reason=" + reason + ", but upchain \"" + reactiveSource.getName() + "\" is not currently subscribed to \"" + getName() + "\"");
        }
    }

    @Override // IReactiveTarget
    public void unsubscribeAllSources(@NonNull String reason) {
        for (final IReactiveSource<Bitmap> reactiveSource : mReactiveSources) {
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

    @NonNull
    @Override
    public ImmutableValue<String> getOrigin() {
        return mOrigin;
    }
}
