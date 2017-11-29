package io.sugo.android.mpmetrics;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import io.sugo.android.viewcrawler.TrackingDebug;
import io.sugo.android.viewcrawler.UpdatesFromSugo;
import io.sugo.android.viewcrawler.ViewCrawler;
import io.sugo.android.viewcrawler.XWalkViewListener;

/**
 * Core class for interacting with Sugo Analytics.
 * <p>
 * <p>Call {@link #getInstance(Context)} with
 * your main application activity and your Sugo API token as arguments
 * an to get an instance you can use to report how users are using your
 * application.
 * <p>
 * <p>Once you have an instance, you can send events to Sugo
 * using {@link #track(String, JSONObject)}
 * <p>
 * <p>The Sugo library will periodically send information to
 * Sugo servers, so your application will need to have
 * <tt>android.permission.INTERNET</tt>. In addition, to preserve
 * battery life, messages to Sugo servers may not be sent immediately
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
 *      SugoAPI mSugo;
 *
 *      public void onCreate(Bundle saved) {
 *          mSugo = SugoAPI.getInstance(this, "YOUR SUGO API TOKEN");
 *          ...
 *      }
 *
 *      public void whenSomethingInterestingHappens(int flavor) {
 *          JSONObject properties = new JSONObject();
 *          properties.put("flavor", flavor);
 *          mSugo.track("Something Interesting Happened", properties);
 *          ...
 *      }
 *
 *      public void onDestroy() {
 *          mSugo.flush();
 *          super.onDestroy();
 *      }
 * }
 * }
 * </pre>
 * <p>
 * <p>In addition to this documentation, you may wish to take a look at
 * <a href="https://github.com/sugo/sample-android-sugo-integration">the Sugo sample Android application</a>.
 * <p>
 * <p>There are also <a href="https://sugo.com/docs/">step-by-step getting started documents</a>
 * available at sugo.com
 *
 * @author Administrator
 * @see <a href="https://sugo.com/docs/integration-libraries/android">getting started documentation for tracking events</a>
 * @see <a href="https://sugo.com/docs/people-analytics/android">getting started documentation for People Analytics</a>
 * @see <a href="https://sugo.com/docs/people-analytics/android-push">getting started with push notifications for Android</a>
 * @see <a href="https://github.com/sugo/sample-android-sugo-integration">The Sugo Android sample application</a>
 */
public class SugoAPI {
    /**
     * String version of the library.
     */
    public static final String VERSION = SGConfig.VERSION;

    private static final String LOGTAG = "SugoAPI.API";

    public static boolean developmentMode = false;

    private final Context mContext;
    private final AnalyticsMessages mMessages;
    private final SGConfig mConfig;
    private final String mToken;
    private final String mSessionId;
    private final UpdatesFromSugo mUpdatesFromSugo;
    private final PersistentIdentity mPersistentIdentity;
    private final TrackingDebug mTrackingDebug;
    private final Map<String, String> mDeviceInfo;
    private final Map<String, Long> mEventTimings;
    private SugoActivityLifecycleCallbacks mSugoActivityLifecycleCallbacks;

    // Maps each token to a singleton SugoAPI instance
    private static final Map<Context, SugoAPI> sInstanceMap = new HashMap<Context, SugoAPI>();
    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();

    private static final String APP_LINKS_LOGTAG = "SugoAPI.AL";
    private static final String ENGAGE_DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss";

    private static final String KEY_USER_ID_LOGIN_TIME = "SUGO_USER_ID_LOGIN_TIME";
    private boolean mDisableDecideChecker;

    // SugoAPI 实例化之前设置 superProperties 的临时变量
    private static JSONObject sPreSuperProps = new JSONObject();
    private static JSONObject sPreSuperPropsOnce = new JSONObject();

    /**
     * You shouldn't instantiate SugoAPI objects directly.
     * Use SugoAPI.getInstance to get an instance.
     */

    SugoAPI(Context context) {
        this(context, SGConfig.getInstance(context));
    }

    /**
     * You shouldn't instantiate SugoAPI objects directly.
     * Use SugoAPI.getInstance to get an instance.
     */
    SugoAPI(Context context, SGConfig config) {
        Context appContext = context.getApplicationContext();
        mContext = appContext;
        mConfig = config;
        mToken = config.getToken();

        mSessionId = generateSessionId();

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
        } catch (final PackageManager.NameNotFoundException e) {
            Log.e(LOGTAG, "Exception getting app version name", e);
        }
        mDeviceInfo = Collections.unmodifiableMap(deviceInfo);

        mUpdatesFromSugo = constructUpdatesFromSugo(context, mToken);
        mTrackingDebug = constructTrackingDebug();
        mPersistentIdentity = getPersistentIdentity(appContext, mToken);
        registerSuperProperties(sPreSuperProps);
        registerSuperPropertiesOnce(sPreSuperPropsOnce);
        mEventTimings = mPersistentIdentity.getTimeEvents();

        // TODO reading persistent identify immediately forces the lazy load of the preferences, and defeats the
        // purpose of PersistentIdentity's laziness.
        String distinctId = mPersistentIdentity.getEventsDistinctId();
        mConfig.setDistinctId(distinctId);
        mMessages = getAnalyticsMessages(mUpdatesFromSugo);

        if (!mConfig.getDisableDecideChecker()) {
            mMessages.startApiCheck();
        }

        if (!mPersistentIdentity.hasTrackedIntegration()) {
            Map<String, Object> firstVisitTimeMap = new HashMap<>();
            firstVisitTimeMap.put(SGConfig.FIELD_FIRST_VISIT_TIME, System.currentTimeMillis());
            registerSuperPropertiesMap(firstVisitTimeMap);
            track("首次访问");
            track("APP安装");
            flush();
            mPersistentIdentity.setTrackedIntegration(true);
        }

        registerSugoActivityLifecycleCallbacks();

        mUpdatesFromSugo.startUpdates();
    }

    /**
     * Get the instance of SugoAPI associated with your Sugo project token.
     * <p>
     * <p>Use getInstance to get a reference to a shared
     * instance of SugoAPI you can use to send events
     * and People Analytics updates to Sugo.</p>
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

            SugoAPI instance = sInstanceMap.get(appContext);
            if (null == instance && ConfigurationChecker.checkBasicConfiguration(appContext)) {
                instance = new SugoAPI(context);
                sInstanceMap.put(appContext, instance);
            }

            return instance;
        }
    }

    public static void startSugo(Context context, SGConfig sgConfig) {
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
            Log.e(LOGTAG, "未检测到 SugoSDK 的 Token，请正确设置 io.sugo.android.SGConfig.token");
            return;
        }
        if (TextUtils.isEmpty(sgConfig.getProjectId())) {
            Log.e(LOGTAG, "未检测到 SugoSDK 的 ProjectId，请正确设置 io.sugo.android.SGConfig.ProjectId");
            return;
        }

        SugoAPI.getInstance(context);
        Log.i("Sugo", "SugoSDK 初始化成功！");
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    private String getCurrentSessionId() {
        return mSessionId;
    }

    /**
     * Associate all future calls to {@link #track(String, JSONObject)} with the user identified by
     * the given distinct id.
     * <p>
     * <p>
     * <p>Calls to {@link #track(String, JSONObject)} made before corresponding calls to
     * identify will use an internally generated distinct id, which means it is best
     * to call identify early to ensure that your Sugo funnels and retention
     * analytics can continue to track the user throughout their lifetime. We recommend
     * calling identify as early as you can.
     * <p>
     * <p>Once identify is called, the given distinct id persists across restarts of your
     * application.
     *
     * @param distinctId a string uniquely identifying this user. Events sent to
     *                   Sugo using the same disinct_id will be considered associated with the
     *                   same visitor/customer for retention and funnel reporting, so be sure that the given
     *                   value is globally unique for each individual user you intend to track.
     */
    public void identify(String distinctId) {
        synchronized (mPersistentIdentity) {
            mPersistentIdentity.setEventsDistinctId(distinctId);
            distinctId = mPersistentIdentity.getEventsDistinctId();
            mConfig.setDistinctId(distinctId);
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

    public void timeEvent(@NonNull final String eventName, @NonNull String tag) {
        timeEvent(eventName, tag, 0);
    }

    public void timeEvent(@NonNull final String eventName, @NonNull String tag, long offset) {
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
     * <p>Every call to track eventually results in a data point sent to Sugo. These data points
     * are what are measured, counted, and broken down to create your Sugo reports. Events
     * have a string name, and an optional set of name/value pairs that describe the properties of
     * that event.
     *
     * @param eventName  The name of the event to send
     * @param properties A Map containing the key value pairs of the properties to include in this event.
     *                   Pass null if no extra properties exist.
     *                   <p>
     *                   See also {@link #track(String, org.json.JSONObject)}
     */
    public void trackMap(@NonNull String eventName, Map<String, Object> properties) {
        if (null == properties) {
            track(null, eventName, null);
        } else {
            try {
                track(null, eventName, new JSONObject(properties));
            } catch (NullPointerException e) {
                Log.w(LOGTAG, "Can't have null keys in the properties of trackMap!");
            }
        }
    }

    public void track(@NonNull String eventName, JSONObject properties) {
        track(null, eventName, properties);
    }

    /**
     * Track an event.
     * <p>
     * <p>Every call to track eventually results in a data point sent to Sugo. These data points
     * are what are measured, counted, and broken down to create your Sugo reports. Events
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
        if (eventName.trim().equals("")) {
            Log.e("SugoAPI.track", "track failure. eventName can't be empty");
            return;
        }
        if (SugoAPI.developmentMode && isMainThread()) {
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

            messageProps.put(SGConfig.SESSION_ID, getCurrentSessionId());
            messageProps.put(SGConfig.FIELD_PAGE, SugoPageManager.getInstance().getCurrentPage(mContext));
            messageProps.put(SGConfig.FIELD_PAGE_NAME, SugoPageManager.getInstance().getCurrentPageName(mContext));
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
                if (SGConfig.DEBUG) {
                    Log.d("Track " + eventName + " " + SGConfig.FIELD_DURATION, "事件耗时：" + secondsElapsed + "秒");
                }
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
                        newValue = "聚焦";
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
                mUpdatesFromSugo.sendTestEvent(events);
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
    }

    /**
     * Equivalent to {@link #track(String, JSONObject)} with a null argument for properties.
     * Consider adding properties to your tracking to get the best insights and experience from Sugo.
     *
     * @param eventName the name of the event to send
     */
    public void track(@NonNull String eventName) {
        track(null, eventName, null);
    }

    /**
     * Push all queued Sugo events and People Analytics changes to Sugo servers.
     * <p>
     * <p>Events and People messages are pushed gradually throughout
     * the lifetime of your application. This means that to ensure that all messages
     * are sent to Sugo when your application is shut down, you will
     * need to call flush() to let the Sugo library know it should
     * send all remaining messages to the server. We strongly recommend
     * placing a call to flush() in the onDestroy() method of
     * your main application activity.
     */
    public void flush() {
        mMessages.postToServer();
    }

    /**
     * Returns a json object of the user's current super properties
     * <p>
     * <p>SuperProperties are a collection of properties that will be sent with every event to Sugo,
     * and persist beyond the lifetime of your application.
     */
    public JSONObject getSuperProperties() {
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
        return mPersistentIdentity.getEventsDistinctId();
    }

    /**
     * Register properties that will be sent with every subsequent call to {@link #track(String, JSONObject)}.
     * <p>
     * <p>SuperProperties are a collection of properties that will be sent with every event to Sugo,
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
     *                        See also {@link #registerSuperProperties(org.json.JSONObject)}
     */
    public void registerSuperPropertiesMap(Map<String, Object> superProperties) {
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
     * <p>SuperProperties are a collection of properties that will be sent with every event to Sugo,
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
        mPersistentIdentity.registerSuperProperties(superProperties);
    }

    public static void setSuperPropertiesBeforeStartSugo(Context context, String key, String value) {
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
        }
    }

    public static void setSuperPropertiesOnceBeforeStartSugo(Context context, String key, String value) {
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
     *                        See also {@link #registerSuperPropertiesOnce(org.json.JSONObject)}
     */
    public void registerSuperPropertiesOnceMap(Map<String, Object> superProperties) {
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
        mPersistentIdentity.registerSuperPropertiesOnce(superProperties);
    }

    /**
     * Erase all currently registered superProperties.
     * <p>
     * <p>Future tracking calls to Sugo will not contain the specific
     * superProperties registered before the clearSuperProperties method was called.
     * <p>
     * <p>To remove a single superProperty, use {@link #unregisterSuperProperty(String)}
     *
     * @see #registerSuperProperties(JSONObject)
     */
    public void clearSuperProperties() {
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
        mPersistentIdentity.updateSuperProperties(update);
    }


    /**
     * Clears all distinct_ids, superProperties, and push registrations from persistent storage.
     * Will not clear referrer information.
     */
    public void reset() {
        // Will clear distinct_ids, superProperties, notifications, surveys, experiments,
        // and waiting People Analytics properties. Will have no effect
        // on messages already queued to send with AnalyticsMessages.
        mPersistentIdentity.clearPreferences();
        identify(getDistinctId());
        flush();
    }

    /**
     * Returns an unmodifiable map that contains the device description properties
     * that will be sent to Sugo. These are not all of the default properties,
     * but are a subset that are dependant on the user's device or installed version
     * of the host application, and are guaranteed not to change while the app is running.
     */
    public Map<String, String> getDeviceInfo() {
        return mDeviceInfo;
    }

    /**
     * Attempt to register SugoActivityLifecycleCallbacks to the application's event lifecycle.
     * Once registered, we can automatically check for and show surveys and in-app notifications
     * when any Activity is opened.
     * <p>
     * This is only available if the android version is >= 16. You can disable livecycle callbacks by setting
     * io.sugo.android.SGConfig.AutoShowSugoUpdates to false in your AndroidManifest.xml
     * <p>
     * This function is automatically called when the library is initialized unless you explicitly
     * set io.sugo.android.SGConfig.AutoShowSugoUpdates to false in your AndroidManifest.xml
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void registerSugoActivityLifecycleCallbacks() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mContext.getApplicationContext() instanceof Application) {
                final Application app = (Application) mContext.getApplicationContext();
                mSugoActivityLifecycleCallbacks = new SugoActivityLifecycleCallbacks(this, mConfig);
                app.registerActivityLifecycleCallbacks(mSugoActivityLifecycleCallbacks);
            } else {
                Log.i(LOGTAG, "Context is not an Application, We won't be able to automatically flush on an app background.");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Conveniences for testing. These methods should not be called by
    // non-test client code.

    private AnalyticsMessages getAnalyticsMessages(UpdatesFromSugo updatesFromSugo) {
        return AnalyticsMessages.getInstance(mContext, updatesFromSugo);
    }

    private PersistentIdentity getPersistentIdentity(final Context context, final String token) {

        final Future<SharedPreferences> referrerPrefs = sPrefsLoader.loadPreferences(context, SGConfig.REFERRER_PREFS_NAME, null);

        final String prefsName = "io.sugo.android.mpmetrics.SugoAPI_" + token;
        final Future<SharedPreferences> storedPreferences = sPrefsLoader.loadPreferences(context, prefsName, null);

        final String timeEventsPrefsName = "SugoAPI.TimeEvents_" + token;
        final Future<SharedPreferences> timeEventsPrefs = sPrefsLoader.loadPreferences(context, timeEventsPrefsName, null);

        final String sugoPrefsName = "io.sugo.android.mpmetrics.SugoAPI_" + token;
        final Future<SharedPreferences> sugoPrefs = sPrefsLoader.loadPreferences(context, sugoPrefsName, null);

        return new PersistentIdentity(referrerPrefs, storedPreferences, timeEventsPrefs, sugoPrefs);
    }

    private UpdatesFromSugo constructUpdatesFromSugo(final Context context, final String token) {
        if (Build.VERSION.SDK_INT < SGConfig.UI_FEATURES_MIN_API) {
            Log.i(LOGTAG, "SDK version is lower than " + SGConfig.UI_FEATURES_MIN_API + ". Web Configuration are disabled.");
            return new NoOpUpdatesFromSugo();
        } else if (mConfig.getDisableViewCrawler() || Arrays.asList(mConfig.getDisableViewCrawlerForProjects()).contains(token)) {
            Log.i(LOGTAG, "DisableViewCrawler is set to true. Web Configuration are disabled.");
            return new NoOpUpdatesFromSugo();
        } else {
            return new ViewCrawler(context, mToken, this);
        }
    }

    private TrackingDebug constructTrackingDebug() {
        if (mUpdatesFromSugo instanceof ViewCrawler) {
            return (TrackingDebug) mUpdatesFromSugo;
        }

        return null;
    }

    class NoOpUpdatesFromSugo implements UpdatesFromSugo {
        public NoOpUpdatesFromSugo() {
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
        public void setXWalkViewListener(XWalkViewListener XWalkViewListener) {

        }

        @Override
        public boolean sendConnectEditor(Uri data) {
            return false;
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

    public SGConfig getConfig() {
        return mConfig;
    }

    public static SugoWebNodeReporter getSugoWebNodeReporter(Object key) {
        return SugoWebEventListener.sugoWNReporter.get(key);
    }

    public static void setSugoWebNodeReporter(Object key, SugoWebNodeReporter sugoWebNodeReporter) {
        SugoWebEventListener.sugoWNReporter.put(key, sugoWebNodeReporter);
    }

    public void setSnapshotViewListener(XWalkViewListener XWalkViewListener) {
        mUpdatesFromSugo.setXWalkViewListener(XWalkViewListener);
    }

    /**
     * 连接到 Editor
     *
     * @param data 例如: sugo.9db31f867e0b54b2744e48dde0a3d1bb://sugo/?sKey=e628da6c344acf503bc1b0574326f3b4
     */
    public void connectEditor(Uri data) {
        mUpdatesFromSugo.sendConnectEditor(data);
    }

    /**
     * 禁用记录一个 Activity 实例的生命周期
     *
     * @param activity
     */
    public void disableTraceActivity(Activity activity) {
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

    /**
     * @param userIdKey   "userId"
     * @param userIdValue "123456"
     */
    public void login(final String userIdKey, final String userIdValue) {
        final Map<String, Object> firstLoginTimeMap = new HashMap<>();
        if (!TextUtils.isEmpty(userIdKey)) {
            mPersistentIdentity.writeUserIdKey(userIdKey);
            firstLoginTimeMap.put(userIdKey, userIdValue);
        }

        long recordUserFirstLoginTime = mPersistentIdentity.readUserLoginTime(userIdValue);
        if (recordUserFirstLoginTime > 0) {
            firstLoginTimeMap.put(SGConfig.FIELD_FIRST_LOGIN_TIME, recordUserFirstLoginTime);
            registerSuperPropertiesMap(firstLoginTimeMap);
        } else {
            Thread loginThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (SGConfig.DEBUG) {
                            Log.i(LOGTAG, "query user first login time for : " + userIdValue);
                        }
                        StringBuilder urlBuilder = new StringBuilder();
                        urlBuilder.append(mConfig.getFirstLoginEndpoint())
                                .append("?userId=")
                                .append(userIdValue)
                                .append("&projectId=")
                                .append(mConfig.getProjectId())
                                .append("&token=")
                                .append(mConfig.getToken());
                        URL url = new URL(urlBuilder.toString());
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        if (urlConnection.getResponseCode() == 200) {
                            InputStream inputStream = urlConnection.getInputStream();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffers = new byte[1024];
                            int len = 0;
                            while (-1 != (len = inputStream.read(buffers))) {
                                baos.write(buffers, 0, len);
                                baos.flush();
                            }
                            String result = baos.toString("utf-8");
                            if (SGConfig.DEBUG) {
                                Log.i(LOGTAG, "query user first login time result : " + result);
                            }
                            JSONObject dataObj = new JSONObject(result);
                            boolean success = dataObj.optBoolean("success", false);
                            if (success && dataObj.has("result")
                                    && dataObj.getJSONObject("result").has("firstLoginTime")) {
                                long firstLoginTime = dataObj.getJSONObject("result").getLong("firstLoginTime");
                                firstLoginTimeMap.put(SGConfig.FIELD_FIRST_LOGIN_TIME, firstLoginTime);
                                registerSuperPropertiesMap(firstLoginTimeMap);
                                // 存储起来，下次调用 login 不再请求网络
                                mPersistentIdentity.writeUserLoginTime(userIdValue, firstLoginTime);
                                boolean firstLogin = dataObj.getJSONObject("result").optBoolean("isFirstLogin", false);
                                if (firstLogin) {
                                    track("首次登录");
                                }
                            }
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            loginThread.start();
        }
    }

    public void logout() {
        String userIdKey = mPersistentIdentity.readUserIdKey();
        if (userIdKey != null) {
            unregisterSuperProperty(userIdKey);
        }
        unregisterSuperProperty(SGConfig.FIELD_FIRST_LOGIN_TIME);
    }

    private boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

}
