package io.sugo.sdkdemo;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;
import android.util.Log;

import io.sugo.android.metrics.InitSugoCallback;
import io.sugo.android.metrics.SGConfig;
import io.sugo.android.metrics.SugoAPI;


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

            }
        });
    }

}
