package io.sugo.android.xwalk;

import android.webkit.JavascriptInterface;

import io.sugo.android.mpmetrics.SugoWebNodeReporter;

/**
 * Created by Administrator on 2017/2/8.
 */

public class SugoXWalkViewNodeReporter extends SugoWebNodeReporter {

    SugoXWalkViewNodeReporter() {
        super();
    }

    @Override
    @JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void reportNodes(String url, String nodeJson, int clientWidth, int clientHeight) {
        super.reportNodes(url,nodeJson,clientWidth,clientHeight);
    }
}
