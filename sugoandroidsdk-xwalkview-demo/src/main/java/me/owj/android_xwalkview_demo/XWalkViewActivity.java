package me.owj.android_xwalkview_demo;

import android.os.Bundle;

import org.xwalk.core.XWalkActivity;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;

import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.android.xwalk.SugoXWalkViewClient;
import io.sugo.android.xwalk.SugoXWalkViewSupport;

public class XWalkViewActivity extends XWalkActivity {

    private XWalkView mXWalkView;

    @Override
    protected void onXWalkReady() {
        mXWalkView = (XWalkView) findViewById(R.id.xwalkview);
        mXWalkView.setResourceClient(new XWalkResourceClient(mXWalkView) {
            @Override
            public void onLoadFinished(XWalkView view, String url) {
                super.onLoadFinished(view, url);
                SugoXWalkViewClient.handlePageFinished(view, url);
            }
        });
        SugoXWalkViewSupport.handleXWalkView(SugoAPI.getInstance(this), mXWalkView);
        mXWalkView.load("https://m.jd.com", null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        XWalkPreferences.setValue(XWalkPreferences.ANIMATABLE_XWALK_VIEW, true);
        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xwalk_view);

    }

}
