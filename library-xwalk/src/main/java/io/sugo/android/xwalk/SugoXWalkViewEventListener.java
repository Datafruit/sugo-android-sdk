package io.sugo.android.xwalk;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.xwalk.core.XWalkView;

import java.util.HashSet;
import java.util.Iterator;

import io.sugo.android.mpmetrics.SGConfig;
import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.android.mpmetrics.SugoWebEventListener;

/**
 * Created by Administrator on 2017/2/8.
 */

public class SugoXWalkViewEventListener extends SugoWebEventListener {

    protected static HashSet<XWalkView> sCurrentXWalkView = new HashSet<>();

    SugoXWalkViewEventListener(SugoAPI sugoAPI) {
        super(sugoAPI);
    }

    @Override
    @JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void track(String eventId, String eventName, String props) {
        super.track(eventId, eventName, props);
    }

    @Override
    @JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void timeEvent(String eventName) {
        super.timeEvent(eventName);
    }

    public static void bindEvents(String token, JSONArray eventBindings) {
//        eventBindingsMap.put(token, eventBindings);
        if (SugoAPI.developmentMode) {      // 只在连接编辑器模式下操作
            updateXWalkViewInject();
        } else {
            sCurrentXWalkView.clear();
        }
    }

    public static void addCurrentXWalkView(XWalkView currentXWalkView) {
        sCurrentXWalkView.add(currentXWalkView);
        if (SGConfig.DEBUG) {
            Log.d("SugoWebEventListener", "addCurrentXWalkView : " + currentXWalkView.toString());
        }

    }

    public static void updateXWalkViewInject() {
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
    public static void cleanUnuseXwalkView(Activity deadActivity) {
        Iterator<XWalkView> xWalkViewIterator = sCurrentXWalkView.iterator();
        XWalkView xWalkView = null;
        while (xWalkViewIterator.hasNext()) {
            xWalkView = xWalkViewIterator.next();
            Activity activity = (Activity) xWalkView.getContext();
            if (activity == null || activity == deadActivity || activity.isFinishing() ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
                xWalkViewIterator.remove();
                sugoWNReporter.remove(xWalkView);
                xWalkView.load("javascript:" + sStayScript, null);
                if (SGConfig.DEBUG) {
                    Log.d("SugoWebEventListener", "removeXWalkViewReference : " + xWalkView.toString());
                }
            }
        }
    }

}
