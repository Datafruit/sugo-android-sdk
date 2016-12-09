package io.sugo.android.mpmetrics;

import android.webkit.JavascriptInterface;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

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
        sugoAPI.track(eventId, eventName, null);
    }

    public static void bindEvents(String token, JSONArray eventBindings) {
        eventBindingsMap.put(token, eventBindings);
    }

    public static JSONArray getBindEvents(String token){
        return eventBindingsMap.get(token);
    }
}
