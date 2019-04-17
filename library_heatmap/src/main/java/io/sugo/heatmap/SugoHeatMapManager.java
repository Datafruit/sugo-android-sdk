package io.sugo.heatmap;

import android.app.Application;
import android.content.Context;

/**
 * create by chenyuyi on 2019/4/17
 */
public class SugoHeatMapManager {
    private Context mContext;
    private static SugoHeatMapManager instance;
    SugoHeatMapCallbacks callbacks;
    private SugoHeatMapManager(Context mContext) {
        super();
        this.mContext=mContext;
    }

    public static SugoHeatMapManager getInstance(Context context){
        if (instance==null){
            instance = new SugoHeatMapManager(context);
        }
        return instance;
    }

    public void bindingHeatMapFunction(){
        final Application app = (Application) mContext.getApplicationContext();
        if (callbacks==null){
            callbacks = new SugoHeatMapCallbacks();
            app.registerActivityLifecycleCallbacks(callbacks);
        }

    }
}
