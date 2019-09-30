package io.sugo.android.x5;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.tencent.smtt.sdk.WebView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import io.sugo.android.metrics.SGConfig;
import io.sugo.android.metrics.SugoAPI;
import io.sugo.android.metrics.SugoWebEventListener;

/**
 * create by chenyuyi on 2019/9/25
 */


public class SugoX5EventListener extends SugoWebEventListener {

    protected static HashSet<WebView> sCurrentX5View = new HashSet<>();

    SugoX5EventListener(SugoAPI sugoAPI) {
        super(sugoAPI);
    }

    @Override
    @JavascriptInterface
    public void track(String eventId, String eventName, String props) {
        super.track(eventId, eventName, props);
    }

    @Override
    @JavascriptInterface
    public void timeEvent(String eventName) {
        super.timeEvent(eventName);
    }

    @Override
    @JavascriptInterface
    public boolean isShowHeatMap() {
        return super.isShowHeatMap();
    }

    @Override
    @JavascriptInterface
    public int getEventHeatColor(String eventId) {
        return super.getEventHeatColor(eventId);
    }

    public static void bindEvents(String token, JSONArray eventBindings) {
//        eventBindingsMap.put(token, eventBindings);
        if (SugoAPI.editorConnected) {      // 只在连接编辑器模式下操作
            updateXWalkViewInject();
        } else {
            sCurrentX5View.clear();
        }
    }

    public static void addCurrentXWalkView(WebView currentXWalkView) {
        sCurrentX5View.add(currentXWalkView);
        if (SGConfig.DEBUG) {
            Log.d("SugoWebEventListener", "addCurrentXWalkView : " + currentXWalkView.toString());
        }

    }

    public static void updateXWalkViewInject() {
        Iterator<WebView> viewIterator = sCurrentX5View.iterator();
        while (viewIterator.hasNext()) {
            final WebView x5View = viewIterator.next();
            if (x5View == null) {
                return;
            }
            ((Activity) x5View.getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    x5View.reload();
                    if (SGConfig.DEBUG) {
                        Log.d("SugoWebEventListener", "x5View reload : " + x5View.toString());
                    }
                }
            });
        }
    }

    /**
     * 移除 webview 引用，即移除对 Activity 的引用，避免内存泄漏
     */
    public static void cleanUnusex5View(Activity deadActivity) {
        Iterator<WebView> x5ViewIterator = sCurrentX5View.iterator();
        WebView x5View = null;
        List<WebView> removeXWVS = new ArrayList<>();
        while (x5ViewIterator.hasNext()) {
            x5View = x5ViewIterator.next();
            Activity activity = (Activity) x5View.getContext();
            if (activity == null || activity == deadActivity || activity.isFinishing() ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
                removeXWVS.add(x5View);
            }
        }
        for (WebView removeXWV : removeXWVS) {
            sCurrentX5View.remove(removeXWV);
            sugoWNReporter.remove(removeXWV);
            if(SGConfig.getInstance(removeXWV.getContext()).isEnablePageEvent()) {
                removeXWV.loadUrl("javascript:" + sStayScript);
            }
            if (SGConfig.DEBUG) {
                Log.d("SugoWebEventListener", "removex5ViewReference : " + removeXWV.toString());
            }
        }
    }

}

