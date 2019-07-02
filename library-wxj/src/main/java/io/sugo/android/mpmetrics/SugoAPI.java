package io.sugo.android.mpmetrics;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.core.XWalkView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.sugo.android.util.ExceptionInfoUtils;
import io.sugo.android.util.HttpService;
import io.sugo.android.util.RemoteService;
import io.sugo.android.viewcrawler.TrackingDebug;
import io.sugo.android.viewcrawler.UpdatesFromMixpanel;
import io.sugo.android.viewcrawler.ViewCrawler;

/**
 * Core class for interacting with Mixpanel Analytics.
 * <p>
 * <p>Call {@link #getInstance(Context)} with
 * your main application activity and your Mixpanel API token as arguments
 * an to get an instance you can use to report how users are using your
 * application.
 * <p>
 * <p>Once you have an instance, you can send events to Mixpanel
 * using {@link #track(String, JSONObject)}
 * <p>
 * <p>The Mixpanel library will periodically send information to
 * Mixpanel servers, so your application will need to have
 * <tt>android.permission.INTERNET</tt>. In addition, to preserve
 * battery life, messages to Mixpanel servers may not be sent immediately
 * when you call <tt>track</tt>.
 * The library will send messages periodically throughout the lifetime
 * of your application, but you will need to call {@link #flush()}
 * before your application is completely shutdown to ensure all of your
 * events are sent.
 * <p>
 * <p>A typical use-case for the library might look like this:
 * <p>
 * <pre>
 * {@code
 * public class MainActivity extends Activity {
 *      SugoAPI mMixpanel;
 *
 *      public void onCreate(Bundle saved) {
 *          mMixpanel = SugoAPI.getInstance(this, "YOUR MIXPANEL API TOKEN");
 *          ...
 *      }
 *
 *      public void whenSomethingInterestingHappens(int flavor) {
 *          JSONObject properties = new JSONObject();
 *          properties.put("flavor", flavor);
 *          mMixpanel.track("Something Interesting Happened", properties);
 *          ...
 *      }
 *
 *      public void onDestroy() {
 *          mMixpanel.flush();
 *          super.onDestroy();
 *      }
 * }
 * }
 * </pre>
 * <p>
 * <p>In addition to this documentation, you may wish to take a look at
 * <a href="https://github.com/mixpanel/sample-android-mixpanel-integration">the Mixpanel sample Android application</a>.
 * <p>
 * <p>There are also <a href="https://mixpanel.com/docs/">step-by-step getting started documents</a>
 * available at mixpanel.com
 *
 * @see <a href="https://mixpanel.com/docs/integration-libraries/android">getting started documentation for tracking events</a>
 * @see <a href="https://mixpanel.com/docs/people-analytics/android">getting started documentation for People Analytics</a>
 * @see <a href="https://mixpanel.com/docs/people-analytics/android-push">getting started with push notifications for Android</a>
 * @see <a href="https://github.com/mixpanel/sample-android-mixpanel-integration">The Mixpanel Android sample application</a>
 */
public class SugoAPI {
    /**
     * String version of the library.
     */
    public static final String VERSION = SGConfig.VERSION;

    public static boolean developmentMode = false;

    private static boolean SUGO_ENABLE = true;

    public static final int SUGO_EXTRA_TAG = 91109102;

    public static final String SUGO_TAG = "SUGO";

    /**
     * Declare a string-valued tweak, and return a reference you can use to read the value of the tweak.
     * Tweaks can be changed in Mixpanel A/B tests, and can allow you to alter your customers' experience
     * in your app without re-deploying your application through the app store.
     */
    public static Tweak<String> stringTweak(String tweakName, String defaultValue) {
        return sSharedTweaks.stringTweak(tweakName, defaultValue);
    }

    /**
     * Declare a boolean-valued tweak, and return a reference you can use to read the value of the tweak.
     * Tweaks can be changed in Mixpanel A/B tests, and can allow you to alter your customers' experience
     * in your app without re-deploying your application through the app store.
     */
    public static Tweak<Boolean> booleanTweak(String tweakName, boolean defaultValue) {
        return sSharedTweaks.booleanTweak(tweakName, defaultValue);
    }

    /**
     * Declare a double-valued tweak, and return a reference you can use to read the value of the tweak.
     * Tweaks can be changed in Mixpanel A/B tests, and can allow you to alter your customers' experience
     * in your app without re-deploying your application through the app store.
     */
    public static Tweak<Double> doubleTweak(String tweakName, double defaultValue) {
        return sSharedTweaks.doubleTweak(tweakName, defaultValue);
    }

    /**
     * Declare a float-valued tweak, and return a reference you can use to read the value of the tweak.
     * Tweaks can be changed in Mixpanel A/B tests, and can allow you to alter your customers' experience
     * in your app without re-deploying your application through the app store.
     */
    public static Tweak<Float> floatTweak(String tweakName, float defaultValue) {
        return sSharedTweaks.floatTweak(tweakName, defaultValue);
    }

    /**
     * Declare a long-valued tweak, and return a reference you can use to read the value of the tweak.
     * Tweaks can be changed in Mixpanel A/B tests, and can allow you to alter your customers' experience
     * in your app without re-deploying your application through the app store.
     */
    public static Tweak<Long> longTweak(String tweakName, long defaultValue) {
        return sSharedTweaks.longTweak(tweakName, defaultValue);
    }

    /**
     * Declare an int-valued tweak, and return a reference you can use to read the value of the tweak.
     * Tweaks can be changed in Mixpanel A/B tests, and can allow you to alter your customers' experience
     * in your app without re-deploying your application through the app store.
     */
    public static Tweak<Integer> intTweak(String tweakName, int defaultValue) {
        return sSharedTweaks.intTweak(tweakName, defaultValue);
    }

    /**
     * Declare short-valued tweak, and return a reference you can use to read the value of the tweak.
     * Tweaks can be changed in Mixpanel A/B tests, and can allow you to alter your customers' experience
     * in your app without re-deploying your application through the app store.
     */
    public static Tweak<Short> shortTweak(String tweakName, short defaultValue) {
        return sSharedTweaks.shortTweak(tweakName, defaultValue);
    }

    /**
     * Declare byte-valued tweak, and return a reference you can use to read the value of the tweak.
     * Tweaks can be changed in Mixpanel A/B tests, and can allow you to alter your customers' experience
     * in your app without re-deploying your application through the app store.
     */
    public static Tweak<Byte> byteTweak(String tweakName, byte defaultValue) {
        return sSharedTweaks.byteTweak(tweakName, defaultValue);
    }

    /**
     * You shouldn't instantiate SugoAPI objects directly.
     * Use SugoAPI.getInstance to get an instance.
     */
    SugoAPI(Context context, Future<SharedPreferences> referrerPreferences) {
        this(context, referrerPreferences, SGConfig.getInstance(context));
    }

    /**
     * You shouldn't instantiate SugoAPI objects directly.
     * Use SugoAPI.getInstance to get an instance.
     */
    SugoAPI(Context context, Future<SharedPreferences> referrerPreferences, SGConfig config) {
        Context appContext = context.getApplicationContext();
        mContext = appContext;
        mConfig = config;
        mToken = config.getToken();
        mSessionId = generateSessionId();
        SUGO_ENABLE = mConfig.isSugoEnable();

        restorePositionConfig();
        restorePageInfo();
        restoreDimensions();

        final Map<String, String> deviceInfo = new HashMap<String, String>();
        deviceInfo.put("$android_lib_version", SGConfig.VERSION);
        deviceInfo.put("$android_os", "Android");
        deviceInfo.put("$android_os_version", Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
        deviceInfo.put("$android_manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
        deviceInfo.put("$android_brand", Build.BRAND == null ? "UNKNOWN" : Build.BRAND);
        deviceInfo.put("$android_model", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);
        try {

            final PackageManager manager = mContext.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            deviceInfo.put("$android_app_version", info.versionName);
            deviceInfo.put("$android_app_version_code", Integer.toString(info.versionCode));
        } catch (final Exception e) {
            Log.e(LOGTAG, "Exception getting app version name", e);
        }
        mDeviceInfo = Collections.unmodifiableMap(deviceInfo);

        mUpdatesFromMixpanel = constructUpdatesFromMixpanel(context, mToken);
        mTrackingDebug = constructTrackingDebug();
        mPersistentIdentity = getPersistentIdentity(appContext, referrerPreferences, mToken);
        registerSuperProperties(sPreSuperProps);
        registerSuperPropertiesOnce(sPreSuperPropsOnce);
        mEventTimings = mPersistentIdentity.getTimeEvents();
        classAttributeDict = new HashMap<>();
        mUpdatesListener = constructUpdatesListener();
        mDecideMessages = constructDecideUpdates(mToken, mUpdatesListener, mUpdatesFromMixpanel);


        // TODO reading persistent identify immediately forces the lazy load of the preferences, and defeats the
        // purpose of PersistentIdentity's laziness.
        String decideId = mPersistentIdentity.getPeopleDistinctId();
        if (null == decideId) {
            decideId = mPersistentIdentity.getEventsDistinctId();
        }
        mDecideMessages.setDistinctId(decideId);
        mMessages = getAnalyticsMessages();

        if (!SUGO_ENABLE) {
            // 禁用 Sugo 的功能，初始化到此结束
            mMessages.hardKill();
            return;
        }


        if (!mConfig.getDisableDecideChecker()) {
            mMessages.installDecideCheck(mDecideMessages);
        }

        registerMixpanelActivityLifecycleCallbacks();

        if (sendAppOpen()) {
            track(null, "$app_open", null);
        }

        if (!mPersistentIdentity.hasTrackedIntegration()) {
            JSONObject props = new JSONObject();
            try {
                props.put("app_name", "无限极中国APP");
                props.put(SGConfig.FIELD_PAGE_NAME, "APP安装");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            track("APP安装", props);
            flush();
            mPersistentIdentity.setTrackedIntegration(true);
        }

        mUpdatesFromMixpanel.startUpdates();
    }

    /**
     * Get the instance of SugoAPI associated with your Mixpanel project token.
     * <p>
     * <p>Use getInstance to get a reference to a shared
     * instance of SugoAPI you can use to send events
     * and People Analytics updates to Mixpanel.</p>
     * <p>getInstance is thread safe, but the returned instance is not,
     * and may be shared with other callers of getInstance.
     * The best practice is to call getInstance, and use the returned SugoAPI,
     * object from a single thread (probably the main UI thread of your application).</p>
     * <p>If you do choose to track events from multiple threads in your application,
     * you should synchronize your calls on the instance itself, like so:</p>
     * <pre>
     * {@code
     * SugoAPI instance = SugoAPI.getInstance(context, token);
     * synchronized(instance) { // Only necessary if the instance will be used in multiple threads.
     *     instance.track(...)
     * }
     * }
     * </pre>
     *
     * @param context The application context you are tracking
     * @return an instance of SugoAPI associated with your project
     */
    public static SugoAPI getInstance(Context context) {
        if (null == context) {
            return null;
        }
        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();

            if (null == sReferrerPrefs) {
                sReferrerPrefs = sPrefsLoader.loadPreferences(context, SGConfig.REFERRER_PREFS_NAME, null);
            }

            SugoAPI instance = sInstanceMap.get(appContext);
            if (null == instance && ConfigurationChecker.checkBasicConfiguration(appContext)) {
                instance = new SugoAPI(context, sReferrerPrefs);
//                registerAppLinksListeners(context, instance);
                sInstanceMap.put(appContext, instance);
            }
            return instance;
        }
    }

    public static void startSugo(Context context, SGConfig sgConfig, final InitSugoCallback callback) {
        try {
            if (null == context) {
                Log.e(LOGTAG, "startSugo 失败，context 为空");
                return;
            }
            synchronized (sInstanceMap) {
                if (sInstanceMap.get(context.getApplicationContext()) != null) {
                    Log.e(LOGTAG, "Sugo SDK 已经初始化，不能再次初始化");
                    return;
                }
            }
            if (TextUtils.isEmpty(sgConfig.getToken())) {
                Log.e(LOGTAG, "未检测到 SugoSDK 的 Token，请正确设置 SGConfig.setToken");
                return;
            }
            if (TextUtils.isEmpty(sgConfig.getProjectId())) {
                Log.e(LOGTAG, "未检测到 SugoSDK 的 ProjectId，请正确设置 SGConfig.setProjectId");
                return;
            }
            new SugoInitThread(context, callback).start();
        } catch (Exception e) {
            try {
                SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.ISSUGOINITIALIZE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(ViewCrawler.ISSUGOINITIALIZE, false);
                editor.commit();
            } catch (Exception exception) {
                Log.e(SUGO_TAG, "startSugo: ", e);
            }

            return;
        }


    }


    public static class SugoInitThread extends Thread {

        private Context context;
        private final InitSugoCallback callback;

        public void runSdkInitializeRequest() throws RemoteService.ServiceUnavailableException {
            String responseString = null;
            try {
                SystemInformation mSystemInformation = new SystemInformation(context);
                SGConfig config = SGConfig.getInstance(context);
                final String token = config.getToken();
                final String projectId = config.getProjectId();
                final String appVersion = mSystemInformation.getAppVersionName();
                DecideChecker decideChecker = new DecideChecker(context, config, mSystemInformation);
                responseString = decideChecker.getSugoInitializeEndpointFromServer(token, projectId, appVersion, new HttpService());
                if (SGConfig.DEBUG) {
                    Log.v(LOGTAG, "Sugo decide server response was:\n" + responseString);
                }

                if (TextUtils.isEmpty(responseString)) {
                    //TODO 添加没有数据返回处理，把所有字段设置为false
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.ISSUGOINITIALIZE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(ViewCrawler.ISSUGOINITIALIZE, false);
                    editor.commit();
                    return;
                }
            } catch (Exception e) {
                try {
                    //TODO 添加没有数据返回处理，把所有字段设置为false
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.ISSUGOINITIALIZE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(ViewCrawler.ISSUGOINITIALIZE, false);
                    editor.commit();
                    try {
                        AnalyticsMessages.sendDataForInitSugo(context, e);
                        return;
                    }catch (Exception e1){
                        return;
                    }
                }catch (Exception excepiton){
                    return;
                }

            }
            try {
                JSONObject response = new JSONObject(responseString);
                if (response.has("isSugoInitialize")) {
                    boolean isSugoInitialize = response.optBoolean("isSugoInitialize", false);
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.ISSUGOINITIALIZE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(ViewCrawler.ISSUGOINITIALIZE, isSugoInitialize);
                    editor.commit();
                }
                if (response.has("isHeatMapFunc")) {
                    boolean isHeatMapFunc = response.optBoolean("isHeatMapFunc", false);
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.ISHEATMAPFUNC, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(ViewCrawler.ISHEATMAPFUNC, isHeatMapFunc);
                    editor.commit();
                }
                if (response.has("uploadLocation")) {
                    int uploadLocation = response.optInt("uploadLocation", 0);
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.UPLOADLOCATION, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(ViewCrawler.UPLOADLOCATION, uploadLocation);
                    editor.commit();
                }
                if (response.has("isUpdateConfig")) {
                    boolean isUpdateConfig = response.optBoolean("isUpdateConfig", false);
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.ISUPDATACONFIG, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(ViewCrawler.ISUPDATACONFIG, isUpdateConfig);
                    editor.commit();

                }
                if (response.has("latestEventBindingVersion")) {
                    Long laestEventBindingVersion = response.optLong("latestEventBindingVersion", -1);
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.LAESTEVENTBINDINGVERSION, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putLong(ViewCrawler.LAESTEVENTBINDINGVERSION, laestEventBindingVersion);
                    editor.commit();
                }
                if (response.has("latestDimensionVersion")) {
                    Long laestDimensionVersion = response.optLong("latestDimensionVersion", -1);
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.LAESTDIMENSIONVERSION, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putLong(ViewCrawler.LAESTDIMENSIONVERSION, laestDimensionVersion);
                    editor.commit();
                }
            } catch (JSONException e) {
                try {
                    AnalyticsMessages.sendDataForInitSugo(context, e);
                }catch (Exception exception){
                    return;
                }
                return;
            }
        }

        public SugoInitThread(Context context, final InitSugoCallback callback) {
            this.context = context;
            this.callback = callback;
        }

        @Override
        public void run() {
            super.run();
            try {
                runSdkInitializeRequest();
            } catch (RemoteService.ServiceUnavailableException e) {
                try {
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.ISSUGOINITIALIZE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(ViewCrawler.ISSUGOINITIALIZE, false);
                    editor.commit();
                    Log.e(SUGO_TAG, "sugo init api: " + e.toString());
                    AnalyticsMessages.sendDataForInitSugo(context, e);
                } catch (Exception exception) {
                    Log.e(SUGO_TAG, "run: ", exception);
                    return;
                }

            } catch (Exception e) {
                try {
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.ISSUGOINITIALIZE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(ViewCrawler.ISSUGOINITIALIZE, false);
                    AnalyticsMessages.sendDataForInitSugo(context, e);
                    return;
                } catch (Exception exception) {
                    Log.e(SUGO_TAG, "run: ", exception);
                    return;
                }

            }
            try {
                Message msg = Message.obtain();
                Map<String, Object> map = new HashMap<>();
                map.put("context", context);
                map.put("callback", callback);
                msg.obj = map;
                msg.what = 1;
                SugoInitHandler.sendMessage(msg);
            } catch (Exception e) {
                try {
                    AnalyticsMessages.sendDataForInitSugo(context, e);
                    SharedPreferences preferences = context.getSharedPreferences(ViewCrawler.ISSUGOINITIALIZE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(ViewCrawler.ISSUGOINITIALIZE, false);
                    editor.commit();
                    return;
                } catch (Exception exception) {
                    Log.e(SUGO_TAG, "run: ", exception);
                    return;
                }

            }
        }
    }

    private static Handler SugoInitHandler = new Handler() {


        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    try {
                        Map<String, Object> map = (HashMap) msg.obj;
                        Context context = (Context) map.get("context");
                        InitSugoCallback callback = (InitSugoCallback) map.get("callback");
                        SharedPreferences preferences = (context).getSharedPreferences(ViewCrawler.ISSUGOINITIALIZE, Context.MODE_PRIVATE);
                        boolean isSugoInitialize = preferences.getBoolean(ViewCrawler.ISSUGOINITIALIZE, false);
                        if (isSugoInitialize) {
                            SugoAPI.getInstance(context);
                            if (callback != null) callback.finish();
                            Log.i("Sugo", "SugoSDK 初始化成功！");
                        }
                    } catch (Exception e) {
                        try {
                            AnalyticsMessages.sendDataForInitSugo((Context) ((HashMap) msg.obj).get("context"), e);
                        } catch (Exception exception) {
                            return;
                        }
                        return;
                    }
                    break;
            }
        }
    };


    /**
     * This call is a no-op, and will be removed in future versions.
     *
     * @deprecated in 4.0.0, use io.sugo.android.SGConfig.FlushInterval application metadata instead
     */
    @Deprecated
    public static void setFlushInterval(Context context, long milliseconds) {
        Log.i(
                LOGTAG,
                "SugoAPI.setFlushInterval is deprecated. Calling is now a no-op.\n" +
                        "    To set a custom Mixpanel flush interval for your application, add\n" +
                        "    <meta-data android:name=\"io.sugo.android.SGConfig.FlushInterval\" android:value=\"YOUR_INTERVAL\" />\n" +
                        "    to the <application> section of your AndroidManifest.xml."
        );
    }

    /**
     * This call is a no-op, and will be removed in future versions of the library.
     *
     * @deprecated in 4.0.0, use io.sugo.android.SGConfig.EventsFallbackEndpoint, io.sugo.android.SGConfig.PeopleFallbackEndpoint, or io.sugo.android.SGConfig.DecideFallbackEndpoint instead
     */
    @Deprecated
    public static void enableFallbackServer(Context context, boolean enableIfTrue) {
        Log.i(
                LOGTAG,
                "SugoAPI.enableFallbackServer is deprecated. This call is a no-op.\n" +
                        "    To enable fallback in your application, add\n" +
                        "    <meta-data android:name=\"io.sugo.android.SGConfig.DisableFallback\" android:value=\"false\" />\n" +
                        "    to the <application> section of your AndroidManifest.xml."
        );
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    private String getCurrentSessionId() {
        return mSessionId;
    }

    public void updateSessionId() {
        mSessionId = this.generateSessionId();
    }

    private void restorePageInfo() {
        final String sharedPrefsName = ViewCrawler.SHARED_PREF_EDITS_FILE + mToken;
        SharedPreferences preferences = mContext.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        final String storeInfo = preferences.getString(ViewCrawler.SHARED_PREF_PAGE_INFO_KEY, null);
        if (storeInfo != null && !storeInfo.equals("")) {
            try {
                SugoPageManager.getInstance().setPageInfos(new JSONArray(storeInfo));
            } catch (JSONException e) {
                e.printStackTrace();
                AnalyticsMessages.sendDataForInitSugo(mContext, e);
            }
        }
    }

    private void restoreDimensions() {
        final String sharedPrefsName = ViewCrawler.SHARED_PREF_EDITS_FILE + mToken;
        SharedPreferences preferences = mContext.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        final String storeInfo = preferences.getString(ViewCrawler.SHARED_PREF_DIMENSIONS_KEY, null);
        if (storeInfo != null && !storeInfo.equals("")) {
            try {
                SugoDimensionManager.getInstance().setDimensions(new JSONArray(storeInfo));
            } catch (JSONException e) {
                e.printStackTrace();
                AnalyticsMessages.sendDataForInitSugo(mContext, e);
            }
        }
    }

    private void restorePositionConfig() {
        final String sharedPrefsName = ViewCrawler.SHARED_PREF_EDITS_FILE + mToken;
        SharedPreferences preferences = mContext.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        SGConfig.positionConfig = preferences.getInt(ViewCrawler.POSITION_CONFIG, -1);
        SGConfig.lastReportLoaction = preferences.getLong(ViewCrawler.LAST_REPORT_LOCATION, -1);
    }


    /**
     * This function creates a distinct_id alias from alias to original. If original is null, then it will create an alias
     * to the current events distinct_id, which may be the distinct_id randomly generated by the Mixpanel library
     * before {@link #identify(String)} is called.
     * <p>
     * <p>This call does not identify the user after. You must still call both {@link #identify(String)} and
     *
     * @param alias    the new distinct_id that should represent original.
     * @param original the old distinct_id that alias will be mapped to.
     */
    public void alias(String alias, String original) {
        if (!SUGO_ENABLE) {
            return;
        }
        if (original == null) {
            original = getDistinctId();
        }
        if (alias.equals(original)) {
            Log.w(LOGTAG, "Attempted to alias identical distinct_ids " + alias + ". Alias message will not be sent.");
            return;
        }

        try {
            final JSONObject j = new JSONObject();
            j.put("alias", alias);
            j.put("original", original);
            track(null, "$create_alias", j);
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Failed to alias", e);
            AnalyticsMessages.sendDataForInitSugo(mContext, e);
        }
        flush();
    }

    /**
     * Associate all future calls to {@link #track(String, JSONObject)} with the user identified by
     * the given distinct id.
     * <p>
     * <p>
     * <p>Calls to {@link #track(String, JSONObject)} made before corresponding calls to
     * identify will use an internally generated distinct id, which means it is best
     * to call identify early to ensure that your Mixpanel funnels and retention
     * analytics can continue to track the user throughout their lifetime. We recommend
     * calling identify as early as you can.
     * <p>
     * <p>Once identify is called, the given distinct id persists across restarts of your
     * application.
     *
     * @param distinctId a string uniquely identifying this user. Events sent to
     *                   Mixpanel using the same disinct_id will be considered associated with the
     *                   same visitor/customer for retention and funnel reporting, so be sure that the given
     *                   value is globally unique for each individual user you intend to track.
     */
    public void identify(String distinctId) {
        if (!SUGO_ENABLE) {
            return;
        }
        synchronized (mPersistentIdentity) {
            mPersistentIdentity.setEventsDistinctId(distinctId);
            String decideId = mPersistentIdentity.getPeopleDistinctId();
            if (null == decideId) {
                decideId = mPersistentIdentity.getEventsDistinctId();
            }
            mDecideMessages.setDistinctId(decideId);
        }
    }

    /**
     * Begin timing of an event. Calling timeEvent("Thing") will not send an event, but
     * when you eventually call track("Thing"), your tracked event will be sent with a "$duration"
     * property, representing the number of seconds between your calls.
     *
     * @param eventName the name of the event to track with timing.
     */
    public void timeEvent(@NonNull final String eventName) {
        timeEvent(eventName, "", 0);
    }

    public void timeEvent(@NonNull final String eventName, @NonNull final String tag) {
        timeEvent(eventName, tag, 0);
    }

    public void timeEvent(@NonNull final String eventName, @NonNull String tag, long offset) {
        if (!SUGO_ENABLE) {
            return;
        }
        final long writeTime = System.currentTimeMillis() + offset;
        synchronized (mEventTimings) {
            String timeEventName = eventName + tag;
            mEventTimings.put(timeEventName, writeTime);
            mPersistentIdentity.addTimeEvent(timeEventName, writeTime);
        }
    }

    /**
     * Track an event.
     * <p>
     * <p>Every call to track eventually results in a data point sent to Mixpanel. These data points
     * are what are measured, counted, and broken down to create your Mixpanel reports. Events
     * have a string name, and an optional set of name/value pairs that describe the properties of
     * that event.
     *
     * @param eventName  The name of the event to send
     * @param properties A Map containing the key value pairs of the properties to include in this event.
     *                   Pass null if no extra properties exist.
     *                   <p>
     *                   See also {@link #track(String, JSONObject)}
     */
    public void trackMap(@NonNull String eventName, Map<String, Object> properties) {
        if (!SUGO_ENABLE) {
            return;
        }
        if (null == properties) {
            track(null, eventName, null);
        } else {
            try {
                track(null, eventName, new JSONObject(properties));
            } catch (NullPointerException e) {
                Log.w(LOGTAG, "Can't have null keys in the properties of trackMap!");
                AnalyticsMessages.sendDataForInitSugo(mContext, e);
            }
        }
    }

    public void track(@NonNull String eventName, JSONObject properties) {
        track(null, eventName, properties);
    }

    /**
     * Track an event.
     * <p>
     * <p>Every call to track eventually results in a data point sent to Mixpanel. These data points
     * are what are measured, counted, and broken down to create your Mixpanel reports. Events
     * have a string name, and an optional set of name/value pairs that describe the properties of
     * that event.
     *
     * @param eventName  The name of the event to send
     * @param properties A JSONObject containing the key value pairs of the properties to include in this event.
     *                   Pass null if no extra properties exist.
     */
    // DO NOT DOCUMENT, but track() must be thread safe since it is used to track events in
    // notifications from the UI thread, which might not be our SugoAPI "home" thread.
    // This MAY CHANGE IN FUTURE RELEASES, so minimize code that assumes thread safety
    // (and perhaps document that code here).
    public void track(String eventId, @NonNull String eventName, JSONObject properties) {
        try {
            if (!SUGO_ENABLE) {
                return;
            }
            if (eventName.trim().equals("")) {
                Log.e("SugoAPI.track", "track failure. eventName can't be empty");
                return;
            }
            if (SugoAPI.developmentMode) {
                Toast.makeText(this.mContext, eventName, Toast.LENGTH_SHORT).show();
            }
            final Long eventBegin;
            synchronized (mEventTimings) {
                String timeEventName = eventName;
                if (properties != null && properties.has(SGConfig.TIME_EVENT_TAG)) {
                    timeEventName = eventName + properties.optString(SGConfig.TIME_EVENT_TAG, "");
                    properties.remove(SGConfig.TIME_EVENT_TAG);
                }
                eventBegin = mEventTimings.get(timeEventName);
                mEventTimings.remove(timeEventName);
                mPersistentIdentity.removeTimeEvent(timeEventName);
            }

            try {
                final JSONObject messageProps = new JSONObject();

//            if (mConfig.ismEnableLocation()){
//                double[] loc = getLngAndLat(mContext);
//                messageProps.put(SGConfig.FIELD_LONGITUDE,  loc[0]);
//                messageProps.put(SGConfig.FIELD_LATITUDE,  loc[1]);
//            }

                messageProps.put(SGConfig.SESSION_ID, getCurrentSessionId());
                messageProps.put(SGConfig.FIELD_PAGE, SugoPageManager.getInstance().getCurrentPage(mContext));
                String path_name = messageProps.getString(SGConfig.FIELD_PAGE);
                if (!properties.getString(SGConfig.FIELD_PAGE).isEmpty()){
                    path_name = properties.getString(SGConfig.FIELD_PAGE);
                }
                messageProps.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance().getCurrentPageName(path_name));
                messageProps.put(SGConfig.FIELD_PAGE_CATEGORY, SugoPageManager.getInstance().getCurrentPageCategory(mContext));

                final Map<String, String> referrerProperties = mPersistentIdentity.getReferrerProperties();
                for (final Map.Entry<String, String> entry : referrerProperties.entrySet()) {
                    final String key = entry.getKey();
                    final String value = entry.getValue();
                    messageProps.put(key, value);
                }

                mPersistentIdentity.addSuperPropertiesToObject(messageProps);

                // Don't allow super properties or referral properties to override these fields,
                // but DO allow the caller to override them in their given properties.
                final double timeSecondsDouble = (System.currentTimeMillis()) / 1000.0;
                //final long timeSeconds = (long) timeSecondsDouble;
                messageProps.put(SGConfig.FIELD_TIME, System.currentTimeMillis());
                messageProps.put(SGConfig.FIELD_DISTINCT_ID, getDistinctId());

                if (null != eventBegin) {
                    final double eventBeginDouble = ((double) eventBegin) / 1000.0;
                    final double secondsElapsed = timeSecondsDouble - eventBeginDouble;
                    messageProps.put(SGConfig.FIELD_DURATION, new BigDecimal(secondsElapsed).setScale(2, BigDecimal.ROUND_HALF_UP));
                }

                if (null != properties) {
                    final Iterator<?> propIter = properties.keys();
                    while (propIter.hasNext()) {
                        final String key = (String) propIter.next();
                        messageProps.put(key, properties.get(key));
                    }
                }

                final String eventTypeValue = messageProps.optString(SGConfig.FIELD_EVENT_TYPE);
                if (TextUtils.isEmpty(eventTypeValue)) {
                    messageProps.put(SGConfig.FIELD_EVENT_TYPE, eventName);
                } else {
                    String newValue;
                    switch (eventTypeValue) {
                        case "click":
                            newValue = "点击";
                            break;
                        case "focus":
                            newValue = "对焦";
                            break;
                        case "change":
                            newValue = "改变";
                            break;
                        default:
                            newValue = eventTypeValue;
                            break;
                    }
                    messageProps.put(SGConfig.FIELD_EVENT_TYPE, newValue);
                }

                if (SugoAPI.developmentMode) {
                    JSONArray events = new JSONArray();
                    JSONObject event = new JSONObject();
                    event.put(SGConfig.FIELD_EVENT_ID, eventId);
                    event.put(SGConfig.FIELD_EVENT_NAME, eventName);
                    final Iterator<?> propIter = messageProps.keys();
                    while (propIter.hasNext()) {
                        final String key = (String) propIter.next();
                        Object value = messageProps.get(key);
                        if (value instanceof Date) {
                            messageProps.put(key, ((Date) value).getTime());
                        }
                    }
                    JSONObject defaultDimensions = mMessages.getDefaultEventProperties();
                    final Iterator<String> defaultPropIter = defaultDimensions.keys();
                    while (defaultPropIter.hasNext()) {
                        final String key = defaultPropIter.next();
                        Object value = defaultDimensions.get(key);
                        if (value instanceof Date) {
                            messageProps.put(key, ((Date) value).getTime());
                        } else {
                            messageProps.put(key, value);
                        }
                    }
                    event.put("properties", messageProps);
                    events.put(event);
                    mUpdatesFromMixpanel.sendTestEvent(events);
                } else {
                    final AnalyticsMessages.EventDescription eventDescription =
                            new AnalyticsMessages.EventDescription(eventId, eventName, messageProps, mToken);
                    mMessages.eventsMessage(eventDescription);
                }
//            if (null != mTrackingDebug) {
//                mTrackingDebug.reportTrack(eventName);
//            }
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Exception tracking event " + eventName, e);
            }
        } catch (Exception e) {
            Log.e(LOGTAG, "track method: " + e.toString());
        }

    }

    /**
     * Equivalent to {@link #track(String, JSONObject)} with a null argument for properties.
     * Consider adding properties to your tracking to get the best insights and experience from Mixpanel.
     *
     * @param eventName the name of the event to send
     */
    public void track(@NonNull String eventName) {
        track(null, eventName, null);
    }

    /**
     * Push all queued Mixpanel events and People Analytics changes to Mixpanel servers.
     * <p>
     * <p>Events and People messages are pushed gradually throughout
     * the lifetime of your application. This means that to ensure that all messages
     * are sent to Mixpanel when your application is shut down, you will
     * need to call flush() to let the Mixpanel library know it should
     * send all remaining messages to the server. We strongly recommend
     * placing a call to flush() in the onDestroy() method of
     * your main application activity.
     */
    public void flush() {
        if (!SUGO_ENABLE) {
            return;
        }
        mMessages.postToServer();
    }

    /**
     * Returns a json object of the user's current super properties
     * <p>
     * <p>SuperProperties are a collection of properties that will be sent with every event to Mixpanel,
     * and persist beyond the lifetime of your application.
     */
    public JSONObject getSuperProperties() {
        if (!SUGO_ENABLE) {
            return new JSONObject();
        }
        JSONObject ret = new JSONObject();
        mPersistentIdentity.addSuperPropertiesToObject(ret);
        return ret;
    }

    /**
     * Returns the string id currently being used to uniquely identify the user associated
     * with events sent using {@link #track(String, JSONObject)}. Before any calls to
     * {@link #identify(String)}, this will be an id automatically generated by the library.
     *
     * @return The distinct id associated with event tracking
     * @see #identify(String)
     */
    public String getDistinctId() {
        if (!SUGO_ENABLE) {
            return "";
        }
        return mPersistentIdentity.getEventsDistinctId();
    }

    /**
     * Register properties that will be sent with every subsequent call to {@link #track(String, JSONObject)}.
     * <p>
     * <p>SuperProperties are a collection of properties that will be sent with every event to Mixpanel,
     * and persist beyond the lifetime of your application.
     * <p>
     * <p>Setting a superProperty with registerSuperProperties will store a new superProperty,
     * possibly overwriting any existing superProperty with the same name (to set a
     * superProperty only if it is currently unset, use {@link #registerSuperPropertiesOnce(JSONObject)})
     * <p>
     * <p>SuperProperties will persist even if your application is taken completely out of memory.
     * to remove a superProperty, call {@link #unregisterSuperProperty(String)} or {@link #clearSuperProperties()}
     *
     * @param superProperties A Map containing super properties to register
     *                        <p>
     *                        See also {@link #registerSuperProperties(JSONObject)}
     */
    public void registerSuperPropertiesMap(Map<String, Object> superProperties) {
        if (!SUGO_ENABLE) {
            return;
        }
        if (null == superProperties) {
            Log.e(LOGTAG, "registerSuperPropertiesMap does not accept null properties");
            return;
        }

        try {
            registerSuperProperties(new JSONObject(superProperties));
        } catch (NullPointerException e) {
            Log.w(LOGTAG, "Can't have null keys in the properties of registerSuperPropertiesMap");
        }
    }

    /**
     * Register properties that will be sent with every subsequent call to {@link #track(String, JSONObject)}.
     * <p>
     * <p>SuperProperties are a collection of properties that will be sent with every event to Mixpanel,
     * and persist beyond the lifetime of your application.
     * <p>
     * <p>Setting a superProperty with registerSuperProperties will store a new superProperty,
     * possibly overwriting any existing superProperty with the same name (to set a
     * superProperty only if it is currently unset, use {@link #registerSuperPropertiesOnce(JSONObject)})
     * <p>
     * <p>SuperProperties will persist even if your application is taken completely out of memory.
     * to remove a superProperty, call {@link #unregisterSuperProperty(String)} or {@link #clearSuperProperties()}
     *
     * @param superProperties A JSONObject containing super properties to register
     * @see #registerSuperPropertiesOnce(JSONObject)
     * @see #unregisterSuperProperty(String)
     * @see #clearSuperProperties()
     */
    public void registerSuperProperties(JSONObject superProperties) {
        if (!SUGO_ENABLE) {
            return;
        }
        mPersistentIdentity.registerSuperProperties(superProperties);
    }

    public static void setSuperPropertiesBeforeStartSugo(Context context, String key, String value) {
        if (!SUGO_ENABLE) {
            return;
        }
        if (context == null || TextUtils.isEmpty(key)) {
            return;
        }
        try {
            SugoAPI instance = sInstanceMap.get(context.getApplicationContext());
            if (instance != null) {     // 证明初始化过 SugoAPI.getInstance
                JSONObject object = new JSONObject();
                object.put(key, value);
                instance.registerSuperProperties(object);
            } else {
                sPreSuperProps.put(key, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AnalyticsMessages.sendDataForInitSugo(context, e);
        }
    }

    public static void setSuperPropertiesOnceBeforeStartSugo(Context context, String key, String value) {
        if (!SUGO_ENABLE) {
            return;
        }
        if (context == null || TextUtils.isEmpty(key)) {
            return;
        }
        try {
            SugoAPI instance = sInstanceMap.get(context.getApplicationContext());
            if (instance != null) {     // 证明初始化过 SugoAPI.getInstance
                JSONObject object = new JSONObject();
                object.put(key, value);
                instance.registerSuperPropertiesOnce(object);
            } else {
                sPreSuperPropsOnce.put(key, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AnalyticsMessages.sendDataForInitSugo(context, e);
        }
    }

    /**
     * Remove a single superProperty, so that it will not be sent with future calls to {@link #track(String, JSONObject)}.
     * <p>
     * <p>If there is a superProperty registered with the given name, it will be permanently
     * removed from the existing superProperties.
     * To clear all superProperties, use {@link #clearSuperProperties()}
     *
     * @param superPropertyName name of the property to unregister
     * @see #registerSuperProperties(JSONObject)
     */
    public void unregisterSuperProperty(String superPropertyName) {
        if (!SUGO_ENABLE) {
            return;
        }
        mPersistentIdentity.unregisterSuperProperty(superPropertyName);
    }

    /**
     * Register super properties for events, only if no other super property with the
     * same names has already been registered.
     * <p>
     * <p>Calling registerSuperPropertiesOnce will never overwrite existing properties.
     *
     * @param superProperties A Map containing the super properties to register.
     *                        <p>
     *                        See also {@link #registerSuperPropertiesOnce(JSONObject)}
     */
    public void registerSuperPropertiesOnceMap(Map<String, Object> superProperties) {
        if (!SUGO_ENABLE) {
            return;
        }
        if (null == superProperties) {
            Log.e(LOGTAG, "registerSuperPropertiesOnceMap does not accept null properties");
            return;
        }

        try {
            registerSuperPropertiesOnce(new JSONObject(superProperties));
        } catch (NullPointerException e) {
            Log.w(LOGTAG, "Can't have null keys in the properties of registerSuperPropertiesOnce!");

        }
    }

    /**
     * Register super properties for events, only if no other super property with the
     * same names has already been registered.
     * <p>
     * <p>Calling registerSuperPropertiesOnce will never overwrite existing properties.
     *
     * @param superProperties A JSONObject containing the super properties to register.
     * @see #registerSuperProperties(JSONObject)
     */
    public void registerSuperPropertiesOnce(JSONObject superProperties) {
        if (!SUGO_ENABLE) {
            return;
        }
        mPersistentIdentity.registerSuperPropertiesOnce(superProperties);
    }

    /**
     * Erase all currently registered superProperties.
     * <p>
     * <p>Future tracking calls to Mixpanel will not contain the specific
     * superProperties registered before the clearSuperProperties method was called.
     * <p>
     * <p>To remove a single superProperty, use {@link #unregisterSuperProperty(String)}
     *
     * @see #registerSuperProperties(JSONObject)
     */
    public void clearSuperProperties() {
        if (!SUGO_ENABLE) {
            return;
        }
        mPersistentIdentity.clearSuperProperties();
    }

    /**
     * Updates super properties in place. Given a SuperPropertyUpdate object, will
     * pass the current values of SuperProperties to that update and replace all
     * results with the return value of the update. Updates are synchronized on
     * the underlying super properties store, so they are guaranteed to be thread safe
     * (but long running updates may slow down your tracking.)
     *
     * @param update A function from one set of super properties to another. The update should not return null.
     */
    public void updateSuperProperties(SuperPropertyUpdate update) {
        if (!SUGO_ENABLE) {
            return;
        }
        mPersistentIdentity.updateSuperProperties(update);
    }


    /**
     * Clears all distinct_ids, superProperties, and push registrations from persistent storage.
     * Will not clear referrer information.
     */
    public void reset() {
        if (!SUGO_ENABLE) {
            return;
        }
        // Will clear distinct_ids, superProperties, notifications, surveys, experiments,
        // and waiting People Analytics properties. Will have no effect
        // on messages already queued to send with AnalyticsMessages.
        mPersistentIdentity.clearPreferences();
        identify(getDistinctId());
        flush();
    }

    /**
     * Returns an unmodifiable map that contains the device description properties
     * that will be sent to Mixpanel. These are not all of the default properties,
     * but are a subset that are dependant on the user's device or installed version
     * of the host application, and are guaranteed not to change while the app is running.
     */
    public Map<String, String> getDeviceInfo() {
        if (!SUGO_ENABLE) {
            return new Hashtable<>();
        }
        return mDeviceInfo;
    }


    /**
     * This method is a no-op, kept for compatibility purposes.
     * <p>
     * To enable verbose logging about communication with Mixpanel, add
     * {@code
     * <meta-data android:name="io.sugo.android.SGConfig.EnableDebugLogging" />
     * }
     * <p>
     * To the {@code <application>} tag of your AndroidManifest.xml file.
     *
     * @deprecated in 4.1.0, use Manifest meta-data instead
     */
    @Deprecated
    public void logPosts() {
        Log.i(
                LOGTAG,
                "SugoAPI.logPosts() is deprecated.\n" +
                        "    To get verbose debug level logging, add\n" +
                        "    <meta-data android:name=\"io.sugo.android.SGConfig.EnableDebugLogging\" value=\"true\" />\n" +
                        "    to the <application> section of your AndroidManifest.xml."
        );
    }

    /**
     * Attempt to register SugoActivityLifecycleCallbacks to the application's event lifecycle.
     * Once registered, we can automatically check for and show surveys and in-app notifications
     * when any Activity is opened.
     * <p>
     * This is only available if the android version is >= 16. You can disable livecycle callbacks by setting
     * io.sugo.android.SGConfig.AutoShowMixpanelUpdates to false in your AndroidManifest.xml
     * <p>
     * This function is automatically called when the library is initialized unless you explicitly
     * set io.sugo.android.SGConfig.AutoShowMixpanelUpdates to false in your AndroidManifest.xml
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    /* package */ void registerMixpanelActivityLifecycleCallbacks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mContext.getApplicationContext() instanceof Application) {
                final Application app = (Application) mContext.getApplicationContext();
                mSugoActivityLifecycleCallbacks = new SugoActivityLifecycleCallbacks(this, mConfig);
                app.registerActivityLifecycleCallbacks(mSugoActivityLifecycleCallbacks);
            } else {
                Log.i(LOGTAG, "Context is not an Application, Mixpanel will not automatically show surveys, in-app notifications, or A/B test experiments. We won't be able to automatically flush on an app background.");
            }
        }
    }

    // Package-level access. Used (at least) by GCMReceiver
    // when OS-level events occur.
    /* package */ interface InstanceProcessor {
        public void process(SugoAPI m);
    }

    /* package */
    static void allInstances(InstanceProcessor processor) {
        synchronized (sInstanceMap) {
            for (final SugoAPI instance : sInstanceMap.values()) {
                processor.process(instance);

            }
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Conveniences for testing. These methods should not be called by
    // non-test client code.

    /* package */ AnalyticsMessages getAnalyticsMessages() {
        return AnalyticsMessages.getInstance(mContext);
    }

    /* package */ PersistentIdentity getPersistentIdentity(final Context context, Future<SharedPreferences> referrerPreferences, final String token) {
        final SharedPreferencesLoader.OnPrefsLoadedListener listener = new SharedPreferencesLoader.OnPrefsLoadedListener() {
            @Override
            public void onPrefsLoaded(SharedPreferences preferences) {
                final JSONArray records = PersistentIdentity.waitingPeopleRecordsForSending(preferences);
                if (null != records) {
                    sendAllPeopleRecords(records);
                }
            }
        };

        final String prefsName = "io.sugo.android.mpmetrics.MixpanelAPI_" + token;
        final Future<SharedPreferences> storedPreferences = sPrefsLoader.loadPreferences(context, prefsName, listener);

        final String timeEventsPrefsName = "SugoAPI.TimeEvents_" + token;
        final Future<SharedPreferences> timeEventsPrefs = sPrefsLoader.loadPreferences(context, timeEventsPrefsName, null);

        return new PersistentIdentity(referrerPreferences, storedPreferences, timeEventsPrefs);
    }

    /* package */ DecideMessages constructDecideUpdates(final String token, final DecideMessages.OnNewResultsListener listener, UpdatesFromMixpanel updatesFromMixpanel) {
        return new DecideMessages(token, listener, updatesFromMixpanel);
    }

    /* package */ UpdatesListener constructUpdatesListener() {
        if (Build.VERSION.SDK_INT < SGConfig.UI_FEATURES_MIN_API || (!SUGO_ENABLE)) {
            Log.i(LOGTAG, "Surveys and Notifications are not supported on this Android OS Version");
            return new UnsupportedUpdatesListener();
        } else {
            return new SupportedUpdatesListener();
        }
    }

    /* package */ UpdatesFromMixpanel constructUpdatesFromMixpanel(final Context context, final String token) {
        if (Build.VERSION.SDK_INT < SGConfig.UI_FEATURES_MIN_API) {
            Log.i(LOGTAG, "SDK version is lower than " + SGConfig.UI_FEATURES_MIN_API + ". Web Configuration, A/B Testing, and Dynamic Tweaks are disabled.");
            return new NoOpUpdatesFromMixpanel(sSharedTweaks);
        } else if (mConfig.getDisableViewCrawler() || Arrays.asList(mConfig.getDisableViewCrawlerForProjects()).contains(token)) {
            Log.i(LOGTAG, "DisableViewCrawler is set to true. Web Configuration, A/B Testing, and Dynamic Tweaks are disabled.");
            return new NoOpUpdatesFromMixpanel(sSharedTweaks);
        } else if (!SUGO_ENABLE) {
            if (SGConfig.DEBUG) {
                Log.i(LOGTAG, "sugo web configuration is disabled!");
            }
            return new NoOpUpdatesFromMixpanel(sSharedTweaks);
        } else {
            return new ViewCrawler(context, mToken, this, sSharedTweaks);
        }
    }

    /* package */ TrackingDebug constructTrackingDebug() {
        if (mUpdatesFromMixpanel instanceof ViewCrawler) {
            return (TrackingDebug) mUpdatesFromMixpanel;
        }

        return null;
    }

    /* package */ boolean sendAppOpen() {
        return !mConfig.getDisableAppOpenEvent();
    }

    ///////////////////////


    private interface UpdatesListener extends DecideMessages.OnNewResultsListener {
        public void addOnMixpanelUpdatesReceivedListener(OnMixpanelUpdatesReceivedListener listener);

        public void removeOnMixpanelUpdatesReceivedListener(OnMixpanelUpdatesReceivedListener listener);
    }

    private class UnsupportedUpdatesListener implements UpdatesListener {
        @Override
        public void onNewResults() {
            // Do nothing, these features aren't supported in older versions of the library
        }

        @Override
        public void addOnMixpanelUpdatesReceivedListener(OnMixpanelUpdatesReceivedListener listener) {
            // Do nothing, not supported
        }

        @Override
        public void removeOnMixpanelUpdatesReceivedListener(OnMixpanelUpdatesReceivedListener listener) {
            // Do nothing, not supported
        }
    }

    private class SupportedUpdatesListener implements UpdatesListener, Runnable {
        @Override
        public void onNewResults() {
            mExecutor.execute(this);
        }

        @Override
        public synchronized void addOnMixpanelUpdatesReceivedListener(OnMixpanelUpdatesReceivedListener listener) {
            if (mDecideMessages.hasUpdatesAvailable()) {
                onNewResults();
            }

            mListeners.add(listener);
        }

        @Override
        public synchronized void removeOnMixpanelUpdatesReceivedListener(OnMixpanelUpdatesReceivedListener listener) {
            mListeners.remove(listener);
        }

        @Override
        public synchronized void run() {
            // It's possible that by the time this has run the updates we detected are no longer
            // present, which is ok.
            for (final OnMixpanelUpdatesReceivedListener listener : mListeners) {
                listener.onMixpanelUpdatesReceived();
            }
        }

        private final Set<OnMixpanelUpdatesReceivedListener> mListeners = new HashSet<OnMixpanelUpdatesReceivedListener>();
        private final Executor mExecutor = Executors.newSingleThreadExecutor();
    }

    /* package */ class NoOpUpdatesFromMixpanel implements UpdatesFromMixpanel {
        public NoOpUpdatesFromMixpanel(Tweaks tweaks) {
            mTweaks = tweaks;
        }

        @Override
        public void startUpdates() {
            // No op
        }

        @Override
        public void sendTestEvent(JSONArray events) {

        }

        @Override
        public void setEventBindings(JSONArray bindings) {
            // No op
        }

        @Override
        public void setH5EventBindings(JSONArray bindings) {
            // No op
        }

        @Override
        public void setPageInfos(JSONArray pageInfos) {

        }

        @Override
        public void setDimensions(JSONArray dimensions) {

        }

        @Override
        public void setVariants(JSONArray variants) {
            // No op
        }

        @Override
        public Tweaks getTweaks() {
            return mTweaks;
        }

        @Override
        public void addOnMixpanelTweaksUpdatedListener(OnMixpanelTweaksUpdatedListener listener) {
            // No op
        }

        @Override
        public void removeOnMixpanelTweaksUpdatedListener(OnMixpanelTweaksUpdatedListener listener) {
            // No op
        }

        @Override
        public boolean sendConnectEditor(Uri data) {
            return false;
        }

        private final Tweaks mTweaks;
    }

    ////////////////////////////////////////////////////

    private void recordPeopleMessage(JSONObject message) {
        if (message.has("$distinct_id")) {
            mMessages.peopleMessage(message);
        } else {
            mPersistentIdentity.storeWaitingPeopleRecord(message);
        }
    }

    private void pushWaitingPeopleRecord() {
        final JSONArray records = mPersistentIdentity.waitingPeopleRecordsForSending();
        if (null != records) {
            sendAllPeopleRecords(records);
        }
    }

    // MUST BE THREAD SAFE. Called from crazy places. mPersistentIdentity may not exist
    // when this is called (from its crazy thread)
    private void sendAllPeopleRecords(JSONArray records) {
        for (int i = 0; i < records.length(); i++) {
            try {
                final JSONObject message = records.getJSONObject(i);
                mMessages.peopleMessage(message);
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Malformed people record stored pending identity, will not send it.", e);
            }
        }
    }


    public void handleWebView(WebView webView) {

        handleWebView(webView, mConfig.getToken(), null);
    }

    public void handleWebView(WebView webView, String token, SugoWebViewClient webViewClient) {
        webView.setVisibility(View.VISIBLE);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        if (webViewClient == null) {
            webViewClient = new SugoWebViewClient();
        }
        webView.setWebViewClient(webViewClient);
        addWebViewJavascriptInterface(webView);
    }

    public void addWebViewJavascriptInterface(WebView webView) {
        webView.addJavascriptInterface(new SugoWebEventListener(this), "sugoEventListener");
        SugoWebNodeReporter reporter = new SugoWebNodeReporter();
        webView.addJavascriptInterface(reporter, "sugoWebNodeReporter");
        setSugoWebNodeReporter(webView, reporter);
    }

    public void addWebViewJavascriptInterface(WebViewDelegate delegate) {
        delegate.addJavascriptInterface(new SugoWebEventListener(this), "sugoEventListener");
        SugoWebNodeReporter reporter = new SugoWebNodeReporter();
        delegate.addJavascriptInterface(reporter, "sugoWebNodeReporter");
        setSugoWebNodeReporter(delegate, reporter);
    }

    public void addWebViewJavascriptInterface(XWalkView xWalkView) {
        xWalkView.addJavascriptInterface(new SugoWebEventListener(this), "sugoEventListener");
        SugoWebNodeReporter reporter = new SugoWebNodeReporter();
        xWalkView.addJavascriptInterface(reporter, "sugoWebNodeReporter");
        setSugoWebNodeReporter(xWalkView, reporter);
    }

    public SGConfig getConfig() {
        return mConfig;
    }

    public static SugoWebNodeReporter getSugoWebNodeReporter(Object key) {
        return SugoWebEventListener.sugoWNReporter.get(key);
    }

    public static void setSugoWebNodeReporter(Object key, SugoWebNodeReporter sugoWebNodeReporter) {
        SugoWebEventListener.sugoWNReporter.put(key, sugoWebNodeReporter);
    }

    /**
     * 连接到 Editor
     *
     * @param data 例如: sugo.9db31f867e0b54b2744e48dde0a3d1bb://sugo/?sKey=e628da6c344acf503bc1b0574326f3b4
     */
    public void connectEditor(Uri data) {
        if (!SUGO_ENABLE) {
            return;
        }
        mUpdatesFromMixpanel.sendConnectEditor(data);
    }

    /**
     * 禁用记录一个 Activity 实例的生命周期
     *
     * @param activity
     */
    public void disableTraceActivity(Activity activity) {
        if (!SUGO_ENABLE) {
            return;
        }
        mSugoActivityLifecycleCallbacks.disableTraceActivity(activity);
    }

    public void traceFragmentResumed(Fragment fragment, String pageName) {
        traceFragment("浏览", "", fragment.getClass().getCanonicalName(), pageName);
        timeEvent("窗口停留", fragment.hashCode() + "");
    }

    public void traceFragmentResumed(android.support.v4.app.Fragment fragment, String pageName) {
        traceFragment("浏览", "", fragment.getClass().getCanonicalName(), pageName);
        timeEvent("窗口停留", fragment.hashCode() + "");
    }

    public void traceFragmentPaused(Fragment fragment, String pageName) {
        traceFragment("窗口停留", fragment.hashCode() + "", fragment.getClass().getCanonicalName(), pageName);
    }

    public void traceFragmentPaused(android.support.v4.app.Fragment fragment, String pageName) {
        traceFragment("窗口停留", fragment.hashCode() + "", fragment.getClass().getCanonicalName(), pageName);
    }

    private void traceFragment(String eventName, String timeEventTag, String pathName, String pageName) {
        if (!SUGO_ENABLE) {
            return;
        }
        JSONObject props = new JSONObject();
        try {
            props.put(SGConfig.FIELD_PAGE, pathName);
            props.put(SGConfig.FIELD_PAGE_NAME, pageName);
            if (timeEventTag != null && !timeEventTag.equals("")) {
                props.put(SGConfig.TIME_EVENT_TAG, timeEventTag);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        track(eventName, props);
    }

    private final Context mContext;
    private final AnalyticsMessages mMessages;
    private final SGConfig mConfig;
    private final String mToken;
    private String mSessionId;
    private final UpdatesFromMixpanel mUpdatesFromMixpanel;
    private final PersistentIdentity mPersistentIdentity;
    private final UpdatesListener mUpdatesListener;
    private final TrackingDebug mTrackingDebug;
    private final DecideMessages mDecideMessages;
    private final Map<String, String> mDeviceInfo;
    private final Map<String, Long> mEventTimings;
    private SugoActivityLifecycleCallbacks mSugoActivityLifecycleCallbacks;
    private final Map<String, String> classAttributeDict;

    // Maps each token to a singleton SugoAPI instance
    private static final Map<Context, SugoAPI> sInstanceMap = new HashMap<Context, SugoAPI>();
    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    private static final Tweaks sSharedTweaks = new Tweaks();
    private static Future<SharedPreferences> sReferrerPrefs;

    private static final String LOGTAG = "SugoAPI.API";
    private static final String APP_LINKS_LOGTAG = "SugoAPI.AL";
    private static final String ENGAGE_DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss";

    private boolean mDisableDecideChecker;

    // SugoAPI 实例化之前设置 superProperties 的临时变量
    private static JSONObject sPreSuperProps = new JSONObject();
    private static JSONObject sPreSuperPropsOnce = new JSONObject();


    public Map<String, String> getClassAttributeDict() {
        return classAttributeDict;
    }


    /**
     * 获取经纬度
     *
     * @param context
     * @return
     */
    public double[] getLngAndLat(Context context) {
        double latitude = 0.0;
        double longitude = 0.0;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {  //从gps获取经纬度
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            } else {//当GPS信号弱没获取到位置的时候又从网络获取
                return getLngAndLatWithNetwork(context);
            }
        } else {    //从网络获取经纬度
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        }
        double[] loc = {longitude, latitude};
        return loc;
    }

    public Context getCurrentContext() {
        return mContext;
    }

    public double[] getLngAndLatWithNetwork(Context context) {
        double latitude = 0.0;
        double longitude = 0.0;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        double[] loc = {longitude, latitude};
        return loc;
    }


    LocationListener locationListener = new LocationListener() {

        // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        // Provider被enable时触发此函数，比如GPS被打开
        @Override
        public void onProviderEnabled(String provider) {

        }

        // Provider被disable时触发此函数，比如GPS被关闭
        @Override
        public void onProviderDisabled(String provider) {

        }

        //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
        @Override
        public void onLocationChanged(Location location) {
        }
    };

}
