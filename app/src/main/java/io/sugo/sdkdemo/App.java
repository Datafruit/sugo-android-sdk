package io.sugo.sdkdemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 *
 * @author Administrator
 * @date 2017/3/15
 */

public class App extends Application {

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log.i("ActivityLifecycle:", "onActivityCreated:" + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log.i("ActivityLifecycle:", "onActivityStarted:" + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log.i("ActivityLifecycle:", "onActivityResumed:" + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log.i("ActivityLifecycle:", " onActivityPaused:" + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log.i("ActivityLifecycle:", "onActivityStopped:" + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                Log.i("ActivityLifecycle:", "onActivitySaveInstanceState:" + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.i("ActivityLifecycle:", "ActivityDestroyed:" + activity.getClass().getSimpleName());
            }
        });
    }

}
