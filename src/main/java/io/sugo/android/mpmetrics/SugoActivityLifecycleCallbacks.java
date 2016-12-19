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
    private Runnable check;
    private boolean mIsForeground = false;
    private boolean mPaused = true;
    public static final int CHECK_DELAY = 500;
    private final SugoAPI mMpInstance;
    private final SGConfig mConfig;

    public SugoActivityLifecycleCallbacks(SugoAPI mpInstance, SGConfig config) {
        mMpInstance = mpInstance;
        mConfig = config;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
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
        mPaused = true;
        boolean wasBackground = !mIsForeground;
        mIsForeground = true;

        if (check != null) {
            mHandler.removeCallbacks(check);
        }

        if (wasBackground) {
            // App is in foreground now
        }

        try {
            JSONObject props = new JSONObject();
            props.put("page",activity.getPackageName()+"."+activity.getLocalClassName());
            mMpInstance.track("enter_page_event",props);
            mMpInstance.timeEvent("stay_event");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onActivityPaused(Activity activity) {
        mPaused = true;
        if (check != null) {
            mHandler.removeCallbacks(check);
        }
        mHandler.postDelayed(check = new Runnable(){
            @Override
            public void run() {
                if (mIsForeground && mPaused) {
                    mIsForeground = false;
                    mMpInstance.flush();
                }
            }
        }, CHECK_DELAY);

        try {
            JSONObject props = new JSONObject();
            props.put("page",activity.getPackageName()+"."+activity.getLocalClassName());
            mMpInstance.track("stay_event",props);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onActivityStopped(Activity activity) { }
    @Override
    public void onActivityDestroyed(Activity activity) {}
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }





}
