package io.sugo.android.mpmetrics;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.core.XWalkView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by fengxj on 11/7/16.
 */

public class SugoWebEventListener {
    private final SugoAPI sugoAPI;
    private static Map<String, JSONArray> eventBindingsMap = new HashMap<String, JSONArray>();
    protected static HashSet<WebView> sCurrentWebView = new HashSet<>();
    protected static HashSet<XWalkView> sCurrentXWalkView = new HashSet<>();
    public static Map<Object, SugoWebNodeReporter> sugoWNReporter = new HashMap<Object, SugoWebNodeReporter>();

    SugoWebEventListener(SugoAPI sugoAPI) {
        this.sugoAPI = sugoAPI;
    }

    @JavascriptInterface
    @org.xwalk.core.JavascriptInterface
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
    @org.xwalk.core.JavascriptInterface
    public void timeEvent(String eventName) {
        sugoAPI.timeEvent(eventName);
    }

    public static void bindEvents(String token, JSONArray eventBindings) {
        eventBindingsMap.put(token, eventBindings);
        if (SugoAPI.developmentMode) {      // 只在连接编辑器模式下操作
            updateWebViewInject();
            updateXWalkViewInject();
        } else {
            sCurrentWebView.clear();
        }
    }

    public static JSONArray getBindEvents(String token) {
        return eventBindingsMap.get(token);
    }


    public static void addCurrentWebView(WebView currentWebView) {
        if (!SugoAPI.developmentMode) {
            return;
        }
        sCurrentWebView.add(currentWebView);
        if (SGConfig.DEBUG) {
            Log.d("SugoWebEventListener", "addCurrentWebView : " + currentWebView.toString());
        }

    }

    public static void addCurrentXWalkView(XWalkView currentXWalkView) {
        if (!SugoAPI.developmentMode) {
            return;
        }
        sCurrentXWalkView.add(currentXWalkView);
        if (SGConfig.DEBUG) {
            Log.d("SugoWebEventListener", "addCurrentXWalkView : " + currentXWalkView.toString());
        }

    }

    private static void updateWebViewInject() {
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

    private static void updateXWalkViewInject() {
        Iterator<XWalkView> viewIterator = sCurrentXWalkView.iterator();
        while (viewIterator.hasNext()) {
            final XWalkView xWalkView = viewIterator.next();
            if (xWalkView == null) {
                return;
            }
            ((Activity) xWalkView.getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    xWalkView.reload(XWalkView.RELOAD_NORMAL);
                    if (SGConfig.DEBUG) {
                        Log.d("SugoWebEventListener", "XWalkView reload : " + xWalkView.toString());
                    }
                }
            });

        }
    }

    /**
     * 移除 webview 引用，即移除对 Activity 的引用，避免内存泄漏
     */
    public static void cleanUnuseWebView(Activity deadActivity) {
        Iterator<WebView> webViewIterator = sCurrentWebView.iterator();
        WebView webView = null;
        while (webViewIterator.hasNext()) {
            webView = webViewIterator.next();
            Activity activity = (Activity) webView.getContext();
            if (activity == null || activity == deadActivity || activity.isFinishing()) {
                webViewIterator.remove();
                sugoWNReporter.remove(webView);
                if (SGConfig.DEBUG) {
                    Log.d("SugoWebEventListener", "removeWebViewReference : " + webView.toString());
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
                webViewIterator.remove();
                sugoWNReporter.remove(webView);
                if (SGConfig.DEBUG) {
                    Log.d("SugoWebEventListener", "removeWebViewReference : " + webView.toString());
                }
            }
        }

        Iterator<XWalkView> xWalkViewIterator = sCurrentXWalkView.iterator();
        XWalkView xWalkView = null;
        while (xWalkViewIterator.hasNext()) {
            xWalkView = xWalkViewIterator.next();
            Activity activity = (Activity) xWalkView.getContext();
            if (activity == null || activity == deadActivity || activity.isFinishing()) {
                xWalkViewIterator.remove();
                sugoWNReporter.remove(xWalkView);
                if (SGConfig.DEBUG) {
                    Log.d("SugoWebEventListener", "removeXWalkViewReference : " + xWalkView.toString());
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
                xWalkViewIterator.remove();
                sugoWNReporter.remove(xWalkView);
                if (SGConfig.DEBUG) {
                    Log.d("SugoWebEventListener", "removeXWalkViewReference : " + xWalkView.toString());
                }
            }
        }

    }

}
