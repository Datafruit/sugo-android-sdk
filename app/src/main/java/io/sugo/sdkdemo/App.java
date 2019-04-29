package io.sugo.sdkdemo;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

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
        SugoAPI.startSugo(this, SGConfig.getInstance(this).setSugoEnable(true).logConfig());
        SugoAPI instance = SugoAPI.getInstance(this);
        SugoHeatMapManager manager = SugoHeatMapManager.getInstance(this);
        manager.bindingHeatMapFunction();
        JSONObject jsonObject= new JSONObject();
        try {
            jsonObject.put("event_label", "过滤的数据(只包含类型不符合匹配规则的数据)上报到kafka的一个新的topic字段值匹配规则是，如果数字类型字段传了字符类型就设置为0数据字段值长度大于100个字符就认为是过大，截取前面的100个字符过滤的数据(只包含类型不符合匹配规则的数据)上报到kafka的一个新的topic");
            instance.track("test", jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
