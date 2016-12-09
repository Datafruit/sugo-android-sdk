package io.sugo.android.mpmetrics;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import io.sugo.android.viewcrawler.TrackingDebug;
import io.sugo.android.viewcrawler.UpdatesFromMixpanel;
import io.sugo.android.viewcrawler.ViewCrawler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Core class for interacting with Mixpanel Analytics.
 * <p>
 * <p>Call {@link #getInstance(Context, String)} with
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
        String token = mToken;

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

        mUpdatesFromMixpanel = constructUpdatesFromMixpanel(context, token);
        mTrackingDebug = constructTrackingDebug();
        mPersistentIdentity = getPersistentIdentity(appContext, referrerPreferences, token);
        mEventTimings = mPersistentIdentity.getTimeEvents();
        mUpdatesListener = constructUpdatesListener();
        mDecideMessages = constructDecideUpdates(token, mUpdatesListener, mUpdatesFromMixpanel);

        // TODO reading persistent identify immediately forces the lazy load of the preferences, and defeats the
        // purpose of PersistentIdentity's laziness.
        String decideId = mPersistentIdentity.getPeopleDistinctId();
        if (null == decideId) {
            decideId = mPersistentIdentity.getEventsDistinctId();
        }
        mDecideMessages.setDistinctId(decideId);
        mMessages = getAnalyticsMessages();

        if (!mConfig.getDisableDecideChecker()) {
            mMessages.installDecideCheck(mDecideMessages);
        }

        registerMixpanelActivityLifecycleCallbacks();

        if (sendAppOpen()) {
            track(null, "$app_open", null);
        }

        if (!mPersistentIdentity.hasTrackedIntegration()) {
            try {
                final JSONObject messageProps = new JSONObject();

                messageProps.put("mp_lib", "android");
                messageProps.put("lib", "android");
                messageProps.put("distinct_id", token);

                final AnalyticsMessages.EventDescription eventDescription =
                        new AnalyticsMessages.EventDescription(null, "Integration", messageProps, "85053bf24bba75239b16a601d9387e17");
                mMessages.eventsMessage(eventDescription);
                flush();
                mPersistentIdentity.setTrackedIntegration(true);
            } catch (JSONException e) {
            }
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
     * @param token   Your Mixpanel project token. You can get your project token on the Mixpanel web site,
     *                in the settings dialog.
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
                registerAppLinksListeners(context, instance);
                sInstanceMap.put(appContext, instance);
            }

            checkIntentForInboundAppLink(context);

            return instance;
        }
    }

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
    public void timeEvent(final String eventName) {
        final long writeTime = System.currentTimeMillis();
        synchronized (mEventTimings) {
            mEventTimings.put(eventName, writeTime);
            mPersistentIdentity.addTimeEvent(eventName, writeTime);
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
     *                   See also {@link #track(String, org.json.JSONObject)}
     */
    public void trackMap(String eventName, Map<String, Object> properties) {
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

    public void track(String eventName, JSONObject properties) {
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
    public void track(String eventId, String eventName, JSONObject properties) {
        if (SugoAPI.developmentMode) {
            Toast.makeText(this.mContext, eventName, Toast.LENGTH_SHORT).show();
        }
        final Long eventBegin;
        synchronized (mEventTimings) {
            eventBegin = mEventTimings.get(eventName);
            mEventTimings.remove(eventName);
            mPersistentIdentity.removeTimeEvent(eventName);
        }

        try {
            final JSONObject messageProps = new JSONObject();

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
            final long timeSeconds = (long) timeSecondsDouble;
            messageProps.put("time", timeSeconds);
            messageProps.put("distinct_id", getDistinctId());

            if (null != eventBegin) {
                final double eventBeginDouble = ((double) eventBegin) / 1000.0;
                final double secondsElapsed = timeSecondsDouble - eventBeginDouble;
                messageProps.put("$duration", secondsElapsed);
            }

            if (null != properties) {
                final Iterator<?> propIter = properties.keys();
                while (propIter.hasNext()) {
                    final String key = (String) propIter.next();
                    messageProps.put(key, properties.get(key));
                }
            }

            if(SugoAPI.developmentMode){
                JSONArray events = new JSONArray();
                JSONObject event = new JSONObject();
                event.put("event_id", eventId);
                event.put("event_name", eventName);
                event.put("properties", messageProps);
                events.put(event);
                mUpdatesFromMixpanel.sendTestEvent(events);
            }else {
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
     * Consider adding properties to your tracking to get the best insights and experience from Mixpanel.
     *
     * @param eventName the name of the event to send
     */
    public void track(String eventName) {
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
        mMessages.postToServer();
    }

    /**
     * Returns a json object of the user's current super properties
     * <p>
     * <p>SuperProperties are a collection of properties that will be sent with every event to Mixpanel,
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
        mPersistentIdentity.registerSuperProperties(superProperties);
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
     * <p>Future tracking calls to Mixpanel will not contain the specific
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
     * that will be sent to Mixpanel. These are not all of the default properties,
     * but are a subset that are dependant on the user's device or installed version
     * of the host application, and are guaranteed not to change while the app is running.
     */
    public Map<String, String> getDeviceInfo() {
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
     * Attempt to register MixpanelActivityLifecycleCallbacks to the application's event lifecycle.
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
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mContext.getApplicationContext() instanceof Application) {
                final Application app = (Application) mContext.getApplicationContext();
                MixpanelActivityLifecycleCallbacks mixpanelActivityLifecycleCallbacks = new MixpanelActivityLifecycleCallbacks(this, mConfig);
                app.registerActivityLifecycleCallbacks(mixpanelActivityLifecycleCallbacks);
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
        if (Build.VERSION.SDK_INT < SGConfig.UI_FEATURES_MIN_API) {
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

    private static void registerAppLinksListeners(Context context, final SugoAPI mixpanel) {
        // Register a BroadcastReceiver to receive com.parse.bolts.measurement_event and track a call to mixpanel
        try {
            final Class<?> clazz = Class.forName("android.support.v4.content.LocalBroadcastManager");
            final Method methodGetInstance = clazz.getMethod("getInstance", Context.class);
            final Method methodRegisterReceiver = clazz.getMethod("registerReceiver", BroadcastReceiver.class, IntentFilter.class);
            final Object localBroadcastManager = methodGetInstance.invoke(null, context);
            methodRegisterReceiver.invoke(localBroadcastManager, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final JSONObject properties = new JSONObject();
                    final Bundle args = intent.getBundleExtra("event_args");
                    if (args != null) {
                        for (final String key : args.keySet()) {
                            try {
                                properties.put(key, args.get(key));
                            } catch (final JSONException e) {
                                Log.e(APP_LINKS_LOGTAG, "failed to add key \"" + key + "\" to properties for tracking bolts event", e);
                            }
                        }
                    }
                    mixpanel.track(null, "$" + intent.getStringExtra("event_name"), properties);
                }
            }, new IntentFilter("com.parse.bolts.measurement_event"));
        } catch (final InvocationTargetException e) {
            Log.d(APP_LINKS_LOGTAG, "Failed to invoke LocalBroadcastManager.registerReceiver() -- App Links tracking will not be enabled due to this exception", e);
        } catch (final ClassNotFoundException e) {
            Log.d(APP_LINKS_LOGTAG, "To enable App Links tracking android.support.v4 must be installed: " + e.getMessage());
        } catch (final NoSuchMethodException e) {
            Log.d(APP_LINKS_LOGTAG, "To enable App Links tracking android.support.v4 must be installed: " + e.getMessage());
        } catch (final IllegalAccessException e) {
            Log.d(APP_LINKS_LOGTAG, "App Links tracking will not be enabled due to this exception: " + e.getMessage());
        }
    }

    private static void checkIntentForInboundAppLink(Context context) {
        // call the Bolts getTargetUrlFromInboundIntent method simply for a side effect
        // if the intent is the result of an App Link, it'll trigger al_nav_in
        // https://github.com/BoltsFramework/Bolts-Android/blob/1.1.2/Bolts/src/bolts/AppLinks.java#L86
        if (context instanceof Activity) {
            try {
                final Class<?> clazz = Class.forName("bolts.AppLinks");
                final Intent intent = ((Activity) context).getIntent();
                final Method getTargetUrlFromInboundIntent = clazz.getMethod("getTargetUrlFromInboundIntent", Context.class, Intent.class);
                getTargetUrlFromInboundIntent.invoke(null, context, intent);
            } catch (final InvocationTargetException e) {
                Log.d(APP_LINKS_LOGTAG, "Failed to invoke bolts.AppLinks.getTargetUrlFromInboundIntent() -- Unable to detect inbound App Links", e);
            } catch (final ClassNotFoundException e) {
                Log.d(APP_LINKS_LOGTAG, "Please install the Bolts library >= 1.1.2 to track App Links: " + e.getMessage());
            } catch (final NoSuchMethodException e) {
                Log.d(APP_LINKS_LOGTAG, "Please install the Bolts library >= 1.1.2 to track App Links: " + e.getMessage());
            } catch (final IllegalAccessException e) {
                Log.d(APP_LINKS_LOGTAG, "Unable to detect inbound App Links: " + e.getMessage());
            }
        } else {
            Log.d(APP_LINKS_LOGTAG, "Context is not an instance of Activity. To detect inbound App Links, pass an instance of an Activity to getInstance.");
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
        webViewClient.setmToken(token);
        webView.setWebViewClient(webViewClient);
        webView.addJavascriptInterface(new SugoWebEventListener(this), "sugoEventListener");
        webView.addJavascriptInterface(new SugoWebNodeReporter(), "sugoWebNodeReporter");
    }

    private final Context mContext;
    private final AnalyticsMessages mMessages;
    private final SGConfig mConfig;
    private final String mToken;
    private final UpdatesFromMixpanel mUpdatesFromMixpanel;
    private final PersistentIdentity mPersistentIdentity;
    private final UpdatesListener mUpdatesListener;
    private final TrackingDebug mTrackingDebug;
    private final DecideMessages mDecideMessages;
    private final Map<String, String> mDeviceInfo;
    private final Map<String, Long> mEventTimings;

    // Maps each token to a singleton SugoAPI instance
    private static final Map<Context, SugoAPI> sInstanceMap = new HashMap<Context, SugoAPI>();
    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    private static final Tweaks sSharedTweaks = new Tweaks();
    private static Future<SharedPreferences> sReferrerPrefs;

    private static final String LOGTAG = "SugoAPI.API";
    private static final String APP_LINKS_LOGTAG = "SugoAPI.AL";
    private static final String ENGAGE_DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss";

    private boolean mDisableDecideChecker;
}
