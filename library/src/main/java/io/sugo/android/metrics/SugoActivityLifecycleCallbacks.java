package io.sugo.android.metrics;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class SugoActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

    private static final int CHECK_DELAY = 1000;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mCheckInBackground;
    private boolean mIsForeground = false;
    private boolean mPaused = true;
    private final SugoAPI mSugoAPI;
    private final SGConfig mConfig;

    // 是否启动中
    private boolean mIsLaunching = true;
    private HashSet<Activity> mDisableActivities;

    public SugoActivityLifecycleCallbacks(SugoAPI sugoAPI, SGConfig config) {
        mSugoAPI = sugoAPI;
        mDisableActivities = new HashSet<>();
        mConfig = config;

        // 第一个界面正在启动
        mSugoAPI.track("启动");
        mSugoAPI.timeEvent("APP停留");
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= SGConfig.UI_FEATURES_MIN_API && mConfig.getAutoShowSugoUpdates()) {
            if (!activity.isTaskRoot()) {
                return; // No checks, no nothing.
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mPaused = false;
        boolean wasBackground = !mIsForeground;
        mIsForeground = true;

        if (mCheckInBackground != null) {
            mHandler.removeCallbacks(mCheckInBackground);
            mCheckInBackground = null;
        }

        if (wasBackground && !mIsLaunching) {
            // App is in foreground now
            // App 从 background 状态回来，是被唤醒
            JSONObject props = new JSONObject();
            try {
                props.put(SGConfig.FIELD_PAGE, activity.getClass().getCanonicalName());
                props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
                        .getCurrentPageName(activity.getClass().getCanonicalName()));
                props.put(SGConfig.FIELD_PAGE_CATEGORY, SugoPageManager.getInstance()
                        .getCurrentPageCategory(activity.getClass().getCanonicalName()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSugoAPI.track("唤醒", props);
        }

        if (!mDisableActivities.contains(activity)
                && SGConfig.getInstance(activity.getApplicationContext()).isEnablePageEvent()) {
            try {
                JSONObject props = new JSONObject();
                props.put(SGConfig.FIELD_PAGE, activity.getClass().getCanonicalName());
                props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
                        .getCurrentPageName(activity.getClass().getCanonicalName()));
                props.put(SGConfig.FIELD_PAGE_CATEGORY, SugoPageManager.getInstance()
                        .getCurrentPageCategory(activity.getClass().getCanonicalName()));
                mSugoAPI.track("浏览", props);
                mSugoAPI.timeEvent("窗口停留");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // 第一个界面已经显示完毕
        if (mIsLaunching) {
            mIsLaunching = false;
        }
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        mPaused = true;
        if (mCheckInBackground != null) {
            mHandler.removeCallbacks(mCheckInBackground);
            mCheckInBackground = null;
        }
        mHandler.postDelayed(mCheckInBackground = new Runnable() {
            @Override
            public void run() {
                // 延迟一段时间后检测 APP 没有处于前台的话，那就是【后台】状态
                if (mIsForeground && mPaused) {
                    mIsForeground = false;
                    JSONObject props = new JSONObject();
                    try {
                        props.put(SGConfig.FIELD_PAGE, activity.getClass().getCanonicalName());
                        props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
                                .getCurrentPageName(activity.getClass().getCanonicalName()));
                        props.put(SGConfig.FIELD_PAGE_CATEGORY, SugoPageManager.getInstance()
                                .getCurrentPageCategory(activity.getClass().getCanonicalName()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // App 进入后台运行状态
                    mSugoAPI.track("后台", props);
                    mSugoAPI.flush();
                }
            }
        }, CHECK_DELAY);
        if (!mDisableActivities.contains(activity)
                && SGConfig.getInstance(activity.getApplicationContext()).isEnablePageEvent()) {
            try {
                JSONObject props = new JSONObject();
                props.put(SGConfig.FIELD_PAGE, activity.getClass().getCanonicalName());
                props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
                        .getCurrentPageName(activity.getClass().getCanonicalName()));
                props.put(SGConfig.FIELD_PAGE_CATEGORY, SugoPageManager.getInstance()
                        .getCurrentPageCategory(activity.getClass().getCanonicalName()));
                mSugoAPI.track("窗口停留", props);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // TODO: 2017/3/15 此处有 BUG （比如启动的 Activity 调用第二个 Activity 后 finish 自己 ）
        // 最后一个被摧毁的 Activity，是应用被退出
        if (activity.isTaskRoot()) {
            if (mCheckInBackground != null) {
                mHandler.removeCallbacks(mCheckInBackground);
                mCheckInBackground = null;
            }     // 程序正在退出，避免 后台 事件

            JSONObject props = new JSONObject();
            try {
                props.put(SGConfig.FIELD_PAGE, activity.getClass().getCanonicalName());
                props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
                        .getCurrentPageName(activity.getClass().getCanonicalName()));
                props.put(SGConfig.FIELD_PAGE_CATEGORY, SugoPageManager.getInstance()
                        .getCurrentPageCategory(activity.getClass().getCanonicalName()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSugoAPI.track("退出", props);
            mSugoAPI.track("APP停留");
            mSugoAPI.flush();
        }
        mDisableActivities.remove(activity);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    void disableTraceActivity(Activity activity) {
        mDisableActivities.add(activity);
    }

}
