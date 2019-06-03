package io.sugo.sdkdemo;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import io.sugo.android.mpmetrics.InitSugoCallback;
import io.sugo.android.mpmetrics.SGConfig;
import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.heatmap.SugoHeatMapManager;

/**
 * @author Administrator
 * @date 2017/3/15
 */

public class App extends Application {

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate() {
        super.onCreate();
        SugoAPI.startSugo(this, SGConfig.getInstance(this).setSugoEnable(true).logConfig(), new InitSugoCallback() {
            @Override
            public void finish() {
                SugoHeatMapManager manager =    SugoHeatMapManager.getInstance(App.this);//假设在Application中初始化
                manager.bindingHeatMapFunction();

            }
        });

    }

}
