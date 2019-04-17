package io.sugo.heatmap;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.Map;

import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.android.mpmetrics.SugoWebEventListener;


/**
 * create by chenyuyi on 2019/4/17
 */
public class SugoHeatMapCallbacks implements Application.ActivityLifecycleCallbacks {

    private boolean mIsLaunching = true;     // 是否启动中
    private LinearLayout mDummyView;

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        addOnclickPointListener(activity);
    }

    private void addOnclickPointListener(final Activity activity) {

        if (mDummyView == null) {
            mDummyView = new LinearLayout(activity.getApplication());
//            createView(activity);
        }else{
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



    @Override
    public void onActivityPaused(Activity activity) {

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
