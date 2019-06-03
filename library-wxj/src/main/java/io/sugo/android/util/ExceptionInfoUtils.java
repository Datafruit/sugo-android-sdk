package io.sugo.android.util;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import io.sugo.android.mpmetrics.SGConfig;
import io.sugo.android.mpmetrics.SystemInformation;

/**
 * create by chenyuyi on 2019/5/28
 */
public class ExceptionInfoUtils {
    public static String EVENTNAME = "SugoSdkException";


    public static JSONObject ExceptionInfo(Context mContext,Exception exception){
        JSONObject props = new JSONObject();
        SGConfig config = SGConfig.getInstance(mContext);
        SystemInformation mSystemInformation = new SystemInformation(mContext);
        final String token = config.getToken();
        final String projectId = config.getProjectId();
        final String appVersion = mSystemInformation.getAppVersionName();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String text = sw.toString();
        try {
            props.put("token", token);
            props.put("projectId",projectId);
            props.put("sdkVersion",SGConfig.VERSION);
            props.put("appVersion",appVersion);
            props.put("deviceId",mSystemInformation.getDeviceId());
            props.put("systemVersion",android.os.Build.VERSION.RELEASE);
            props.put("PhoneModel",android.os.Build.MODEL);
            props.put("phoneManufacturer",android.os.Build.BRAND);
            props.put("exception",text);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return props;
    }
}
