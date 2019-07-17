package io.sugo.android.metrics;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.sugo.android.util.StringUtils;
import io.sugo.android.viewcrawler.SugoHeatMap;

/**
 *
 * @author fengxj
 * @date 11/7/16
 */

public class SugoWebEventListener {
    protected final SugoAPI sugoAPI;
    protected WebView webView;
    protected static Map<String, JSONArray> eventBindingsMap = new HashMap<String, JSONArray>();
    protected static HashSet<WebView> sCurrentWebView = new HashSet<>();

    public static Map<Object, SugoWebNodeReporter> sugoWNReporter = new HashMap<Object, SugoWebNodeReporter>();

    protected static final String sStayScript =
            "var duration = (new Date().getTime() - sugo.enter_time)/1000;\n" +
                    "sugo.track('停留', {" + SGConfig.FIELD_DURATION + ": duration});\n";

    public SugoWebEventListener(SugoAPI sugoAPI) {
        this.sugoAPI = sugoAPI;
    }


    public SugoWebEventListener(SugoAPI sugoAPI, WebView webView) {
        this.sugoAPI = sugoAPI;
        this.webView = webView;
    }

    @JavascriptInterface
    public void track(String eventId, String eventName, String props) {
        try {
            JSONObject jsonObject = new JSONObject(props);
            if (!jsonObject.has(SGConfig.FIELD_PAGE_NAME)) {
                jsonObject.put(SGConfig.FIELD_PAGE_NAME, "");
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
        }

    }

    @JavascriptInterface
    public void registerSuperProperties(String props) {
        try {
            JSONObject jsonObject = new JSONObject(props);
            sugoAPI.registerSuperProperties(jsonObject);
        } catch (JSONException e) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(SGConfig.FIELD_TEXT, e.toString());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            sugoAPI.track("Exception", jsonObject);
        }

    }
    @JavascriptInterface
    public void login(String key, String value) {
        sugoAPI.login(key ,value);
    }
    @JavascriptInterface
    public void loginout() {
        sugoAPI.logout();
    }

    @JavascriptInterface
    public void pageFinish(String url, String js) {
        if(webView == null){
            return ;
        }
        final String finalUrl = url;
        final String finalJs = js;
        Activity activity = (Activity) webView.getContext();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(finalJs == null || finalJs.trim().equals("") ){
                    SugoWebViewClient.handlePageFinished(webView, finalUrl);
                } else {
                    SugoWebViewClient.handlePageFinished(webView, finalUrl, finalJs);
                }

            }
        });
    }
    @JavascriptInterface
    public void timeEvent(String eventName) {
        sugoAPI.timeEvent(eventName);
    }

    @JavascriptInterface
    public boolean isShowHeatMap() {
        return SugoHeatMap.isShowHeatMap();
    }

    @JavascriptInterface
    public int getEventHeatColor(String eventId) {
        return SugoHeatMap.getEventHeat(eventId);
    }


    public static void bindEvents(String token, JSONArray eventBindings) {
        eventBindingsMap.put(token, eventBindings);
        // 只在连接编辑器模式下操作
        if (SugoAPI.editorConnected) {
            updateWebViewInject();
        } else {
            sCurrentWebView.clear();
        }
    }

    public static JSONArray getBindEvents(String token) {
        return eventBindingsMap.get(token);
    }

    public static void addCurrentWebView(WebView currentWebView) {
        sCurrentWebView.add(currentWebView);
        if (SGConfig.DEBUG) {
            Log.d("SugoWebEventListener", "addCurrentWebView : " + currentWebView.toString());
        }

    }

    public static void updateWebViewInject() {
        Iterator<WebView> webViewIterator = sCurrentWebView.iterator();
        while (webViewIterator.hasNext()) {
            final WebView webView = webViewIterator.next();
            if (webView == null) {
                return;
            }
            ((Activity) webView.getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webView.reload();
                    if (SGConfig.DEBUG) {
                        Log.d("SugoWebEventListener", "webview reload : " + webView.toString());
                    }
                }
            });

        }
    }

    /**
     * 移除 webview 引用，即移除对 Activity 的引用，避免内存泄漏
     */
    public static void cleanUnuseWebView(Activity deadActivity,boolean isRemoveWV) {
        Iterator<WebView> webViewIterator = sCurrentWebView.iterator();
        WebView webView = null;
        List<WebView> removeWebViews = new ArrayList<>();
        while (webViewIterator.hasNext()) {
            webView = webViewIterator.next();
            Activity activity = (Activity) webView.getContext();
            if (activity == null || activity == deadActivity || activity.isFinishing() ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
                removeWebViews.add(webView);
            }
        }
        for (WebView removeWV : removeWebViews) {
            if (isRemoveWV){
                sCurrentWebView.remove(removeWV);
                sugoWNReporter.remove(removeWV);
            }
            if (SGConfig.getInstance(removeWV.getContext()).isEnablePageEvent()) {
                removeWV.loadUrl("javascript:" + sStayScript);
            }
            if (SGConfig.DEBUG) {
                Log.d("SugoWebEventListener", "removeWebViewReference : " + removeWV.toString());
            }
        }
    }

}
