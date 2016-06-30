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

import com.reactivecascade.i.IAction;
import com.reactivecascade.i.IBindingContext;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default binding context implementations
 */
public class BindingContextUtil {
    private static final String TAG = BindingContextUtil.class.getSimpleName();

    public static final IBindingContext APP_BINDING_CONTEXT = new IBindingContext() {
        @Override
        public boolean isBindingContextOpen() {
            return true;
        }

        @Override
        public void closeBindingContext() {
            throw new UnsupportedOperationException("AppBindingContext can not be closed");
        }

        @Override
        public void onClose(@NonNull IAction action) {
            throw new UnsupportedOperationException("AppBindingContext can not be closed");
        }
    };

    public static class DefaultBindingContext extends Origin implements IBindingContext {
        private final AtomicBoolean open = new AtomicBoolean(true);
        private final CopyOnWriteArraySet<IAction> onCloseActions = new CopyOnWriteArraySet<>();

        @Override
        public final boolean isBindingContextOpen() {
            return open.get();
        }

        @Override
        public final void closeBindingContext() {
            if (open.compareAndSet(true, false)) {
                onBindingContextClose();
            }
        }

        @CallSuper
        public void onClose(@NonNull IAction action) {
            onCloseActions.add(action);
        }

        @CallSuper
        protected void onBindingContextClose() {
            Exception caught = null;

            for (IAction action : onCloseActions) {
                try {
                    action.call();
                } catch (Exception e) {
                    caught = e;
                    RCLog.e(this, "Can not complete all onCloseActions- other onCloseActions will still be attempted", e);
                }
            }

            if (caught != null) {
                throw new IllegalStateException(caught);
            }
        }
    }

    public static class AsyncFragment extends Fragment {
        private final IBindingContext bindingContext = new DefaultBindingContext();
        private IBindingContext pauseResumeBindingContext;

        @NonNull
        public IBindingContext getBindingContext() {
            return bindingContext;
        }

        @Nullable
        @UiThread
        public IBindingContext getPauseResumeBindingContext() {
            return pauseResumeBindingContext;
        }

        @Override
        @UiThread
        public void onResume() {
            super.onResume();

            pauseResumeBindingContext = new DefaultBindingContext();
        }

        @Override
        @UiThread
        public void onPause() {
            pauseResumeBindingContext.closeBindingContext();

            super.onPause();
        }

        @Override
        @UiThread
        public void onDestroy() {
            bindingContext.closeBindingContext();

            super.onDestroy();
        }
    }

    public static abstract class AsyncFragmentActivity extends FragmentActivity {
        private final IBindingContext bindingContext = new DefaultBindingContext();
        private IBindingContext pauseResumeBindingContext;

        @NonNull
        public IBindingContext getBindingContext() {
            return bindingContext;
        }

        @Nullable
        @UiThread
        public IBindingContext getPauseResumeBindingContext() {
            return pauseResumeBindingContext;
        }

        @Override
        @UiThread
        public void onResume() {
            super.onResume();

            pauseResumeBindingContext = new DefaultBindingContext();
        }

        @Override
        @UiThread
        public void onPause() {
            pauseResumeBindingContext.closeBindingContext();

            super.onPause();
        }

        @Override
        @UiThread
        public void onStop() {
            bindingContext.closeBindingContext();

            super.onStop();
        }
    }
}
