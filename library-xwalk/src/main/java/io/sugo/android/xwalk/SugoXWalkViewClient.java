package io.sugo.android.xwalk;

import android.app.Activity;
import android.content.Context;

import org.xwalk.core.XWalkView;

import io.sugo.android.metrics.SugoWebViewClient;

/**
 * Created by Administrator on 2017/2/8.
 */

public class SugoXWalkViewClient extends SugoWebViewClient {

    public static void handlePageFinished(XWalkView xWalkView, String url) {
        Context context = xWalkView.getContext();
        Activity activity = (Activity) context;
        String script = getInjectScript(activity, url);
        xWalkView.load("javascript:" + script, "");

        SugoXWalkViewEventListener.addCurrentXWalkView(xWalkView);
    }

}
