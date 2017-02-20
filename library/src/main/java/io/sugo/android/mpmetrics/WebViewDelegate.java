package io.sugo.android.mpmetrics;

/**
 * Created by fengxj on 1/4/17.
 */

public interface WebViewDelegate {
    void addJavascriptInterface(Object object, String name);
    void loadUrl (String url);
}
