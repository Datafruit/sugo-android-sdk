package io.sugo.android.mpmetrics;

import android.webkit.JavascriptInterface;

/**
 * Created by fengxj on 11/7/16.
 */

public class SugoWebNodeReporter {
    public static int version = 0;
    public static String webNodeJson = "";
    public static String url = null;
    public static int clientWidth = 0;
    public static int clientHeight = 0;
    SugoWebNodeReporter() {
    }

    @JavascriptInterface
    public void reportNodes(String url,String nodeJson,int clientWidth,int clientHeight) {
        SugoWebNodeReporter.webNodeJson = nodeJson;
        SugoWebNodeReporter.url = url;
        SugoWebNodeReporter.clientWidth = clientWidth;
        SugoWebNodeReporter.clientHeight = clientHeight;
        SugoWebNodeReporter.version ++;
    }
}
