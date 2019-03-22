package io.sugo.android.mpmetrics;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.renderscript.Sampler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        /* package */ class SugoActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mCheckInBackground;
    private boolean mIsForeground = false;
    private boolean mPaused = true;
    public static final int CHECK_DELAY = 1000;
    private final SugoAPI mSugoAPI;
    private final SGConfig mConfig;

    private boolean mIsLaunching = true;     // 是否启动中
    private HashSet<Activity> mDisableActivities;
    private LinearLayout mDummyView;

    public SugoActivityLifecycleCallbacks(SugoAPI sugoAPI, SGConfig config) {
        mSugoAPI = sugoAPI;
        mDisableActivities = new HashSet<>();
        mConfig = config;
        JSONObject props = new JSONObject();
        try {
            props.put(SGConfig.FIELD_PAGE, "启动");
            props.put(SGConfig.FIELD_PAGE_NAME, "启动");
            props.put("app_name", "无限极中国APP");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSugoAPI.track("启动", props);    // 第一个界面正在启动
        mSugoAPI.timeEvent("APP停留");
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        //当activity重新载入时，清除url路径
        SugoWebEventListener.webViewUrl = null;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (Build.VERSION.SDK_INT >= SGConfig.UI_FEATURES_MIN_API && mConfig.getAutoShowMixpanelUpdates()) {
            if (!activity.isTaskRoot()) {
                return; // No checks, no nothing.
            }
        }
    }

    class SystemInfo {   //获取设备划分区域的宽高
        final double itemHeight;
        final double itemWidth;

        SystemInfo(final Activity activity) {
            SystemInformation msystemInformation = new SystemInformation(activity.getApplication());
            final DisplayMetrics displayMetrics = msystemInformation.getDisplayMetrics();
            float h = displayMetrics.heightPixels;
            float w = displayMetrics.widthPixels;
            itemHeight = h / 64;
            itemWidth = w / 36;
        }

        public double getItemHeight() {
            return itemHeight;
        }

        public double getItemWidth() {
            return itemWidth;
        }
    }

    @Override
    public void onActivityResumed(final Activity activity) {
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
                props.put(SGConfig.FIELD_PAGE, activity.getClass().getCanonicalName());
                props.put(SGConfig.FIELD_PAGE_NAME, "唤醒");
                props.put(SGConfig.FIELD_PAGE_CATEGORY, SugoPageManager.getInstance()
                        .getCurrentPageCategory(activity.getClass().getCanonicalName()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSugoAPI.track("唤醒", props);
            mSugoAPI.timeEvent("APP停留");

        }
        if (!mDisableActivities.contains(activity) && mSugoAPI.getConfig().isEnablePageEvent()) {
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

        if (mIsLaunching) {
            mIsLaunching = false;
        }

        if (mConfig.getSubmitOnclickPointEvent()) {
            addOnclickPointListener(activity);
        }

    }


    private void addOnclickPointListener(final Activity activity) {

        if (mDummyView == null) {
            mDummyView = new LinearLayout(activity.getApplication());
//            createView(activity);
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, /* width */
                1, /* height */
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSPARENT
        );
        params.gravity = Gravity.LEFT | Gravity.TOP;
        WindowManager mWindowManager = (WindowManager) activity.getApplication().getSystemService(Context.WINDOW_SERVICE);
        mDummyView.setLayoutParams(params);
        mDummyView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        double x = event.getX();
                        double y = event.getY();
                        int serialNum = calculateTouchArea(activity, (float) x, (float) y);
                        SugoAPI sugoAPI = SugoAPI.getInstance(activity);
                        Map<String, Object> values = new HashMap<String, Object>();
                        values.put("onclick_point", serialNum);//对应底部按钮标签名
                        String activityname = null;
                        if (SugoWebEventListener.webViewUrl != null) {
                            activityname = SugoWebEventListener.webViewUrl;
                        } else {
                            activityname = activity.getClass().getName();
                        }

                        values.put("path_name", activityname);
                        sugoAPI.trackMap("屏幕点击", values);

                        return false;
                    }
                });
        mWindowManager.addView(mDummyView, params);
    }


    private float getScreenHeight(final Activity activity, int type, int distance) {
        WindowManager wm = (WindowManager) activity.getApplication().getSystemService(Context.WINDOW_SERVICE);

        int statusBarHeight =getStatusBarHeight(activity);

        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        if (type == 0) {
            return dm.widthPixels / distance;
        } else {
            return (dm.heightPixels+statusBarHeight) / distance;
        }
    }

    private int getStatusBarHeight(final Activity activity){
        int result=0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    private int calculateTouchArea(final Activity activity, float x, float y) {
        int columnNum = 36;
        int lineNum = 64;
        int statusBarHeight =getStatusBarHeight(activity);
        float areaWidth = getScreenHeight(activity, 0, columnNum);
        float areaHeight = getScreenHeight(activity, 1, lineNum);
        float columnSerialValue = x / areaWidth;
        float lineNumSerialValue = (y+statusBarHeight )/ areaHeight;
        int columnSerialNum = (columnSerialValue - (int) columnSerialValue) >= 0 ? (int) columnSerialValue + 1 : (int) columnSerialValue;
        int lineNumSerialNum = (lineNumSerialValue - (int) lineNumSerialValue) > 0 ? (int) lineNumSerialValue : (int) lineNumSerialValue - 1;
        int serialNum = columnSerialNum + lineNumSerialNum * columnNum;
        if (x==0){
            serialNum +=1;
        }
        return serialNum;
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        if (mConfig.getSubmitOnclickPointEvent()) {
            WindowManager mWindowManager = (WindowManager) activity.getApplication().getSystemService(Context.WINDOW_SERVICE);
            if (mDummyView != null) {
                mWindowManager.removeViewImmediate(mDummyView);
            }
        }

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
                        props.put(SGConfig.FIELD_PAGE, activity.getClass().getCanonicalName());
                        props.put(SGConfig.FIELD_PAGE_NAME, "后台");
                        props.put(SGConfig.FIELD_PAGE_CATEGORY, SugoPageManager.getInstance()
                                .getCurrentPageCategory(activity.getClass().getCanonicalName()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mSugoAPI.track("后台", props);        // App 进入后台运行状态
                    try {
                        props.put(SGConfig.FIELD_PAGE_NAME, "APP停留");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mSugoAPI.track("APP停留", props);        // App 进入停留状态
                    mSugoAPI.flush();
                }
            }
        }, CHECK_DELAY);

        if (!mDisableActivities.contains(activity) && mSugoAPI.getConfig().isEnablePageEvent()) {
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
        SugoWebEventListener.webViewUrl = null;
        mDisableActivities.remove(activity);
//         // 无限极使用了 代码埋点 ，所以这里注释掉
//        String runningPage = SugoPageManager.getInstance().getCurrentPage(activity.getApplicationContext());
//        String packageName = activity.getApplicationContext().getPackageName();     // 应用包名
//        // 正在运行的 Activity 不是当前应用的包名，说明是回到了其它应用（或 Launcher)
//        // 不是最后一个被摧毁的 Activity，不是应用被退出
//        if (!runningPage.startsWith(packageName) && (activity.isTaskRoot())) {
//            if (mCheckInBackground != null) {
//                mHandler.removeCallbacks(mCheckInBackground);
//            }     // 程序正在退出，避免 后台 事件
//
//            JSONObject props = new JSONObject();
//            try {
//                props.put(SGConfig.FIELD_PAGE, activity.getClass().getCanonicalName());
//                props.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance()
//                        .getCurrentPageName(activity.getClass().getCanonicalName()));
//        props.put(SGConfig.FIELD_PAGE_CATEGORY, SugoPageManager.getInstance()
//                .getCurrentPageCategory(activity.getClass().getCanonicalName()));
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            try {
//                props.put(SGConfig.FIELD_PAGE_NAME, "退出");
//                mSugoAPI.track("退出", props);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            try {
//                props.put(SGConfig.FIELD_PAGE_NAME, "APP停留");
//                mSugoAPI.track("APP停留", props);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
        mSugoAPI.flush();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    void disableTraceActivity(Activity activity) {
        mDisableActivities.add(activity);
    }

}
