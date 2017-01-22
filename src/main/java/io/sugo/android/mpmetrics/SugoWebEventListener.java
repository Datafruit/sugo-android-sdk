package io.sugo.android.mpmetrics;

import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fengxj on 11/7/16.
 */

public class SugoWebEventListener {
    private final SugoAPI sugoAPI;
    private static Map<String, JSONArray> eventBindingsMap = new HashMap<String, JSONArray>();
    public static String sCurrentWebUrl = "";

    SugoWebEventListener(SugoAPI sugoAPI) {
        this.sugoAPI = sugoAPI;
    }

    @JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void track(String eventId, String eventName, String props) {
        try {
            JSONObject jsonObject = new JSONObject(props);
            if (sCurrentWebUrl.contains("http")) {
                URL url = new URL(sCurrentWebUrl);
                String path = url.getPath();
                String pageName = SugoPageManager.getInstance().getCurrentPageName(path);
                jsonObject.put(SGConfig.FIELD_PAGE_NAME, pageName);
            }
            if (eventId == null || eventId.trim() == "")
                sugoAPI.track(eventName, jsonObject);
            else
                sugoAPI.track(eventId, eventName, jsonObject);
        } catch (JSONException e) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(SGConfig.FIELD_TEXT, e.toString());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            sugoAPI.track("Exception", jsonObject);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    @JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void timeEvent(String eventName) {
        sugoAPI.timeEvent(eventName);
    }

    public static void bindEvents(String token, JSONArray eventBindings) {
        eventBindingsMap.put(token, eventBindings);
    }

    public static JSONArray getBindEvents(String token) {
        return eventBindingsMap.get(token);
    }
}
