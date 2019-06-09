package io.sugo.android.util;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.sugo.android.mpmetrics.SGConfig;
import io.sugo.android.mpmetrics.SystemInformation;

/**
 * create by chenyuyi on 2019/5/28
 */
public class ExceptionInfoUtils {
    public static String EVENTNAME = "SugoSdkException";
    public static final JSONObject obj = new JSONObject();

    public static JSONObject ExceptionInfo(Context mContext, Exception exception) {
        try {
            JSONObject props = new JSONObject();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            String text = sw.toString();
            props.put("exception", text);
        } catch (Exception e) {
            Log.e("SUGO_EXECPTION", "" , e);
        }
        return obj;
    }

    public static JSONObject ExceptionInfo2(Context mContext, Bundle configBundle, Exception exception) {
        try {

            JSONObject props = new JSONObject();
            SystemInformation mSystemInformation = new SystemInformation(mContext);
            final String token = configBundle.getString("io.sugo.android.SGConfig.token");
            final String appVersion = mSystemInformation.getAppVersionName();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            String text = sw.toString();


            String projectId = configBundle.getString("io.sugo.android.SGConfig.ProjectId");

            props.put("token", token);
            props.put("projectId", projectId);
            props.put("sdkVersion", SGConfig.VERSION);
            props.put("appVersion", appVersion);
            props.put("deviceId", mSystemInformation.getDeviceId());
            props.put("systemVersion", android.os.Build.VERSION.RELEASE);
            props.put("PhoneModel", android.os.Build.MODEL);
            props.put("phoneManufacturer", android.os.Build.BRAND);
            props.put("exception", text);
            return props;
        } catch (Exception e) {
            Log.e("SUGO_EXECPTION", "" , e);
        }
        return obj;
    }
}
