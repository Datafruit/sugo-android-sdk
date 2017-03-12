package io.sugo.android.mpmetrics;

import android.app.Activity;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.core.XWalkView;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by fengxj on 11/7/16.
 */

public class SugoWebEventListener {
    private final SugoAPI sugoAPI;
    private static Map<String, JSONArray> eventBindingsMap = new HashMap<String, JSONArray>();
    protected static HashSet<WebView> sCurrentWebView = new HashSet<>();
    protected static HashSet<XWalkView> sCurrentXWalkView = new HashSet<>();
    public static Map<Object, SugoWebNodeReporter> sugoWNReporter = new HashMap<Object, SugoWebNodeReporter>();

    protected static final String sStayScript =
            "var duration = (new Date().getTime() - sugo.enter_time) / 1000;\n" +
                    "sugo.track('停留', {" + SGConfig.FIELD_DURATION + " : duration});\n";
    protected static long webEnterTime = 0;

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
        sCurrentWebView.add(currentWebView);
        if (SGConfig.DEBUG) {
            Log.d("SugoWebEventListener", "addCurrentWebView : " + currentWebView.toString());
        }

    }

    public static void addCurrentXWalkView(XWalkView currentXWalkView) {
        sCurrentXWalkView.add(currentXWalkView);
        webEnterTime = System.currentTimeMillis();
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
            if (activity == null || activity == deadActivity || activity.isFinishing() ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
                webViewIterator.remove();
                sugoWNReporter.remove(webView);
                if (SGConfig.DEBUG) {
                    Log.d("SugoWebEventListener", "removeWebViewReference : " + webView.toString());
                }
            }
        }

        Iterator<XWalkView> xWalkViewIterator = sCurrentXWalkView.iterator();
        while (xWalkViewIterator.hasNext()) {
            XWalkView xWalkView = xWalkViewIterator.next();
            Activity activity = (Activity) xWalkView.getContext();
            if (activity == null || activity == deadActivity || activity.isFinishing() ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
                xWalkViewIterator.remove();
                sugoWNReporter.remove(xWalkView);
                if (SGConfig.DEBUG) {
                    Log.d("SugoWebEventListener", "removeXWalkViewReference : " + xWalkView.toString());
                }
            }
        }

    }

    private static void injectStayEvent(XWalkView xWalkView) {
        Activity activity = (Activity) xWalkView.getContext();
        if (activity == null) {
            return;
        }
        final double timeSecondsDouble = (System.currentTimeMillis()) / 1000.0;
        final double eventBeginDouble = ((double) webEnterTime) / 1000.0;
        double duration = new BigDecimal(timeSecondsDouble - eventBeginDouble).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        String url = xWalkView.getUrl();


        String filePath = activity.getFilesDir().getPath(); // /data/user/0/io.sugo.xxx/files
        String dataPkgPath = filePath.substring(0, filePath.indexOf("/files"));      // /data/user/0/io.sugo.android
        String realPath = "";
        try {
            Pattern pattern = Pattern.compile("^[A-Za-z0-9]*://.*", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(url).matches()) {
                URL urlObj = new URL(url);
                realPath = urlObj.getPath();
                realPath = realPath.replaceFirst(dataPkgPath, "");
                String ref = urlObj.getRef();
                if (!TextUtils.isEmpty(ref)) {
                    if (ref.contains("?")) {
                        int qIndex = ref.indexOf("?");
                        ref = ref.substring(0, qIndex);
                    }
                    realPath = realPath + "#" + ref;
                }
            }
            realPath = realPath.replace(SGConfig.getInstance(activity.getApplicationContext()).getWebRoot(), "");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        JSONObject pageInfo = SugoPageManager.getInstance().getCurrentPageInfo(realPath);
        String pageName = "";
        String initCode = "";
        if (pageInfo != null) {
            pageName = pageInfo.optString("page_name", "");
            initCode = pageInfo.optString("code", "");
        }
        JSONObject props = new JSONObject();
        try {
            props.put(SGConfig.FIELD_DURATION, duration);
            props.put(SGConfig.FIELD_PAGE, realPath);
            props.put(SGConfig.FIELD_PAGE_NAME, pageName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SugoAPI.getInstance(activity.getApplicationContext()).track("停留", props);
    }

}
