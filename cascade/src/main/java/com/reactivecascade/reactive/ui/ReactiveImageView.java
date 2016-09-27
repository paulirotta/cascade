/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.reactive.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.reactivecascade.functional.ImmutableValue;
import com.reactivecascade.i.IAsyncOrigin;
import com.reactivecascade.i.IReactiveSource;
import com.reactivecascade.i.IReactiveTarget;
import com.reactivecascade.i.NotCallOrigin;
import com.reactivecascade.util.AssertUtil;
import com.reactivecascade.util.RCLog;

import java.util.concurrent.CopyOnWriteArraySet;

import static com.reactivecascade.Async.UI;
import static com.reactivecascade.Async.isUiThread;

/**
 * An ImageView which can be directly bound to change the screen when an up-chain from changes
 */
public class ReactiveImageView extends ImageView implements IReactiveTarget<Bitmap>, IAsyncOrigin {
    @NonNull
    private final ImmutableValue<String> mOrigin = isInEditMode() ? RCLog.DEFAULT_ORIGIN : RCLog.originAsync();
    private final CopyOnWriteArraySet<IReactiveSource<Bitmap>> reactiveSources = new CopyOnWriteArraySet<>();

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
    public void subSource(
            @NonNull String reason,
            @NonNull IReactiveSource<Bitmap> reactiveSource) {
        RCLog.v(this, "Subscribing ReactiveImageView: reason=" + reason + " source=" + reactiveSource.getName());

        if (reactiveSources.add(reactiveSource)) {
            RCLog.v(this, reactiveSource.getName() + " says hello: reason=" + reason);
        } else {
            RCLog.d(this, "Did you say hello several times or create some other mess? Upchain says hello, but we already have a hello from \"" + reactiveSource.getName() + "\" at \"" + getName() + "\"");
        }
    }

    @Override // IReactiveTarget
    @NotCallOrigin
    public void unsubSource(
            @NonNull String reason,
            @NonNull IReactiveSource<Bitmap> reactiveSource) {
        AssertUtil.assertNotNull(mOrigin);

        if (reactiveSources.remove(reactiveSource)) {
            RCLog.v(this, "Upchain says goodbye: reason=" + reason + " reactiveSource=" + reactiveSource.getName());
            reactiveSource.unsubscribe(reason, this);
        } else {
            RCLog.throwIllegalStateException(this, "Upchain says goodbye, reason=" + reason + ", but upchain \"" + reactiveSource.getName() + "\" is not currently subscribed to \"" + getName() + "\"");
        }
    }

    @Override // IReactiveTarget
    public void unsubAllSources(@NonNull String reason) {
        for (final IReactiveSource<Bitmap> reactiveSource : reactiveSources) {
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
        unsubAllSources("onDetachedFromWindow");

        super.onDetachedFromWindow();
    }

    @NonNull
    @Override
    public ImmutableValue<String> getOrigin() {
        return mOrigin;
    }
}
