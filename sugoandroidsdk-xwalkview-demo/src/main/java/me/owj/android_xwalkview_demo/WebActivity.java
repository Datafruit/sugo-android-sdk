package me.owj.android_xwalkview_demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.android.mpmetrics.SugoWebViewClient;

public class WebActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        final WebView webView = (WebView) findViewById(R.id.web_view);
        SugoAPI.getInstance(this).addWebViewJavascriptInterface(webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                SugoWebViewClient.handlePageFinished(webView, url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://m.jd.com/?a=b#123#234");

    }

}
