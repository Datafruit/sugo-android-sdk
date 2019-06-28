package io.sugo.heatmap;


import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xwalk.core.XWalkView;

import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.android.mpmetrics.SugoPageManager;
import io.sugo.android.mpmetrics.SugoWebEventListener;
import io.sugo.android.viewcrawler.ViewCrawler;


/**
 * create by chenyuyi on 2019/4/17
 */
public class SugoHeatMapCallbacks implements Application.ActivityLifecycleCallbacks {

    private boolean mIsLaunching = true;     // 是否启动中
    private LinearLayout mDummyView;
    private static String TAG = "SugoHeatMapCallbacks";

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            SharedPreferences preferences = activity.getSharedPreferences(ViewCrawler.ISHEATMAPFUNC, Context.MODE_PRIVATE);
            boolean isHeatMapFunc = preferences.getBoolean(ViewCrawler.ISHEATMAPFUNC, false);
            if (isHeatMapFunc && SugoPageManager.getInstance().isOpenHeatMapFunc())
                addOnclickPointListener(activity);
        } catch (Exception e) {
            Log.e(TAG, "onActivityResumed: " + e.toString());
        }

    }

    private void addOnclickPointListener(final Activity activity) {
        try {
            if (mDummyView == null) {
                mDummyView = new LinearLayout(activity.getApplication());
            } else {
                return;
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
                            try{
                                double x = event.getX();
                                double y = event.getY();
                                int serialNum = calculateTouchArea(activity, (float) x, (float) y);
                                SugoAPI sugoAPI = SugoAPI.getInstance(activity);
                                Map<String, Object> values = new HashMap<String, Object>();
                                values.put("onclick_point", serialNum);//对应底部按钮标签名
                                String activityname = null;
                                activityname = activity.getClass().getName();
                                if (SugoWebEventListener.sCurrentWebView.size()>0){
                                    for (WebView value :SugoWebEventListener.sCurrentWebView ) {
                                        if (value.getVisibility() == View.VISIBLE){
                                            int hashcode = value.hashCode();
                                            if(SugoWebEventListener.webViewUrlMap.containsKey(hashcode)){
                                                activityname = SugoWebEventListener.webViewUrlMap.get(hashcode);
                                                break;
                                            }
                                        }
                                    }
                                }else if (SugoWebEventListener.sCurrentXWalkView.size()>0){
                                    for (XWalkView value :SugoWebEventListener.sCurrentXWalkView){
                                        if (value.getVisibility() == View.VISIBLE){
                                            int hashcode = value.hashCode();
                                            if(SugoWebEventListener.webViewUrlMap.containsKey(hashcode)){
                                                activityname = SugoWebEventListener.webViewUrlMap.get(hashcode);
                                                break;
                                            }
                                        }
                                    }
                                }

                                values.put("path_name", activityname);
                                if (isSubmitPoinWithPage(activityname)) {
                                    sugoAPI.trackMap("屏幕点击", values);
                                }
                                return false;
                            }catch (Exception e){
                                Log.e(TAG, "mDummyView.setOnTouchListener: " + e.toString());
                                return false;
                            }

                        }
                    });
            mWindowManager.addView(mDummyView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean isSubmitPoinWithPage(String page) {
        try {
            JSONObject pageInfo = SugoPageManager.getInstance().getCurrentPageInfo(page);
            if (pageInfo == null) {
                return false;
            }
            boolean isSubmitPoint = pageInfo.optBoolean("isSubmitPoint");
            if (isSubmitPoint) return true;
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    private int calculateTouchArea(final Activity activity, float x, float y) {
        try {
            int columnNum = 36;
            int lineNum = 64;
            int statusBarHeight = getStatusBarHeight(activity);
            float areaWidth = getScreenHeight(activity, 0, columnNum);
            float areaHeight = getScreenHeight(activity, 1, lineNum);
            float columnSerialValue = x / areaWidth;
            float lineNumSerialValue = (y + statusBarHeight) / areaHeight;
            int columnSerialNum = (columnSerialValue - (int) columnSerialValue) >= 0 ? (int) columnSerialValue + 1 : (int) columnSerialValue;
            int lineNumSerialNum = (lineNumSerialValue - (int) lineNumSerialValue) > 0 ? (int) lineNumSerialValue : (int) lineNumSerialValue - 1;
            int serialNum = columnSerialNum + lineNumSerialNum * columnNum;
            if (x == 0) {
                serialNum += 1;
            }
            return serialNum;
        } catch (Exception e) {
            Log.e(TAG, "calculateTouchArea: " + e.toString());
            return 0;
        }
    }

    private float getScreenHeight(final Activity activity, int type, int distance) {
        try {
            WindowManager wm = (WindowManager) activity.getApplication().getSystemService(Context.WINDOW_SERVICE);

            int statusBarHeight = getStatusBarHeight(activity);

            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            if (type == 0) {
                return dm.widthPixels / distance;
            } else {
                return (dm.heightPixels + statusBarHeight) / distance;
            }
        } catch (Exception e) {
            Log.e(TAG, "getScreenHeight: " + e.toString());
            return 0;
        }
    }

    private int getStatusBarHeight(final Activity activity) {
        try {
            int result = 0;
            int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = activity.getResources().getDimensionPixelSize(resourceId);
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "getStatusBarHeight: " + e.toString());
            return 0;
        }

    }


    @Override
    public void onActivityPaused(Activity activity) {
        try {
            if (mDummyView == null) return;
            WindowManager mWindowManager = (WindowManager) activity.getApplication().getSystemService(Context.WINDOW_SERVICE);
            mWindowManager.removeView(mDummyView);
            mDummyView = null;
        } catch (Exception e) {
            Log.e(TAG, "onActivityPaused: " + e.toString());
        }

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
