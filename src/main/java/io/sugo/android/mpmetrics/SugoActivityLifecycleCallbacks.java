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

import java.util.ArrayList;
import java.util.List;

import io.sugo.android.viewcrawler.GestureTracker;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
/* package */ class SugoActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable check;
    private boolean mIsForeground = false;
    private boolean mPaused = true;
    public static final int CHECK_DELAY = 500;
    private final SugoAPI mMpInstance;
    private final SGConfig mConfig;

    private final List<Activity> mSurviveActivities = new ArrayList<>();

    public SugoActivityLifecycleCallbacks(SugoAPI mpInstance, SGConfig config) {
        mMpInstance = mpInstance;
        mConfig = config;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mSurviveActivities.add(activity);
        if (mSurviveActivities.size() == 1) {
            JSONObject props = new JSONObject();
            try {
                props.put(SGConfig.FIELD_PAGE, activity.getPackageName() + "." + activity.getLocalClassName());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mMpInstance.track("launch_event", props);    // 启动了第一个 Activity，说明是应用被启动
        }
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

        if (check != null) {
            mHandler.removeCallbacks(check);
        }

        if (wasBackground) {
            // App is in foreground now
            // App 从 background 状态回来，是被唤醒
            JSONObject props = new JSONObject();
            try {
                props.put(SGConfig.FIELD_PAGE, activity.getPackageName() + "." + activity.getLocalClassName());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mMpInstance.track("awake_event", props);
        }

        try {
            JSONObject props = new JSONObject();
            props.put(SGConfig.FIELD_PAGE,activity.getPackageName()+"."+activity.getLocalClassName());
            mMpInstance.track("enter_page_event",props);
            mMpInstance.timeEvent("stay_event");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        mPaused = true;
        if (check != null) {
            mHandler.removeCallbacks(check);
        }
        mHandler.postDelayed(check = new Runnable(){
            @Override
            public void run() {
                if (mIsForeground && mPaused) {
                    mIsForeground = false;
                    JSONObject props = new JSONObject();
                    try {
                        props.put(SGConfig.FIELD_PAGE, activity.getPackageName() + "." + activity.getLocalClassName());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mMpInstance.track("in_background_event", props);        // App 进入后台运行状态
                    mMpInstance.flush();
                }
            }
        }, CHECK_DELAY);

        try {
            JSONObject props = new JSONObject();
            props.put(SGConfig.FIELD_PAGE,activity.getPackageName()+"."+activity.getLocalClassName());
            mMpInstance.track("stay_event",props);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) { }

    @Override
    public void onActivityDestroyed(Activity activity) {
        mSurviveActivities.remove(activity);
        if (mSurviveActivities.isEmpty()) {
            JSONObject props = new JSONObject();
            try {
                props.put(SGConfig.FIELD_PAGE, activity.getPackageName() + "." + activity.getLocalClassName());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mMpInstance.track("exit_event", props);     // 最后一个被摧毁的 Activity，是应用被退出
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }


}
