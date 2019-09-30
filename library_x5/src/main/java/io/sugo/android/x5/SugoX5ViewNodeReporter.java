package io.sugo.android.x5;

import android.webkit.JavascriptInterface;

import io.sugo.android.metrics.SugoWebNodeReporter;

/**
 * create by chenyuyi on 2019/9/25
 */
public class SugoX5ViewNodeReporter extends SugoWebNodeReporter {

    SugoX5ViewNodeReporter() {
        super();
    }

    @Override
    @JavascriptInterface
    public void reportNodes(String url, String nodeJson, int clientWidth, int clientHeight, String title) {
        super.reportNodes(url, nodeJson, clientWidth, clientHeight, title);
    }
}
