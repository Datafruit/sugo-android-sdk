

##使用文档
`AndroidManifest.xml`文件的`application`节点下添加

```xml
<meta-data
            android:name="com.mixpanel.android.MPConfig.EditorUrl"
            android:value="ws://192.168.0.111:8887/connect/" />

<meta-data
            android:name="com.mixpanel.android.MPConfig.DecideEndpoint"
            android:value="http://192.168.0.111:8080/api/sdk/decide" />

<meta-data
            android:name="com.mixpanel.android.MPConfig.token"
            android:value="{YOUR_TOKEN}" />

<meta-data
            android:name="com.mixpanel.android.MPConfig.EventsEndpoint"
            android:value="http://collect.sugo.net/post?locate={YOUR_PROJECT}" />
```

`AndroidManifest.xml`文件的 `MAIN activity` 下添加
```xml

 <intent-filter>
                <data android:scheme="sugo.{YOUR_TOKEN}"/>
                <action android:name="android.intent.action.VIEW"/>

                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.BROWSABLE"/>
            </intent-filter>
```
`MAIN activity` 的 `onCreate` 时间如下 添加
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MixpanelAPI mixpanel = MixpanelAPI.getInstance(this);
}
```

H5支持
```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_x5_web_view);
        initWebView();
    }

    void initWebView() {
        final WebView webView = (WebView) findViewById(R.id.sys_webview);
        MixpanelAPI.getInstance(this).handleWebView(webView);
        webView.loadUrl("http://dev.ufile.ucloud.cn/test.html");

    }
```
