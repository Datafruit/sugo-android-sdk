package io.sugo.android.x5;

import android.app.Activity;
import android.content.Context;

import com.tencent.smtt.sdk.WebView;

import io.sugo.android.metrics.SugoWebViewClient;

import static io.sugo.android.metrics.SugoWebViewClient.getInjectScript;

/**
 * create by chenyuyi on 2019/9/25
 */
public class SugoX5ViewClient extends SugoWebViewClient {

    public static void handlePageFinished(WebView webView, String url) {
        Context context = webView.getContext();
        Activity activity = (Activity) context;
        String script = getInjectScript(activity, url);
        webView.loadUrl("javascript:" + script);

        SugoX5EventListener.addCurrentXWalkView(webView);
    }

}
