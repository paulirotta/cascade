package com.futurice.cascade.util;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.futurice.cascade.i.IAction;
import com.futurice.cascade.i.IBindingContext;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default binding context implementations
 */
public class BindingContextUtils {
    private static final String TAG = BindingContextUtils.class.getSimpleName();

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
        public IBindingContext onClose(@NonNull IAction action) {
            throw new UnsupportedOperationException("AppBindingContext can not be closed");
        }
    };

    public static class DefaultBindingContext extends Origin implements IBindingContext {
        private final AtomicBoolean open = new AtomicBoolean(true);
        private final CopyOnWriteArrayList<IAction> onCloseActions = new CopyOnWriteArrayList<>();

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
        public IBindingContext onClose(@NonNull IAction action) {
            onCloseActions.add(action);

            return this;
        }

        @CallSuper
        protected void onBindingContextClose() {
            Iterator<IAction> iterator = onCloseActions.iterator();
            Exception caught = null;

            while (iterator.hasNext()) {
                try {
                    iterator.next().call();
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

    public static class AsyncFragmentActivity extends FragmentActivity {
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
