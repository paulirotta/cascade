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
 *
 * TODO WIP - this class is not yet complete and in-use
 */
public class BindingContextUtil {
    private static final String TAG = BindingContextUtil.class.getSimpleName();

    /**
     * The default implementation of a State-change notification to start and stop data-driven reactive actions
     *
     * @param <T> the type of object which will control these State changes
     */
    public static class DefaultBindingContext<T> implements IBindingContext<T> {
        private final CopyOnWriteArraySet<IActionOne<T>> onOpenActions = new CopyOnWriteArraySet<>();
        private final CopyOnWriteArraySet<IActionOne<T>> onCloseActions = new CopyOnWriteArraySet<>();
        private final AtomicReference<T> bindingContextAR = new AtomicReference<>(null);

        /**
         *
         * @return
         */
        @Override
        @CallSuper
        public boolean isOpen() {
            return bindingContextAR.get() != null;
        }

        /**
         *
         * @param t the type of object controlling this binding's lifecycle
         */
        @Override
        public final void openBindingContext(T t) {
            if (bindingContextAR.compareAndSet(null, t)) {
                doBindingContextStateChangeActions(onOpenActions, t);
            } else {
                RCLog.d(this, "Can not openBindingContext: illegal transition from " + bindingContextAR.get() + " to " + t);
            }
        }

        /**
         *
         * @param t the type of object controlling this binding's lifecyle
         */
        @Override
        public final void closeBindingContext(T t) {
            if (bindingContextAR.compareAndSet(t, null)) {
                doBindingContextStateChangeActions(onCloseActions, t);
            } else {
                RCLog.d(this, "Can not closeBindingContext: illegal transition from " + bindingContextAR.get() + " to " + t);
            }
        }

        /**
         *
         * @param action
         */
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

        /**
         *
         * @param action performed when binding ends
         */
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
                    RCLog.e(this, "Problem during binding context State change actions- any other actions will still be attempted", e);
                }
            }

            if (caught != null) {
                throw new IllegalStateException(caught);
            }
        }
    }

    /**
     * A parent class for a {@link Fragment} that will provide a binding context to automatically
     * pause/resume/close reactive bindings
     */
    public static class AsyncFragment extends Fragment {
        private IBindingContext<Fragment> startStopBindingContext = new DefaultBindingContext<>();
        private IBindingContext<Fragment> pauseResumeBindingContext = new DefaultBindingContext<>();

        /**
         *
         * @return
         */
        @NonNull
        @UiThread
        public final IBindingContext getStartStopBindingContext() {
            return startStopBindingContext;
        }

        /**
         *
         * @return
         */
        @Nullable
        @UiThread
        public final IBindingContext getPauseResumeBindingContext() {
            return pauseResumeBindingContext;
        }

        /**
         *
         */
        @Override
        @UiThread
        @CallSuper
        public void onResume() {
            pauseResumeBindingContext.openBindingContext(this);
            super.onResume();
        }

        /**
         *
         */
        @Override
        @UiThread
        @CallSuper
        public void onPause() {
            pauseResumeBindingContext.closeBindingContext(this);
            pauseResumeBindingContext = new DefaultBindingContext<>();

            super.onPause();
        }

        /**
         *
         */
        @Override
        @UiThread
        @CallSuper
        public void onStop() {
            startStopBindingContext.closeBindingContext(this);
            startStopBindingContext = new DefaultBindingContext<>();

            super.onStop();
        }
    }

    /**
     * A parent class for a {@link FragmentActivity} that will provide a binding context to automatically
     * pause/resume/close reactive bindings
     */
    public static abstract class AsyncFragmentActivity extends FragmentActivity {
        /**
         * Use this binding context if you want your reactive action to end when the fragment closes
         *
         * You should create this type of binding in your {@link Fragment#onStart()} method
         */
        private IBindingContext<FragmentActivity> startStopBindingContext = new DefaultBindingContext<>();
        private IBindingContext<FragmentActivity> pauseResumeBindingContext = new DefaultBindingContext<>();

        /**
         *
         * @return a new binding context each time the {@link FragmentActivity} is stopped
         */
        @NonNull
        @UiThread
        public final IBindingContext<FragmentActivity> getStartStopBindingContext() {
            return startStopBindingContext;
        }

        /**
         * Use this binding context if you want your reactive actions to end when the fragment pauses.
         *
         * You should create this type of binding in your {@link FragmentActivity#onResume()} method
         *
         * @return a new binding context each time the application is paused
         */
        @NonNull
        @UiThread
        public final IBindingContext getPauseResumeBindingContext() {
            return pauseResumeBindingContext;
        }

        /**
         * Run when the binding context goes inactive
         *
         * Bound actions are stopped before the activity pauses
         */
        @Override
        @UiThread
        @CallSuper
        public void onPause() {
            pauseResumeBindingContext.closeBindingContext(this);
            pauseResumeBindingContext = new DefaultBindingContext<>();

            super.onPause();
        }

        /**
         * Run when the binding context returns from an inactive/paused State
         *
         * Bound actions are fired before the activity itself resumes in order to initialize variables
         * for display
         */
        @Override
        @UiThread
        @CallSuper
        public void onResume() {
            pauseResumeBindingContext.openBindingContext(this);
            super.onResume();
        }

        /**
         * Run when the binding context returns from an inactive/paused State
         *
         * Bound actions are fired before the activity itself starts in order to initialize variables
         * for display
         */
        @Override
        @UiThread
        @CallSuper
        public void onStart() {
            startStopBindingContext.openBindingContext(this);
            super.onStart();
        }

        /**
         * Run when the binding context ends
         *
         * Bound actions are stopped before the activity stops
         */
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
