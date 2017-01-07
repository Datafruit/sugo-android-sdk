package io.sugo.android.mpmetrics;

import android.webkit.JavascriptInterface;

/**
 * Created by fengxj on 11/7/16.
 */

public class SugoWebNodeReporter {
    public  int version = 0;
    public  String webNodeJson = "";
    public  String url = null;
    public  int clientWidth = 0;
    public  int clientHeight = 0;
    SugoWebNodeReporter() {
    }

    @JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void reportNodes(String url,String nodeJson,int clientWidth,int clientHeight) {
        this.webNodeJson = nodeJson;
        this.url = url;
        this.clientWidth = clientWidth;
        this.clientHeight = clientHeight;
        this.version ++;
    }
}
