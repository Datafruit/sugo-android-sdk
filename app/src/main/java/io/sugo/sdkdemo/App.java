package io.sugo.sdkdemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * @author Administrator
 * @date 2017/3/15
 */

public class App extends Application {

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate() {
        super.onCreate();

    }

}
