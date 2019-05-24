package io.sugo.heatmap;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * create by chenyuyi on 2019/4/17
 */
public class SugoHeatMapManager {
    private Context mContext;
    private static SugoHeatMapManager instance;
    private static String TAG = "SugoHeatMapManager";
    SugoHeatMapCallbacks callbacks;
    private SugoHeatMapManager(Context mContext) {
        super();
        this.mContext=mContext;
    }

    public static SugoHeatMapManager getInstance(Context context){
        try{
            if (instance==null){
                instance = new SugoHeatMapManager(context);
            }
            return instance;
        }catch (Exception e){
            Log.e(TAG, "getInstance: "+e.toString());
            return null;
        }

    }

    public void bindingHeatMapFunction(){
        try{
            final Application app = (Application) mContext.getApplicationContext();
            if (callbacks==null){
                callbacks = new SugoHeatMapCallbacks();
                app.registerActivityLifecycleCallbacks(callbacks);
            }
        }catch (Exception e){
            Log.e(TAG, "bindingHeatMapFunction: "+e.toString());
            return ;
        }


    }
}
