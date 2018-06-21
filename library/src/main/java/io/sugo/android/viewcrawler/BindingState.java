package io.sugo.android.viewcrawler;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.sugo.android.metrics.SGConfig;

/**
 * 执行绑定以及管理绑定事件的生命周期
 * SDK 可以替换 app 的所有绑定事件 {@link BindingState#setBindings(java.util.Map)}
 * SDK 可以接受 activity 是否还存在的通知 {@link BindingState#add(android.app.Activity)} and {@link BindingState#remove(android.app.Activity)}
 */
public class BindingState extends UIThreadSet<Activity> {

    @SuppressWarnings("unused")
    private static final String LOGTAG = "SugoAPI.BindingState";

    private final Handler mUiThreadHandler;
    private final Map<String, List<ViewVisitor>> mAllIntendedBindings;
    private final Set<EditBinding> mCurrentEditBindings;

    public BindingState() {
        mUiThreadHandler = new Handler(Looper.getMainLooper());
        mAllIntendedBindings = new HashMap<String, List<ViewVisitor>>();
        mCurrentEditBindings = new HashSet<EditBinding>();
    }

    /**
     * Should be called whenever a new Activity appears in the application.
     */
    @Override
    public void add(Activity newOne) {
        super.add(newOne);
        applyBindingOnUiThread();
    }

    /**
     * Should be called whenever an activity leaves the application, or is otherwise no longer relevant to our edits.
     */
    @Override
    public void remove(Activity oldOne) {
        super.remove(oldOne);
    }

    public void destroyBindings(Activity activity) {
        final String activityName = activity.getClass().getCanonicalName();
        Iterator<EditBinding> iterator = mCurrentEditBindings.iterator();
        while (iterator.hasNext()) {
            EditBinding binding = iterator.next();
            if (binding.getActivityName().equals(activityName)) {
                binding.kill();
                iterator.remove();
            }
        }
    }

    /**
     * Sets the entire set of edits to be applied to the application.
     * <p>
     * Edits are represented by ViewVisitors, batched in a map by the String name of the activity
     * they should be applied to. Edits to apply to all views should be in a list associated with
     * the key {@code null} (Not the string "null", the actual null value!)
     * <p>
     * The given edits will completely replace any existing edits.
     * <p>
     * setBindings can be called from any thread, although the changes will occur (eventually) on the
     * UI thread of the application, and may not appear immediately.
     *
     * @param newBindings A Map from activity name to a list of edits to apply
     */
    // Must be thread-safe
    public void setBindings(Map<String, List<ViewVisitor>> newBindings) {
        // Delete images that are no longer needed

        synchronized (mCurrentEditBindings) {
            for (final EditBinding stale : mCurrentEditBindings) {
                stale.kill();
            }
            mCurrentEditBindings.clear();
        }

        synchronized (mAllIntendedBindings) {
            mAllIntendedBindings.clear();
            mAllIntendedBindings.putAll(newBindings);
        }

        applyBindingOnUiThread();
    }

    /**
     * 到主线程执行绑定过程
     */
    private void applyBindingOnUiThread() {
        if (Thread.currentThread() == mUiThreadHandler.getLooper().getThread()) {
            applyIntendedBindings();
        } else {
            mUiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    applyIntendedBindings();
                }
            });
        }
    }

    /**
     * Must be called on UI Thread
     * 准备好绑定事件
     * 遍历已存在的 activity 以及通配事件
     */
    private void applyIntendedBindings() {
        for (final Activity activity : getAll()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (activity.isDestroyed()) {
                    if (SGConfig.DEBUG) {
                        Log.i(LOGTAG, activity.getLocalClassName() + " is destroyed, will not apply bindings");
                    }
                    return;
                }
            }
            if (activity.isFinishing()) {
                if (SGConfig.DEBUG) {
                    Log.i(LOGTAG, activity.getLocalClassName() + " is finishing, will not apply bindings");
                }
                return;
            }
            final String activityName = activity.getClass().getCanonicalName();
            final View rootView = activity.getWindow().getDecorView().getRootView();

            final List<ViewVisitor> specificChanges;
            final List<ViewVisitor> wildcardChanges;
            synchronized (mAllIntendedBindings) {
                specificChanges = mAllIntendedBindings.get(activityName);
                wildcardChanges = mAllIntendedBindings.get(null);
            }

            if (null != specificChanges) {
                applyChangesFromList(activityName, rootView, specificChanges);
            }

            if (null != wildcardChanges) {
                applyChangesFromList(activityName, rootView, wildcardChanges);
            }
        }
    }

    /**
     * Must be called on UI Thread
     * 开始绑定所有准备好的绑定事件
     *
     * @param rootView
     * @param changes
     */
    private void applyChangesFromList(String activityName, View rootView, List<ViewVisitor> changes) {
        synchronized (mCurrentEditBindings) {
            final int size = changes.size();
            for (int i = 0; i < size; i++) {
                final ViewVisitor visitor = changes.get(i);

                boolean exist = false;
                for (EditBinding binded : mCurrentEditBindings) {
                    if (binded.getViewVisitor() == visitor) {
                        exist = true;
                    }
                }
                if (!exist) {
                    final EditBinding binding = new EditBinding(activityName, rootView, visitor, mUiThreadHandler);
                    mCurrentEditBindings.add(binding);
                }
            }
        }
    }

    /**
     * 这是一组 binding 和 view 的绑定，必须在 UI 线程实例化和运行
     */
    private static class EditBinding implements ViewTreeObserver.OnGlobalLayoutListener, Runnable {

        private volatile boolean mDying;
        private boolean mAlive;
        private final WeakReference<View> mViewRoot;
        private final ViewVisitor mViewVisitor;
        private final Handler mUIHandler;
        private final String mActivityName;

        public EditBinding(String activityName, View viewRoot, ViewVisitor viewVisitor, Handler uiThreadHandler) {
            this.mActivityName = activityName;
            this.mViewVisitor = viewVisitor;
            mViewRoot = new WeakReference<View>(viewRoot);
            mUIHandler = uiThreadHandler;
            mAlive = true;
            mDying = false;

            final ViewTreeObserver observer = viewRoot.getViewTreeObserver();
            if (observer.isAlive()) {
                // 当 ViewTree 改变时，回调 run 重新绑定
                observer.addOnGlobalLayoutListener(this);
            }
            run();
        }

        @Override
        public void onGlobalLayout() {
            run();
        }

        @Override
        public void run() {
            if (!mAlive) {
                return;
            }

            final View viewRoot = mViewRoot.get();
            if (null == viewRoot || mDying) {
                cleanUp();
                return;
            }
            // ELSE View is alive and we are alive
            // 如果已经绑定了事件，不需要再去查找了
            if (!mViewVisitor.isBinded()) {
                // 找到这个 View ，并绑定对应的 View 观察者
                // ViewVisitor 内部执行的代码，将替换该 View 的 AccessibilityDelegate 实现监听
                mViewVisitor.visit(viewRoot);
            }
            mUIHandler.removeCallbacks(this);
            mUIHandler.postDelayed(this, 1000);
        }

        public void kill() {
            mDying = true;
            mUIHandler.post(this);
        }

        /**
         * 清除 Listener
         */
        @SuppressWarnings("deprecation")
        private void cleanUp() {
            if (mAlive) {
                final View viewRoot = mViewRoot.get();
                if (null != viewRoot) {
                    final ViewTreeObserver observer = viewRoot.getViewTreeObserver();
                    if (observer.isAlive()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            observer.removeOnGlobalLayoutListener(this);
                        } else {
                            observer.removeGlobalOnLayoutListener(this);
                        }
                    }
                }
                mViewVisitor.cleanup();
            }
            mAlive = false;
        }

        public String getActivityName() {
            return mActivityName;
        }

        public ViewVisitor getViewVisitor() {
            return mViewVisitor;
        }
    }

}
