# Sugo Android SDK 使用文档

## 1. SDK 集成
> 集成 Sugo Android SDK 有三种方式，**选择其中一种即可**。

**1.1 Gradle 集成**
```Groovy
dependencies {
    compile 'io.sugo.android:sugo-android-sdk:1.0.3'
}
```

> 如果需要支持`XWalkView`，请使用

```Groovy
dependencies {
    compile 'io.sugo.android:sugo-android-sdk-xwalk:1.0.3'
}
```

**1.2 下载 SDK 并集成**
下载 SDK 压缩包，解压后将其中的 sugo-android-sdk.jar 导入你的项目 libs 目录中
> 如果需要支持`XWalkView`，请导入`sugo-android-sdk-xwalk.jar`

---

## 2. SDK 配置

> 请先登录您的【数果星盘】管理台，在数据管理-埋点项目-新建项目-新建应用中，创建您的应用，以获取对应的 Token 等。

![image](doc/img/create_app.png)

![image](doc/img/APP_INFO.png)



### 2.1 AndroidManifest.xml 基本配置
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    >

    <!-- 必要的权限 -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.BLUETOOTH"/>

  <application

    >
    ...
    <meta-data
            android:name="io.sugo.android.SGConfig.EnableDebugLogging"
            android:value="false"/>

    <meta-data
            android:name="io.sugo.android.SGConfig.token"
            android:value="{YOUR_TOKEN}" />

    <meta-data
            android:name="io.sugo.android.SGConfig.ProjectId"
            android:value="{YOUR_PROJECT}" />

  </application>
</manifest>
```

- SDK 配置说明

> 以下配置不包括`{}`，填入配置时，请删除大括号`{}`

#### 2.1.0 权限说明

#### 2.1.1 调试模式
> 开启调试模式，可以输出 SugoSDK 的日志

```xml
<meta-data
        android:name="io.sugo.android.SGConfig.EnableDebugLogging"
        android:value="true"/>
```

#### 2.1.2 Token 设置
> 必填

```xml
<meta-data
        android:name="io.sugo.android.SGConfig.token"
        android:value="{YOUR_TOKEN}" />
```

#### 2.1.3 Project Id 配置
> 必填

```xml
<meta-data
        android:name="io.sugo.android.SGConfig.ProjectId"
        android:value="{YOUR_PROJECT}" />
```
如果是私有云，请换成该配置
```xml
<meta-data
        android:name="io.sugo.android.SGConfig.EventsEndpoint"
        android:value="{EVENTS_ENDPOINT}?locate={YOUR_PROJECT}" />
```

#### 2.1.4 部署地址配置
> 仅私有云配置

```xml
<meta-data
        android:name="io.sugo.android.SGConfig.DecideEndpoint"
        android:value="{DECIDE_ENDPOINT}" />
```

#### 2.1.5 可视化埋点链接地址配置
> 仅私有云配置

```xml
<meta-data
        android:name="io.sugo.android.SGConfig.EditorUrl"
        android:value="{EDITOR_URL} />
```

#### 2.1... 代码混淆
> 如果您的应用使用了代码混淆， 请添加


```

#保护注解
-keepattributes *Annotation*
#不混淆资源类
-keepclassmembers class **.R$* {
    public static <fields>;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepattributes *JavascriptInterface*

-keep class io.sugo.android.mpmetrics.SugoAPI { *; }
-keep class io.sugo.android.mpmetrics.SugoWebEventListener { *; }
-keep class io.sugo.android.mpmetrics.SugoWebNodeReporter { *; }

```


---

## 3. SDK 使用

标准的使用实例，应该是在 APP 启动的第一个`Activity`中，添加以下代码
```Java
public class MainActivity extends Activity {

    SugoAPI mSugo;

    public void onCreate(Bundle saved) {
        // 获取 SugoAPI 实例，在第一次调用时，SDK 将会初始化
        mSugo = SugoAPI.getInstance(this);
        ...
    }

    // 使用 SugoAPI.track 方法来记录事件
    public void whenSomethingInterestingHappens(int flavor) {
        JSONObject properties = new JSONObject();
        properties.put("flavor", flavor);
        mSugo.track("Something Interesting Happened", properties);
        ...
    }

    public void onDestroy() {
        // 如果启动的 Activity 不是最后销毁的 Activity，
        // 这行代码可以移到对应的最后销毁的 Activity 的 onDestroy 方法里
        mSugo.flush();      // 非必须，仅推荐使用
        super.onDestroy();
    }
}
```

### 3.1 基本功能
#### 3.1.1 自定义事件
调用 `SugoAPI.track()`实现在代码中埋点来统计一次用户行为
**参数说明**
- eventId : 事件 id
- eventName : 事件名称
- properties : 事件的属性
```Java
SugoAPI sugoAPI = SugoAPI.getInstance(context);

JSONObject props = new JSONObject();
props.put("Gender", "Female");
props.put("Plan", "Premium");

sugoAPI.track("Plan Selected", props);
```

#### 3.1.2 时长事件
调用`SugoAPI.timeEvent()`来追踪一个事件发生的时长，例如统计一次图片上传所消耗的时间
```Java
SugoAPI sugoAPI = SugoAPI.getInstance(context);

// start the timer for the event "Image Upload"
sugoAPI.timeEvent("Image Upload");

// stop the timer if the imageUpload() method returns true
if(imageUpload()){
    sugoAPI.track("Image Upload");
}
```


#### 3.1.3 开启 H5 埋点
##### 3.1.3.1. WebView 支持
```Java
    webView.getSettings().setJavaScriptEnabled(true);
    SugoAPI instance = SugoAPI.getInstance(this);
    instance.addWebViewJavascriptInterface(webView);
    webView.setWebViewClient(new SugoWebViewClient() {
    });
    webView.loadUrl("http://sugo.io");
```

> 如果开发者不能实现或继承`SugoWebViewClient`，想要设置自己的`WebViewClient`，
> 则需要在自己的`WebViewClient`的`onPageFinished()`回调里，
> 调用`SugoWebViewClient.handlePageFinished(webView, url)`即可
```Java
webView.setWebViewClient(new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            SugoWebViewClient.handlePageFinished(webView, url);
        }
    });
```

##### 3.1.3.2 XWalkView 支持
> 如果您想要在项目中支持 XWalkView 的可视化埋点 ，可以使用以下代码
> (先要引用 sugo android sdk 的 xwalk 扩展库)

- 1   初始化 SugoAPI 是，使能 XWalkView
```Java
    // 首先，在第一次初始化 SugoAPI 的时候，使能 XWalkView 功能
    mSugoAPI = SugoAPI.getInstance(this);
    SugoXWalkViewSupport.enableXWalkView(mSugoAPI, true);
```
- 2   然后，在 XWalkView 使用的 Activity (继承了 XWalkActivity ) 中, 调用`SugoXWalkViewSupport.handleXWalkView`
- 3   接着，调用`XWalkView.setResourceClient` 重写 `onLoadFinished`方法，调用`SugoXWalkViewClient.handlePageFinished` 即可完成
```Java
    @Override
    protected void onXWalkReady() {
        mXWalkView = (XWalkView) findViewById(R.id.xwalkview);
        SugoXWalkViewSupport.handleXWalkView(SugoAPI.getInstance(this), mXWalkView);
        mXWalkView.setResourceClient(new XWalkResourceClient(mXWalkView) {
                    @Override
                    public void onLoadFinished(XWalkView view, String url) {
                        super.onLoadFinished(view, url);
                        SugoXWalkViewClient.handlePageFinished(view, url);
                    }
                });
        mXWalkView.load("http://sugo.io", null);
    }
```
- 4  最后，在`onCreate` 中调用`XWalkPreferences.setValue(XWalkPreferences.ANIMATABLE_XWALK_VIEW, true);`
才能使得 XWalkView 的网页内容被上传至可视化埋点


##### 3.1.3.3 H5 代码埋点

在进行了 `3.1.3.1` 或 `3.1.3.2` 的操作之后，SugoSDK 可支持 H5 的可视化埋点。
如果需要代码埋点，可在 js 中调用以下方法：

自定义事件，参数如下：
**参数说明**
- event_id : 事件 id
- event_name : 事件名称
- props : 事件的属性
```JavaScript
sugo.track(event_id, event_name, props);
```

时长事件（可参照 3.1.2）
```JavaScript
sugo.timeEvent(event_name);
```

#### 3.1.4 默认属性
有一种很常见的情况，你想让一些常见的属性在每一个事件中都添加上，因此，你可以将这些属性注册为`默认属性`。

`默认属性`是保存在设备上的，将会持续存在 APP 里。
SugoSDK 也设置了一些默认的`默认属性`，[点这里查看]()

想要设置`默认属性`，调用`SugoAPI.registerSuperProperties`
```Java
SugoAPI sugoAPI = SugoAPI.getInstance(context);

// Send a "User Type: Paid" property will be sent with all future track calls.
JSONObject props = new JSONObject();
props.put("User Type", "Paid");
sugoAPI.registerSuperProperties(props);
```
接下来，你记录的任何事件，这些`默认属性`都会被一起发送。
`默认属性`是存储在设备上的，退出 APP 后下次启动，该`默认属性`依旧存在。

**只设置一次`默认属性`**
通过`SugoAPI.registerSuperProperties`会强行覆盖已有的同名属性，如果有些属性你想只设置一次（例如第一次登录的时间），可以调用`SugoAPI.registerSuperPropertiesOnce`，这个方法只保存第一次设置时的值，后面对该属性的修改操作将无效。

**移除`默认属性`**
通过`SugoAPI.unregisterSuperProperty`可移除已设置的`默认属性`



---

## 4 Q&A
