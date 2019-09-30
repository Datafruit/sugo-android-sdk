package io.sugo.android.x5;

import android.app.Activity;
import android.util.JsonWriter;
import android.util.Log;
import android.view.View;

import com.tencent.smtt.sdk.WebView;

import org.json.JSONArray;

import java.io.IOException;

import io.sugo.android.metrics.SugoAPI;
import io.sugo.android.metrics.SugoWebNodeReporter;
import io.sugo.android.viewcrawler.X5Listener;

/**
 * create by chenyuyi on 2019/9/25
 */
public class SugoX5Support {
    private static boolean sIsSnapshotx5ViewEnable = false;

    public static void enablex5View(SugoAPI sugoAPI, boolean enable) {
        sIsSnapshotx5ViewEnable = enable;
        if (sIsSnapshotx5ViewEnable) {
            sugoAPI.setSnapshotViewListener(new X5Listener() {
                @Override
                public void snapshotSpecialView(JsonWriter j, View view) {
                    try {
                        if (checkx5View() && view instanceof WebView) {    // 检查是否有 x5View 这个类，否则接下来的调用会崩溃
                            SugoWebNodeReporter sugoWebNodeReporter = SugoAPI.getSugoWebNodeReporter(view);
                            if (sugoWebNodeReporter != null) {
                                final WebView x5View = (WebView) view;
                                int oldVersion = sugoWebNodeReporter.version;
                                x5View.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        x5View.loadUrl("javascript:if(typeof sugo === 'object' && typeof sugo.reportNodes === 'function'){sugo.reportNodes();}");
                                    }
                                });
                                int max_attempt = 40;
                                int count = 0;
                                while (oldVersion == sugoWebNodeReporter.version && count < max_attempt) {
                                    try {
                                        count++;
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        Log.e("snapshotSpecialView", e.getMessage());
                                    }
                                }
                                if (count < max_attempt) {

                                    j.name("htmlPage");

                                    j.beginObject();
                                    j.name("url").value(sugoWebNodeReporter.url);
                                    j.name("clientWidth").value(sugoWebNodeReporter.clientWidth);
                                    j.name("clientHeight").value(sugoWebNodeReporter.clientHeight);
                                    j.name("nodes").value(sugoWebNodeReporter.webNodeJson);
//                                    j.name("screenshot").value(bitmapToBase64(captureImage(x5View)));
                                    j.endObject();

                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void recyclerx5View(Activity activity) {
                    SugoX5EventListener.cleanUnusex5View(activity);
                }

                @Override
                public void bindEvents(String token, JSONArray eventBindings) {
                    SugoX5EventListener.bindEvents(token, eventBindings);
                }
            });
        } else {
            sugoAPI.setSnapshotViewListener((X5Listener) null);
        }
    }

    public static void handlex5View(SugoAPI sugoAPI, WebView x5View) {
        if (!isSnapshotx5ViewEnable()) {
            Log.e("Sugox5ViewSupport", "如果想要支持 x5View ，请调用 Sugox5ViewSupport.enablex5View");
            return;
        }
        x5View.addJavascriptInterface(new SugoX5EventListener(sugoAPI), "sugoEventListener");
        SugoX5ViewNodeReporter reporter = new SugoX5ViewNodeReporter();
        x5View.addJavascriptInterface(reporter, "sugoWebNodeReporter");
        SugoAPI.setSugoWebNodeReporter(x5View, reporter);
    }


    public static boolean isSnapshotx5ViewEnable() {
        return sIsSnapshotx5ViewEnable;
    }

    private static boolean checkx5View() {
        try {
            Class x5ViewClass = Class.forName("com.tencent.smtt.sdk.WebView");
            return true;
        } catch (ClassNotFoundException e) {
        }
        return false;
    }
}
