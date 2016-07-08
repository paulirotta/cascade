/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.util;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.reactivecascade.i.IActionOne;
import com.reactivecascade.i.IBindingContext;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default binding context implementations
 */
public class BindingContextUtil {
    private static final String TAG = BindingContextUtil.class.getSimpleName();

    public static class DefaultBindingContext<T> implements IBindingContext<T> {
        private final CopyOnWriteArraySet<IActionOne<T>> onOpenActions = new CopyOnWriteArraySet<>();
        private final CopyOnWriteArraySet<IActionOne<T>> onCloseActions = new CopyOnWriteArraySet<>();
        private final AtomicReference<T> bindingContextAR = new AtomicReference<>(null);

        @Override
        @CallSuper
        public boolean isOpen() {
            return bindingContextAR.get() != null;
        }

        @Override
        public final void openBindingContext(T t) {
            if (bindingContextAR.compareAndSet(null, t)) {
                doBindingContextStateChangeActions(onOpenActions, t);
            } else {
                RCLog.d(this, "Can not openBindingContext: illegal transition from " + bindingContextAR.get() + " to " + t);
            }
        }

        @Override
        public final void closeBindingContext(T t) {
            if (bindingContextAR.compareAndSet(t, null)) {
                doBindingContextStateChangeActions(onCloseActions, t);
            } else {
                RCLog.d(this, "Can not closeBindingContext: illegal transition from " + bindingContextAR.get() + " to " + t);
            }
        }

        @Override
        public final void onOpen(@NonNull IActionOne<T> action) {
            T t = bindingContextAR.get();

            if (t != null) {
                // Already open- execute now
                Set<IActionOne<T>> set = new HashSet<>(1);
                set.add(action);
                doBindingContextStateChangeActions(set, t);
            }
            onOpenActions.add(action);
        }

        @Override
        public final void onClose(@NonNull IActionOne<T> action) {
            onCloseActions.add(action);
        }

        private void doBindingContextStateChangeActions(Set<IActionOne<T>> actions, T t) {
            Exception caught = null;

            for (IActionOne<T> action : actions) {
                try {
                    action.call(t);
                } catch (Exception e) {
                    caught = e;
                    RCLog.e(this, "Problem during binding context state change actions- any other actions will still be attempted", e);
                }
            }

            if (caught != null) {
                throw new IllegalStateException(caught);
            }
        }
    }

    public static class AsyncFragment extends Fragment {
        private IBindingContext<Fragment> startStopBindingContext = new DefaultBindingContext<>();
        private IBindingContext<Fragment> pauseResumeBindingContext = new DefaultBindingContext<>();

        @NonNull
        @UiThread
        public final IBindingContext getStartStopBindingContext() {
            return startStopBindingContext;
        }

        @Nullable
        @UiThread
        public final IBindingContext getPauseResumeBindingContext() {
            return pauseResumeBindingContext;
        }

        @Override
        @UiThread
        @CallSuper
        public void onResume() {
            super.onResume();
        }

        @Override
        @UiThread
        @CallSuper
        public void onPause() {
            pauseResumeBindingContext.closeBindingContext(this);
            pauseResumeBindingContext = new DefaultBindingContext<>();

            super.onPause();
        }

        @Override
        @UiThread
        @CallSuper
        public void onStop() {
            startStopBindingContext.closeBindingContext(this);
            startStopBindingContext = new DefaultBindingContext<>();

            super.onStop();
        }
    }

    public static abstract class AsyncFragmentActivity extends FragmentActivity {
        private IBindingContext<FragmentActivity> startStopBindingContext = new DefaultBindingContext<>();
        private IBindingContext<FragmentActivity> pauseResumeBindingContext = new DefaultBindingContext<>();

        @NonNull
        @UiThread
        public final IBindingContext getStartStopBindingContext() {
            return startStopBindingContext;
        }

        @Nullable
        @UiThread
        public final IBindingContext getPauseResumeBindingContext() {
            return pauseResumeBindingContext;
        }

        @Override
        @UiThread
        @CallSuper
        public void onResume() {
            super.onResume();
        }

        @Override
        @UiThread
        @CallSuper
        public void onPause() {
            pauseResumeBindingContext.closeBindingContext(this);
            pauseResumeBindingContext = new DefaultBindingContext<>();

            super.onPause();
        }

        @Override
        @UiThread
        @CallSuper
        public void onStop() {
            startStopBindingContext.closeBindingContext(this);
            startStopBindingContext = new DefaultBindingContext<>();

            super.onStop();
        }
    }
}
