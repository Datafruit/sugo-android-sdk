package io.sugo.android.metrics;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import io.sugo.android.util.OfflineMode;


/**
 * Stores global configuration options for the Sugo library. You can enable and disable configuration
 * options using &lt;meta-data&gt; tags inside of the &lt;application&gt; tag in your AndroidManifest.xml.
 * All settings are optional, and default to reasonable recommended values. Most users will not have to
 * set any options.
 * <p>
 * Sugo understands the following options:
 * <p>
 * <dl>
 * <dt>io.sugo.android.SGConfig.EnableDebugLogging</dt>
 * <dd>A boolean value. If true, emit more detailed log messages. Defaults to false</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.BulkUploadLimit</dt>
 * <dd>An integer count of messages, the maximum number of messages to queue before an upload attempt. This value should be less than 50.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.FlushInterval</dt>
 * <dd>An integer number of milliseconds, the maximum time to wait before an upload if the bulk upload limit isn't reached.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.DebugFlushInterval</dt>
 * <dd>An integer number of milliseconds, the maximum time to wait before an upload if the bulk upload limit isn't reached in debug mode.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.DataExpiration</dt>
 * <dd>An integer number of milliseconds, the maximum age of records to send to Sugo. Corresponds to Sugo's server-side limit on record age.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.MinimumDatabaseLimit</dt>
 * <dd>An integer number of bytes. Sugo attempts to limit the size of its persistent data
 * queue based on the storage capacity of the device, but will always allow queing below this limit. Higher values
 * will take up more storage even when user storage is very full.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.DisableFallback</dt>
 * <dd>A boolean value. If true, do not send data over HTTP, even if HTTPS is unavailable. Defaults to true - by default, Sugo will only attempt to communicate over HTTPS.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.ResourcePackageName</dt>
 * <dd>A string java package name. Defaults to the package name of the Application. Users should set if the package name of their R class is different from the application package name due to application id settings.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.DisableGestureBindingUI</dt>
 * <dd>A boolean value. If true, do not allow connecting to the codeless event binding or A/B testing editor using an accelerometer gesture. Defaults to false.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.DisableEmulatorBindingUI</dt>
 * <dd>A boolean value. If true, do not attempt to connect to the codeless event binding or A/B testing editor when running in the Android emulator. Defaults to false.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.DisableAppOpenEvent</dt>
 * <dd>A boolean value. If true, do not send an "$app_open" event when the SugoAPI object is created for the first time. Defaults to true - the $app_open event will not be sent by default.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.AutoShowSugoUpdates</dt>
 * <dd>A boolean value. If true, automatically show surveys, notifications, and A/B test variants. Defaults to true.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.EventsEndpoint</dt>
 * <dd>A string URL. If present, the library will attempt to send events to this endpoint rather than to the default Sugo endpoint.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.EventsFallbackEndpoint</dt>
 * <dd>A string URL. If present, AND if DisableFallback is false, events will be sent to this endpoint if the EventsEndpoint cannot be reached.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.PeopleEndpoint</dt>
 * <dd>A string URL. If present, the library will attempt to send people updates to this endpoint rather than to the default Sugo endpoint.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.PeopleFallbackEndpoint</dt>
 * <dd>A string URL. If present, AND if DisableFallback is false, people updates will be sent to this endpoint if the EventsEndpoint cannot be reached.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.DecideEndpoint</dt>
 * <dd>A string URL. If present, the library will attempt to get survey, notification, codeless event tracking, and A/B test variant information from this url rather than the default Sugo endpoint.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.DecideFallbackEndpoint</dt>
 * <dd>A string URL. If present, AND if DisableFallback is false, the library will query this url if the DecideEndpoint url cannot be reached.</dd>
 * <p>
 * <dt>io.sugo.android.SGConfig.EditorUrl</dt>
 * <dd>A string URL. If present, the library will attempt to connect to this endpoint when in interactive editing mode, rather than to the default Sugo editor url.</dd>
 * </dl>
 *
 * @author Administrator
 */
public class SGConfig {

    private static final String LOGTAG = "SugoAPI.Conf";

    /**
     * Unfortunately, as long as we support building from source in Eclipse,
     * we can't rely on BuildConfig.SUGO_VERSION existing, so this must
     * be hard-coded both in our gradle files and here in code.
     */
    public static final String VERSION = "2.3.5";

    public static final String FIELD_APP_BUILD_NUMBER = "app_build_number";
    public static final String FIELD_APP_VERSION_STRING = "app_version";
    public static final String FIELD_BLUETOOTH_ENABLED = "has_bluetooth";
    public static final String FIELD_BLUETOOTH_VERSION = "bluetooth_version";
    public static final String FIELD_BRAND = "device_brand";
    public static final String FIELD_CARRIER = "carrier";
    public static final String FIELD_FROM_BINDING = "from_binding";
    public static final String FIELD_GOOGLE_PLAY_SERVICES = "google_play_services";
    public static final String FIELD_HAS_NFC = "has_nfc";
    public static final String FIELD_HAS_TELEPHONE = "has_telephone";
    public static final String FIELD_LIB_VERSION = "sdk_version";
    public static final String FIELD_MANUFACTURER = "manufacturer";
    public static final String FIELD_MODEL = "device_model";
    public static final String FIELD_OS = "system_name";
    public static final String FIELD_OS_VERSION = "system_version";
    public static final String FIELD_SCREEN_DPI = "screen_dpi";
    public static final String FIELD_SCREEN_HEIGHT = "screen_height";
    public static final String FIELD_SCREEN_WIDTH = "screen_width";
    public static final String FIELD_TEXT = "event_label";
    public static final String FIELD_CLIENT_NETWORK = "network";
    public static final String FIELD_WIFI = "has_wifi";
    public static final String FIELD_DISTINCT_ID = "distinct_id";
    public static final String FIELD_EVENT_ID = "event_id";
    public static final String FIELD_EVENT_NAME = "event_name";
    public static final String FIELD_MP_LIB = "sugo_lib";
    public static final String FIELD_TIME = "event_time";
    public static final String FIELD_TOKEN = "token";
    public static final String FIELD_PAGE = "path_name";
    public static final String FIELD_DURATION = "duration";
    public static final String SESSION_ID = "session_id";
    public static final String FIELD_PAGE_NAME = "page_name";
    public static final String FIELD_EVENT_TYPE = "event_type";
    public static final String FIELD_DEVICE_ID = "device_id";
    public static final String FIELD_PAGE_CATEGORY = "page_category";
    public static final String FIELD_FIRST_VISIT_TIME = "first_visit_time";
    public static final String FIELD_FIRST_LOGIN_TIME = "first_login_time";

    static final String TIME_EVENT_TAG = "sugo_time_event_tag";

    private String distinctId;
    private final String mToken;
    private final String mProjectId;
    private final String mEventsEndpoint;
    private final String mDecideEndpoint;
    private final String mHeatMapEndpoint;
    private final String mFirstLoginEndpoint;
    private final String mEditorUrl;
    private final String mResourcePackageName;
    private final boolean mEnablePageEvent;
    private final int mBulkUploadLimit;
    private final int mFlushInterval;
    private final long mUpdateDecideInterval;
    private final int mDataExpiration;
    private final int mMinimumDatabaseLimit;
    private final String mEventsFallbackEndpoint;
    private final String mPeopleEndpoint;
    private final String mPeopleFallbackEndpoint;
    private final String mDecideFallbackEndpoint;
    private final boolean mDisableFallback;
    private final boolean mTestMode;
    private final boolean mDisableGestureBindingUI;
    private final boolean mDisableEmulatorBindingUI;
    private final boolean mDisableAppOpenEvent;
    private final boolean mDisableViewCrawler;
    private final String[] mDisableViewCrawlerForProjects;
    private final boolean mAutoShowSugoUpdates;
    private final boolean mDisableDecideChecker;
    private final int mImageCacheMaxMemoryFactor;
    private String webRoot;
    // Mutable, with synchronized accessor and mutator
    private SSLSocketFactory mSSLSocketFactory;
    private OfflineMode mOfflineMode;

    private static SGConfig sInstance;
    private static final Object sInstanceLock = new Object();

    public static boolean DEBUG = false;
    private static boolean sugoEnable = true;

    /**
     * Minimum API level for support of rich UI features, like Surveys, In-App notifications, and dynamic event binding.
     * Devices running OS versions below this level will still support tracking and push notification features.
     */
    public static final int UI_FEATURES_MIN_API = 16;

    // Instances are safe to store, since they're immutable and always the same.
    public static SGConfig getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (null == sInstance) {
                final Context appContext = context.getApplicationContext();
                sInstance = readConfig(appContext);
            }
        }

        return sInstance;
    }

    /**
     * Package access for testing only- do not call directly in library code
     */
    static SGConfig readConfig(Context appContext) {
        final String packageName = appContext.getPackageName();
        try {
            final ApplicationInfo appInfo = appContext.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle configBundle = appInfo.metaData;
            if (null == configBundle) {
                configBundle = new Bundle();
            }
            return new SGConfig(configBundle, appContext);
        } catch (final NameNotFoundException e) {
            throw new RuntimeException("Can't configure Sugo with package name " + packageName, e);
        }
    }

    SGConfig(Bundle metaData, Context context) {

        // By default, we use a clean, FACTORY default SSLSocket. In general this is the right
        // thing to do, and some other third party libraries change the
        SSLSocketFactory foundSSLFactory;
        myX509TrustManager xtm = new myX509TrustManager();
        myHostnameVerifier hnv = new myHostnameVerifier();
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            X509TrustManager[] xtmArray = new X509TrustManager[]{xtm};
            sslContext.init(null, xtmArray, new java.security.SecureRandom());
            foundSSLFactory = sslContext.getSocketFactory();
        } catch (final GeneralSecurityException e) {
            Log.i("SugoAPI.Conf", "System has no SSL support. Built-in events editor will not be available", e);
            foundSSLFactory = null;
        }
        mSSLSocketFactory = foundSSLFactory;

        DEBUG = metaData.getBoolean("io.sugo.android.SGConfig.EnableDebugLogging", false);

        mTestMode = metaData.getBoolean("io.sugo.android.SGConfig.TestMode", false);

        mToken = metaData.getString("io.sugo.android.SGConfig.token");
        mProjectId = metaData.getString("io.sugo.android.SGConfig.ProjectId");
        String apiHost = metaData.getString("io.sugo.android.SGConfig.APIHost");
        String eventsHost = metaData.getString("io.sugo.android.SGConfig.EventsHost");
        String editorHost = metaData.getString("io.sugo.android.SGConfig.EditorHost");

        mDecideEndpoint = apiHost + "/api/sdk/decide";
        mHeatMapEndpoint = apiHost + "/api/sdk/heat";
        mFirstLoginEndpoint = apiHost + "/api/sdk/get-first-login-time";
        mEventsEndpoint = eventsHost + "/posts?locate=" + mProjectId;
        mEditorUrl = editorHost + "/connect/";
        mEnablePageEvent = metaData.getBoolean("io.sugo.android.SGConfig.EnablePageEvent", true);
        mFlushInterval = metaData.getInt("io.sugo.android.SGConfig.FlushInterval", 60 * 1000);
        mUpdateDecideInterval = metaData.getInt("io.sugo.android.SGConfig.UpdateDecideInterval", 60 * 60 * 1000);

        mBulkUploadLimit = metaData.getInt("io.sugo.android.SGConfig.BulkUploadLimit", 40); // 40 records default
        mDataExpiration = metaData.getInt("io.sugo.android.SGConfig.DataExpiration", 1000 * 60 * 60 * 24 * 5); // 5 days default
        mMinimumDatabaseLimit = metaData.getInt("io.sugo.android.SGConfig.MinimumDatabaseLimit", 20 * 1024 * 1024); // 20 Mb
        mDisableFallback = metaData.getBoolean("io.sugo.android.SGConfig.DisableFallback", true);
        mResourcePackageName = metaData.getString("io.sugo.android.SGConfig.ResourcePackageName"); // default is null
        mDisableGestureBindingUI = metaData.getBoolean("io.sugo.android.SGConfig.DisableGestureBindingUI", false);
        mDisableEmulatorBindingUI = metaData.getBoolean("io.sugo.android.SGConfig.DisableEmulatorBindingUI", false);
        mDisableAppOpenEvent = metaData.getBoolean("io.sugo.android.SGConfig.DisableAppOpenEvent", true);
        mDisableViewCrawler = metaData.getBoolean("io.sugo.android.SGConfig.DisableViewCrawler", false);
        mDisableDecideChecker = metaData.getBoolean("io.sugo.android.SGConfig.DisableDecideChecker", false);
        mImageCacheMaxMemoryFactor = metaData.getInt("io.sugo.android.SGConfig.ImageCacheMaxMemoryFactor", 10);
        webRoot = metaData.getString("io.sugo.android.SGConfig.webRoot");
        // Disable if EITHER of these is present and false, otherwise enable
        final boolean surveysAutoCheck = metaData.getBoolean("io.sugo.android.SGConfig.AutoCheckForSurveys", true);
        final boolean sugoUpdatesAutoShow = metaData.getBoolean("io.sugo.android.SGConfig.AutoShowSugoUpdates", true);
        mAutoShowSugoUpdates = surveysAutoCheck && sugoUpdatesAutoShow;

        String eventsFallbackEndpoint = metaData.getString("io.sugo.android.SGConfig.EventsFallbackEndpoint");
        if (null == eventsFallbackEndpoint) {
            eventsFallbackEndpoint = "http://api.sugo.net/track?ip=1";
        }
        mEventsFallbackEndpoint = eventsFallbackEndpoint;

        String peopleEndpoint = metaData.getString("io.sugo.android.SGConfig.PeopleEndpoint");
        if (null == peopleEndpoint) {
            peopleEndpoint = "https://api.sugo.net/engage";
        }
        mPeopleEndpoint = peopleEndpoint;

        String peopleFallbackEndpoint = metaData.getString("io.sugo.android.SGConfig.PeopleFallbackEndpoint");
        if (null == peopleFallbackEndpoint) {
            peopleFallbackEndpoint = "http://api.sugo.net/engage";
        }
        mPeopleFallbackEndpoint = peopleFallbackEndpoint;

        String decideFallbackEndpoint = metaData.getString("io.sugo.android.SGConfig.DecideFallbackEndpoint");
        if (null == decideFallbackEndpoint) {
            decideFallbackEndpoint = "http://decide.sugo.net/decide";
        }
        mDecideFallbackEndpoint = decideFallbackEndpoint;

        int resourceId = metaData.getInt("io.sugo.android.SGConfig.DisableViewCrawlerForProjects", -1);
        if (resourceId != -1) {
            mDisableViewCrawlerForProjects = context.getResources().getStringArray(resourceId);
        } else {
            mDisableViewCrawlerForProjects = new String[0];
        }

    }

    String getFirstLoginEndpoint() {
        return mFirstLoginEndpoint;
    }

    /**
     * The SugoAPI will use the system default SSL socket settings under ordinary circumstances.
     * That means it will ignore settings you associated with the default SSLSocketFactory in the
     * schema registry or in underlying HTTP libraries. If you'd prefer for Sugo to use your
     * own SSL settings, you'll need to call setSSLSocketFactory early in your code, like this
     * <p>
     * {@code
     * <pre>
     *     SGConfig.getInstance(context).setSSLSocketFactory(someCustomizedSocketFactory);
     * </pre>
     * }
     * <p>
     * Your settings will be globally available to all Sugo instances, and will be used for
     * all SSL connections in the library. The call is thread safe, but should be done before
     * your first call to SugoAPI.getInstance to insure that the library never uses it's
     * default.
     * <p>
     * The given socket factory may be used from multiple threads, which is safe for the system
     * SSLSocketFactory class, but if you pass a subclass you should ensure that it is thread-safe
     * before passing it to Sugo.
     *
     * @param factory an SSLSocketFactory that
     */
    public synchronized void setSSLSocketFactory(SSLSocketFactory factory) {
        mSSLSocketFactory = factory;
    }

    /**
     * {@link OfflineMode} allows Sugo to be in-sync with client offline internal logic.
     * If you want to integrate your own logic with Sugo you'll need to call
     * {@link #setOfflineMode(OfflineMode)} early in your code, like this
     * <p>
     * {@code
     * <pre>
     *     SGConfig.getInstance(context).setOfflineMode(OfflineModeImplementation);
     * </pre>
     * }
     * <p>
     * Your settings will be globally available to all Sugo instances, and will be used across
     * all the library. The call is thread safe, but should be done before
     * your first call to SugoAPI.getInstance to insure that the library never uses it's
     * default.
     * <p>
     * The given {@link OfflineMode} may be used from multiple threads, you should ensure that
     * your implementation is thread-safe before passing it to Sugo.
     *
     * @param offlineMode client offline implementation to use on Sugo
     */
    public synchronized void setOfflineMode(OfflineMode offlineMode) {
        mOfflineMode = offlineMode;
    }

    public SGConfig logConfig() {
        if (DEBUG) {
            Log.v(LOGTAG,
                    "SugoAPI (" + VERSION + ") configured with:\n" +
                            "    EnableDebugLogging " + DEBUG + "\n" +
                            "    Token " + getToken() + "\n" +
                            "    ProjectId " + getProjectId() + "\n" +
                            "    DecideEndpoint " + getDecideEndpoint() + "\n" +
                            "    EditorUrl " + getEditorUrl() + "\n" +
                            "    EventsEndpoint " + getEventsEndpoint() + "\n" +
                            "    DisableDecideChecker " + getDisableDecideChecker() + "\n" +
                            "    EnablePageEvent " + isEnablePageEvent() + "\n" +
                            "    FlushInterval " + getFlushInterval() + "\n" +
                            "    BulkUploadLimit " + getBulkUploadLimit() + "\n" +
                            "    UpdateDecideInterval " + getUpdateDecideInterval() + "\n" +
                            "    DataExpiration " + getDataExpiration() + "\n" +
                            "    ResourcePackageName " + getResourcePackageName() + "\n" +
                            "    MinimumDatabaseLimit " + getMinimumDatabaseLimit() + "\n" +
                            "    DisableFallback " + getDisableFallback() + "\n" +
                            "    DisableAppOpenEvent " + getDisableAppOpenEvent() + "\n" +
                            "    DisableViewCrawler " + getDisableViewCrawler() + "\n" +
                            "    DisableDeviceUIBinding " + getDisableGestureBindingUI() + "\n" +
                            "    DisableEmulatorUIBinding " + getDisableEmulatorBindingUI() + "\n" +
                            "    TestMode " + getTestMode() + "\n" +
                            "    PeopleEndpoint " + getPeopleEndpoint() + "\n" +
                            "    EventsFallbackEndpoint " + getEventsFallbackEndpoint() + "\n" +
                            "    PeopleFallbackEndpoint " + getPeopleFallbackEndpoint() + "\n" +
                            "    DecideFallbackEndpoint " + getDecideFallbackEndpoint() + "\n"
            );
        }
        return this;
    }

    public boolean isEnablePageEvent() {
        return mEnablePageEvent;
    }

    public long getUpdateDecideInterval() {
        return mUpdateDecideInterval;
    }

    void setDistinctId(String distinctId) {
        this.distinctId = distinctId;
    }

    String getDistinctId() {
        return distinctId;
    }

    class myX509TrustManager implements X509TrustManager {
        public myX509TrustManager() {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    class myHostnameVerifier implements HostnameVerifier {
        public myHostnameVerifier() {
        }

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    // Max size of queue before we require a flush. Must be below the limit the service will accept.
    public int getBulkUploadLimit() {
        return mBulkUploadLimit;
    }

    // Target max milliseconds between flushes. This is advisory.
    public int getFlushInterval() {
        return mFlushInterval;
    }

    // Throw away records that are older than this in milliseconds. Should be below the server side age limit for events.
    public int getDataExpiration() {
        return mDataExpiration;
    }

    public int getMinimumDatabaseLimit() {
        return mMinimumDatabaseLimit;
    }

    public boolean getDisableFallback() {
        return mDisableFallback;
    }

    public boolean getDisableGestureBindingUI() {
        return mDisableGestureBindingUI;
    }

    public boolean getDisableEmulatorBindingUI() {
        return mDisableEmulatorBindingUI;
    }

    public boolean getDisableAppOpenEvent() {
        return mDisableAppOpenEvent;
    }

    public boolean getDisableViewCrawler() {
        return mDisableViewCrawler;
    }

    public String[] getDisableViewCrawlerForProjects() {
        return mDisableViewCrawlerForProjects;
    }

    public boolean getTestMode() {
        return mTestMode;
    }

    public String getProjectId() {
        return mProjectId;
    }

    // Preferred URL for tracking events
    public String getEventsEndpoint() {
        return mEventsEndpoint;
    }

    // Preferred URL for tracking people
    public String getPeopleEndpoint() {
        return mPeopleEndpoint;
    }

    // Preferred URL for pulling decide data
    public String getDecideEndpoint() {
        return mDecideEndpoint;
    }

    public String getHeatMapEndpoint() {
        return mHeatMapEndpoint;
    }

    public String getWebRoot() {
        if (webRoot == null || webRoot.equals("")) {
            webRoot = " ";
        }
        return webRoot;
    }

    public String getToken() {
        return mToken;
    }

    // Fallback URL for tracking events if post to preferred URL fails
    public String getEventsFallbackEndpoint() {
        return mEventsFallbackEndpoint;
    }

    // Fallback URL for tracking people if post to preferred URL fails
    public String getPeopleFallbackEndpoint() {
        return mPeopleFallbackEndpoint;
    }

    // Fallback URL for pulling decide data if preferred URL fails
    public String getDecideFallbackEndpoint() {
        return mDecideFallbackEndpoint;
    }

    // Check for and show eligible surveys and in app notifications on Activity changes
    public boolean getAutoShowSugoUpdates() {
        return mAutoShowSugoUpdates;
    }

    // Preferred URL for connecting to the editor websocket
    public String getEditorUrl() {
        return mEditorUrl;
    }

    public boolean getDisableDecideChecker() {
        return mDisableDecideChecker;
    }

    // Pre-configured package name for resources, if they differ from the application package name
    //
    // mContext.getPackageName() actually returns the "application id", which
    // usually (but not always) the same as package of the generated R class.
    //
    //  See: http://tools.android.com/tech-docs/new-build-system/applicationid-vs-packagename
    //
    // As far as I can tell, the original package name is lost in the build
    // process in these cases, and must be specified by the developer using
    // SGConfig meta-data.
    public String getResourcePackageName() {
        return mResourcePackageName;
    }

    // This method is thread safe, and assumes that SSLSocketFactory is also thread safe
    // (At this writing, all HttpsURLConnections in the framework share a single factory,
    // so this is pretty safe even if the docs are ambiguous)
    public synchronized SSLSocketFactory getSSLSocketFactory() {
        return mSSLSocketFactory;
    }

    // This method is thread safe, and assumes that OfflineMode is also thread safe
    public synchronized OfflineMode getOfflineMode() {
        return mOfflineMode;
    }

    // ImageStore LRU Cache size will be availableMaxMemory() / mImageCacheMaxMemoryFactor
    public int getImageCacheMaxMemoryFactor() {
        return mImageCacheMaxMemoryFactor;
    }

    public boolean isSugoEnable() {
        return sugoEnable;
    }

    public SGConfig setSugoEnable(boolean sugoEnable) {
        SGConfig.sugoEnable = sugoEnable;
        return this;
    }
}
