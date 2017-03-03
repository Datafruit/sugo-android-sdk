package io.sugo.android.mpmetrics;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import io.sugo.android.viewcrawler.GestureTracker;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
/* package */ class SugoActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mCheckInBackground;
    private boolean mIsForeground = false;
    private boolean mPaused = true;
    public static final int CHECK_DELAY = 1000;
    private final SugoAPI mMpInstance;
    private final SGConfig mConfig;

    private boolean mIsLaunching = true;     // 是否启动中

    public SugoActivityLifecycleCallbacks(SugoAPI mpInstance, SGConfig config) {
        mMpInstance = mpInstance;
        mConfig = config;

        mMpInstance.track("启动");    // 第一个界面正在启动
        mMpInstance.timeEvent("APP停留");
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= SGConfig.UI_FEATURES_MIN_API && mConfig.getAutoShowMixpanelUpdates()) {
            if (!activity.isTaskRoot()) {
                return; // No checks, no nothing.
            }
        }
        new GestureTracker(mMpInstance, activity);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mPaused = false;
        boolean wasBackground = !mIsForeground;
        mIsForeground = true;

        if (mCheckInBackground != null) {
            mHandler.removeCallbacks(mCheckInBackground);
        }

        if (wasBackground && !mIsLaunching) {
            // App is in foreground now
            // App 从 background 状态回来，是被唤醒
            JSONObject props = new JSONObject();
            try {
                props.put(SGConfig.FIELD_PAGE, activity.getPackageName() + "." + activity.getLocalClassName());
                props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
                        .getCurrentPageName(activity.getPackageName() + "." + activity.getLocalClassName()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mMpInstance.track("唤醒", props);
        }

        try {
            JSONObject props = new JSONObject();
            props.put(SGConfig.FIELD_PAGE, activity.getPackageName() + "." + activity.getLocalClassName());
            props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
                    .getCurrentPageName(activity.getPackageName() + "." + activity.getLocalClassName()));
            mMpInstance.track("浏览", props);
            mMpInstance.timeEvent("窗口停留");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mIsLaunching) {
            mIsLaunching = false;    // 第一个界面已经显示完毕
        }
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        mPaused = true;
        if (mCheckInBackground != null) {
            mHandler.removeCallbacks(mCheckInBackground);
        }
        mHandler.postDelayed(mCheckInBackground = new Runnable() {
            @Override
            public void run() {
                if (mIsForeground && mPaused) {
                    mIsForeground = false;
                    JSONObject props = new JSONObject();
                    try {
                        props.put(SGConfig.FIELD_PAGE, activity.getPackageName() + "." + activity.getLocalClassName());
                        props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
                                .getCurrentPageName(activity.getPackageName() + "." + activity.getLocalClassName()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mMpInstance.track("后台", props);        // App 进入后台运行状态
                    mMpInstance.flush();
                }
            }
        }, CHECK_DELAY);

        try {
            JSONObject props = new JSONObject();
            props.put(SGConfig.FIELD_PAGE, activity.getPackageName() + "." + activity.getLocalClassName());
            props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
                    .getCurrentPageName(activity.getPackageName() + "." + activity.getLocalClassName()));
            mMpInstance.track("窗口停留", props);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity.isTaskRoot()) {     // 最后一个被摧毁的 Activity，是应用被退出
            if (mCheckInBackground != null) {
                mHandler.removeCallbacks(mCheckInBackground);
            }     // 程序正在退出，避免 后台 事件

            JSONObject props = new JSONObject();
            try {
                props.put(SGConfig.FIELD_PAGE, activity.getPackageName() + "." + activity.getLocalClassName());
                props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
                        .getCurrentPageName(activity.getPackageName() + "." + activity.getLocalClassName()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mMpInstance.track("退出", props);
            mMpInstance.track("APP停留");
            mMpInstance.flush();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }


}
