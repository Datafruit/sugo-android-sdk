package io.sugo.sdkdemo.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.android.mpmetrics.SugoWebViewClient;
import io.sugo.sdkdemo.R;

/**
 * create by chenyuyi on 2019/6/25
 */
public class WebViewMoreActivity extends AppCompatActivity {

    @BindView(R.id.back_img)
    ImageView mBackImg;
    @BindView(R.id.web_view)
    WebView mWebView;
    @BindView(R.id.web_view2)
    WebView mWebView2;

    private String mUrl1 = null;
    private String mUrl2 = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.more_webview);
        ButterKnife.bind(this);
        mUrl1 = getIntent().getStringExtra("url");
        mUrl2 = getIntent().getStringExtra("url2");
        Button  btn1 = (Button) findViewById(R.id.btn);
        btn1.setOnClickListener(new  View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mWebView2.getVisibility() == View.GONE){
                    mWebView.setVisibility(View.GONE);
                    mWebView2.setVisibility(View.VISIBLE);
                }else{
                    mWebView.setVisibility(View.VISIBLE);
                    mWebView2.setVisibility(View.GONE);
                }

            }
        });
        initWebView();
    }

    private void initWebView() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        SugoAPI.getInstance(this).addWebViewJavascriptInterface(mWebView);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                SugoWebViewClient.handlePageFinished(mWebView, url);
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }
        });
        mWebView.loadUrl(mUrl1 == null ? "https://m.jd.com/" : mUrl1);

        mWebView2.getSettings().setJavaScriptEnabled(true);
        SugoAPI.getInstance(this).addWebViewJavascriptInterface(mWebView2);
        mWebView2.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                SugoWebViewClient.handlePageFinished(mWebView2, url);
            }
        });
        mWebView2.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }
        });
        mWebView2.loadUrl(mUrl2 == null ? "http://search.m.dangdang.com/ddcategory.php" : mUrl2);
    }
}
