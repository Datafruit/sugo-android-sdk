package io.sugo.android.mpmetrics;

import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Created by fengxj on 11/7/16.
 */

public class SugoWebEventListener {
    private final SugoAPI sugoAPI;
    private static Map<String, JSONArray> eventBindingsMap = new HashMap<String, JSONArray>();

    SugoWebEventListener(SugoAPI sugoAPI) {
        this.sugoAPI = sugoAPI;
    }

    @JavascriptInterface
    public void eventOnAndroid(String eventId, String eventName, String props) {
        try {
            JSONObject jsonObject = new JSONObject(props);
            sugoAPI.track(eventId, eventName, jsonObject);
        } catch (JSONException e) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("text", e.toString());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            sugoAPI.track("Exception", jsonObject);
        }

    }

    public static void bindEvents(String token, JSONArray eventBindings) {
        eventBindingsMap.put(token, eventBindings);
    }

    public static JSONArray getBindEvents(String token){
        return eventBindingsMap.get(token);
    }
}
