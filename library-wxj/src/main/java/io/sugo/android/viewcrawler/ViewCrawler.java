package io.sugo.android.viewcrawler;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.JsonWriter;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLSocketFactory;

import io.sugo.android.mpmetrics.OnMixpanelTweaksUpdatedListener;
import io.sugo.android.mpmetrics.ResourceIds;
import io.sugo.android.mpmetrics.ResourceReader;
import io.sugo.android.mpmetrics.SGConfig;
import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.android.mpmetrics.SugoDimensionManager;
import io.sugo.android.mpmetrics.SugoPageManager;
import io.sugo.android.mpmetrics.SugoWebEventListener;
import io.sugo.android.mpmetrics.SuperPropertyUpdate;
import io.sugo.android.mpmetrics.SystemInformation;
import io.sugo.android.mpmetrics.Tweaks;
import io.sugo.android.util.HttpService;
import io.sugo.android.util.ImageStore;
import io.sugo.android.util.JSONUtils;
import io.sugo.android.util.RemoteService;

/**
 * This class is for internal use by the Mixpanel API, and should
 * not be called directly by your code.
 */
@TargetApi(SGConfig.UI_FEATURES_MIN_API)
public class ViewCrawler implements UpdatesFromMixpanel, TrackingDebug, ViewVisitor.OnLayoutErrorListener {



    public ViewCrawler(Context context, String token, SugoAPI mixpanel, Tweaks tweaks) {
        mConfig = SGConfig.getInstance(context);

        Context appContext = context.getApplicationContext();
        mContext = appContext;

        mEditState = new EditState();
        mTweaks = tweaks;
        mDeviceInfo = mixpanel.getDeviceInfo();
        mScaledDensity = Resources.getSystem().getDisplayMetrics().scaledDensity;
        mTweaksUpdatedListeners = new ArrayList<>();

        final Application app = (Application) context.getApplicationContext();
        app.registerActivityLifecycleCallbacks(new LifecycleCallbacks());

        final HandlerThread thread = new HandlerThread(ViewCrawler.class.getCanonicalName());
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mMessageThreadHandler = new ViewCrawlerHandler(appContext, token, thread.getLooper(), this);

        mDynamicEventTracker = new DynamicEventTracker(mixpanel, mMessageThreadHandler);
        mMixpanel = mixpanel;
        mTweaks.addOnTweakDeclaredListener(new Tweaks.OnTweakDeclaredListener() {
            @Override
            public void onTweakDeclared() {
                final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_SEND_DEVICE_INFO);
                mMessageThreadHandler.sendMessage(msg);
            }
        });

    }

    @Override
    public void startUpdates() {
        mMessageThreadHandler.start();
        mMessageThreadHandler.sendMessage(mMessageThreadHandler.obtainMessage(MESSAGE_INITIALIZE_CHANGES));
    }

    @Override
    public void sendTestEvent(JSONArray eventPackage) {
        final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_SEND_TEST_EVENT);
        msg.obj = eventPackage;
        mMessageThreadHandler.sendMessage(msg);
    }

    @Override
    public Tweaks getTweaks() {
        return mTweaks;
    }

    @Override
    public void setEventBindings(JSONArray bindings) {
        final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_EVENT_BINDINGS_RECEIVED);
        msg.obj = bindings;
        mMessageThreadHandler.sendMessage(msg);
    }

    @Override
    public void setH5EventBindings(JSONArray bindings) {
        final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_H5_EVENT_BINDINGS_RECEIVED);
        msg.obj = bindings;
        mMessageThreadHandler.sendMessage(msg);
    }

    @Override
    public void setPageInfos(JSONArray pageInfos) {
        final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_HANDLE_PAGE_INFO_EVENT);
        msg.obj = pageInfos;
        mMessageThreadHandler.sendMessage(msg);
    }

    @Override
    public void setDimensions(JSONArray dimensions) {
        final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_HANDLE_DIMENSIONS_EVENT);
        msg.obj = dimensions;
        mMessageThreadHandler.sendMessage(msg);
    }

    @Override
    public void setVariants(JSONArray variants) {
        final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_VARIANTS_RECEIVED);
        msg.obj = variants;
        mMessageThreadHandler.sendMessage(msg);
    }

    @Override
    public void addOnMixpanelTweaksUpdatedListener(OnMixpanelTweaksUpdatedListener listener) {
        if (null == listener) {
            throw new NullPointerException("Listener cannot be null");
        }

        mTweaksUpdatedListeners.add(listener);
    }

    @Override
    public void removeOnMixpanelTweaksUpdatedListener(OnMixpanelTweaksUpdatedListener listener) {
        mTweaksUpdatedListeners.remove(listener);
    }

    @Override
    public void reportTrack(String eventName) {
        final Message m = mMessageThreadHandler.obtainMessage();
        m.what = MESSAGE_SEND_EVENT_TRACKED;
        m.obj = eventName;

        mMessageThreadHandler.sendMessage(m);
    }

    @Override
    public void onLayoutError(ViewVisitor.LayoutErrorMessage e) {
        final Message m = mMessageThreadHandler.obtainMessage();
        m.what = MESSAGE_SEND_LAYOUT_ERROR;
        m.obj = e;
        mMessageThreadHandler.sendMessage(m);
    }

    public boolean sendConnectEditor(Uri uri) {
        if (mMessageThreadHandler != null) {
            if (uri != null) {
                String host = uri.getHost();
                if (host == null) {
                    return false;
                }
                int msgWhat = -1;
                // 解析浏览器扫描二维码跳转应用的uri
                if (host.equals("sugo")) {
                    secretKey = uri.getQueryParameter("sKey");
                    scanUrlType = uri.getQueryParameter("type");
                    if (scanUrlType != null && scanUrlType.equals("heatmap")) {
                        msgWhat = MESSAGE_GET_HEAT_MAP_DATA;
                    } else {
                        msgWhat = MESSAGE_CONNECT_TO_EDITOR;
                    }
                    final Message message = mMessageThreadHandler.obtainMessage(msgWhat);
                    mMessageThreadHandler.sendMessage(message);
                    return true;
                } else {    // 解析应用内部扫描二维码获取的链接
                    try {
                        String token = uri.getQueryParameter("token");
                        if (token != null && token.equals(SGConfig.getInstance(mContext).getToken())) {
                            secretKey = uri.getQueryParameter("sKey");
                            if (uri.getPath().equals("/heat")) {
                                msgWhat = MESSAGE_GET_HEAT_MAP_DATA;
                            } else {
                                msgWhat = MESSAGE_CONNECT_TO_EDITOR;
                            }
                            final Message message = mMessageThreadHandler.obtainMessage(msgWhat);
                            mMessageThreadHandler.sendMessage(message);
                            return true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    private class EmulatorConnector implements Runnable {
        public EmulatorConnector() {
            mStopped = true;
        }

        @Override
        public void run() {
            if (!mStopped) {
                final Message message = mMessageThreadHandler.obtainMessage(MESSAGE_CONNECT_TO_EDITOR);
                mMessageThreadHandler.sendMessage(message);
            }

            mMessageThreadHandler.postDelayed(this, EMULATOR_CONNECT_ATTEMPT_INTERVAL_MILLIS);
        }

        public void start() {
            mStopped = false;
            mMessageThreadHandler.post(this);
        }

        public void stop() {
            mStopped = true;
            mMessageThreadHandler.removeCallbacks(this);
        }

        private volatile boolean mStopped;
    }

    private class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks, FlipGesture.OnFlipGestureListener {

        public LifecycleCallbacks() {
            mFlipGesture = new FlipGesture(this);
            mEmulatorConnector = new EmulatorConnector();
        }

        @Override
        public void onFlipGesture() {
            //mMixpanel.track("$ab_gesture3");
            secretKey = "onFlipGesture";
            final Message message = mMessageThreadHandler.obtainMessage(MESSAGE_CONNECT_TO_EDITOR);
            mMessageThreadHandler.sendMessage(message);
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (SGConfig.positionConfig > 0){
                long now = System.currentTimeMillis();
                if (now - SGConfig.lastReportLoaction > SGConfig.positionConfig * 60000) {
                    try {
                        JSONObject messageProps = new JSONObject();
                        double[] loc = mMixpanel.getLngAndLat(mContext);
                        messageProps.put(SGConfig.FIELD_LONGITUDE, loc[0]);
                        messageProps.put(SGConfig.FIELD_LATITUDE, loc[1]);
                        messageProps.put(SGConfig.FIELD_EVENT_TYPE, "位置");
                        messageProps.put(SGConfig.FIELD_PAGE_NAME, "位置信息收集");
                        mMixpanel.track("位置信息收集", messageProps);
                        mMixpanel.flush();
                        SGConfig.lastReportLoaction = now;
                        SharedPreferences preferences = mContext.getSharedPreferences(ViewCrawler.SHARED_PREF_EDITS_FILE + mMixpanel.getConfig().getToken(), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putLong(ViewCrawler.LAST_REPORT_LOCATION, now);
                        editor.apply();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            installConnectionSensor(activity);
            mEditState.add(activity);
            Uri data = activity.getIntent().getData();
            if (data != null) {
                if (sendConnectEditor(data)) {
                    activity.getIntent().setData(null);     // 防止未再次扫码却自动连接的情况
                }
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
            mEditState.remove(activity);
            uninstallConnectionSensor(activity);
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            SugoWebEventListener.cleanUnuseWebView(activity);
        }

        private void installConnectionSensor(final Activity activity) {
            if (isInEmulator() && !mConfig.getDisableEmulatorBindingUI()) {
                mEmulatorConnector.start();
            } else if (!mConfig.getDisableGestureBindingUI()) {
                final SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
                final Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sensorManager.registerListener(mFlipGesture, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        private void uninstallConnectionSensor(final Activity activity) {
            if (isInEmulator() && !mConfig.getDisableEmulatorBindingUI()) {
                mEmulatorConnector.stop();
            } else if (!mConfig.getDisableGestureBindingUI()) {
                final SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
                sensorManager.unregisterListener(mFlipGesture);
            }
        }

        private boolean isInEmulator() {
            if (!Build.HARDWARE.equals("goldfish") && !Build.HARDWARE.equals("ranchu")) {
                return false;
            }

            if (!Build.BRAND.startsWith("generic") && !Build.BRAND.equals("Android")) {
                return false;
            }

            if (!Build.DEVICE.startsWith("generic")) {
                return false;
            }

            if (!Build.PRODUCT.contains("sdk")) {
                return false;
            }

            if (!Build.MODEL.toLowerCase(Locale.US).contains("sdk")) {
                return false;
            }

            return true;
        }

        private final FlipGesture mFlipGesture;
        private final EmulatorConnector mEmulatorConnector;
    }

    private class ViewCrawlerHandler extends Handler {

        public ViewCrawlerHandler(Context context, String token, Looper looper, ViewVisitor.OnLayoutErrorListener layoutErrorListener) {
            super(looper);
            mToken = token;
            mSnapshot = null;

            String resourcePackage = mConfig.getResourcePackageName();
            if (null == resourcePackage) {
                resourcePackage = context.getPackageName();
            }

            final ResourceIds resourceIds = new ResourceReader.Ids(resourcePackage, context);

            mImageStore = new ImageStore(context, "ViewCrawler");
            mProtocol = new EditProtocol(resourceIds, mImageStore, layoutErrorListener);
            mEditorChanges = new HashMap<String, Pair<String, JSONObject>>();
            mEditorTweaks = new ArrayList<JSONObject>();
            mEditorAssetUrls = new ArrayList<String>();
            mEditorEventBindings = new ArrayList<Pair<String, JSONObject>>();
            mPersistentChanges = new ArrayList<VariantChange>();
            mPersistentTweaks = new ArrayList<VariantTweak>();
            mPersistentEventBindings = new ArrayList<Pair<String, JSONObject>>();
            mSeenExperiments = new HashSet<Pair<Integer, Integer>>();
            mStartLock = new ReentrantLock();
            mStartLock.lock();
        }

        public void start() {
            mStartLock.unlock();
        }

        @Override
        public void handleMessage(Message msg) {
            mStartLock.lock();
            try {

                final int what = msg.what;
                switch (what) {
                    case MESSAGE_INITIALIZE_CHANGES:
                        loadKnownChanges();
                        initializeChanges();
                        break;
                    case MESSAGE_GET_HEAT_MAP_DATA:
                        // 热图模式下，无需再次热图
                        // 可视化埋点模式下，不可进入热图
                        if (!SugoHeatMap.isShowHeatMap() && (!SugoAPI.developmentMode)) {
                            String heatmapData = getHeatMapData();
                            handleHeatMapData(heatmapData);
                        }
                        break;
                    case MESSAGE_CONNECT_TO_EDITOR:
                        // 热图模式下无法埋点
                        if (!SugoHeatMap.isShowHeatMap()) {
                            connectToEditor();
                        }
                        break;
                    case MESSAGE_SEND_DEVICE_INFO:
                        sendDeviceInfo();
                        break;
                    case MESSAGE_SEND_STATE_FOR_EDITING:
                        sendSnapshot((JSONObject) msg.obj);
                        break;
                    case MESSAGE_SEND_EVENT_TRACKED:
                        sendReportTrackToEditor((String) msg.obj);
                        break;
                    case MESSAGE_SEND_LAYOUT_ERROR:
                        sendLayoutError((ViewVisitor.LayoutErrorMessage) msg.obj);
                        break;
                    case MESSAGE_VARIANTS_RECEIVED:
                        handleVariantsReceived((JSONArray) msg.obj);
                        break;
                    case MESSAGE_HANDLE_EDITOR_CHANGES_RECEIVED:
                        handleEditorChangeReceived((JSONObject) msg.obj);
                        break;
                    case MESSAGE_EVENT_BINDINGS_RECEIVED:
                        handleEventBindingsReceived((JSONArray) msg.obj);
                        break;
                    case MESSAGE_H5_EVENT_BINDINGS_RECEIVED:
                        handleH5EventBindingsReceived((JSONArray) msg.obj);
                        break;
                    case MESSAGE_HANDLE_PAGE_INFO_EVENT:
                        handlePageInfoReceived((JSONArray) msg.obj);
                        break;
                    case MESSAGE_HANDLE_DIMENSIONS_EVENT:
                        handleDimensions((JSONArray) msg.obj);
                        break;
                    case MESSAGE_SEND_TEST_EVENT:
                        handleSendTestEvent((JSONArray) msg.obj);
                        break;
                    case MESSAGE_HANDLE_EDITOR_BINDINGS_RECEIVED:
                        handleEditorBindingsReceived((JSONObject) msg.obj);
                        break;
                    case MESSAGE_HANDLE_EDITOR_CHANGES_CLEARED:
                        handleEditorBindingsCleared((JSONObject) msg.obj);
                        break;
                    case MESSAGE_HANDLE_EDITOR_TWEAKS_RECEIVED:
                        handleEditorTweaksReceived((JSONObject) msg.obj);
                        break;
                    case MESSAGE_HANDLE_EDITOR_CLOSED:
                        handleEditorClosed();
                        break;
                }
            } finally {
                mStartLock.unlock();
            }
        }

        /**
         * Load the experiment ids and variants already in persistent storage into
         * into our set of seen experiments, so we don't double track them.
         */
        private void loadKnownChanges() {
            final SharedPreferences preferences = getSharedPreferences();
            final String storedChanges = preferences.getString(SHARED_PREF_CHANGES_KEY, null);

            if (null != storedChanges) {
                try {
                    final JSONArray variants = new JSONArray(storedChanges);
                    final int variantsLength = variants.length();
                    for (int i = 0; i < variantsLength; i++) {
                        final JSONObject variant = variants.getJSONObject(i);
                        final int variantId = variant.getInt("id");
                        final int experimentId = variant.getInt("experiment_id");
                        final Pair<Integer, Integer> sight = new Pair<Integer, Integer>(experimentId, variantId);
                        mSeenExperiments.add(sight);
                    }
                } catch (JSONException e) {
                    Log.e(LOGTAG, "Malformed variants found in persistent storage, clearing all variants", e);
                    final SharedPreferences.Editor editor = preferences.edit();
                    editor.remove(SHARED_PREF_CHANGES_KEY);
                    editor.remove(SHARED_PREF_BINDINGS_KEY);
                    editor.remove(SHARED_PREF_H5_BINDINGS_KEY);
                    editor.remove(SP_EVENT_BINDING_VERSION);
                    editor.apply();
                }
            }

        }

        /**
         * Load stored changes from persistent storage and apply them to the application.
         */
        private void initializeChanges() {
            final SharedPreferences preferences = getSharedPreferences();
            final String storedChanges = preferences.getString(SHARED_PREF_CHANGES_KEY, null);
            final String storedBindings = preferences.getString(SHARED_PREF_BINDINGS_KEY, null);
            List<Pair<Integer, Integer>> emptyVariantIds = new ArrayList<>();

            try {
                mPersistentChanges.clear();
                mPersistentTweaks.clear();

                if (null != storedChanges) {
                    final JSONArray variants = new JSONArray(storedChanges);
                    final int variantsLength = variants.length();
                    for (int variantIx = 0; variantIx < variantsLength; variantIx++) {
                        final JSONObject nextVariant = variants.getJSONObject(variantIx);
                        final int variantIdPart = nextVariant.getInt("id");
                        final int experimentIdPart = nextVariant.getInt("experiment_id");
                        final Pair<Integer, Integer> variantId = new Pair<Integer, Integer>(experimentIdPart, variantIdPart);

                        final JSONArray actions = nextVariant.getJSONArray("actions");
                        final int actionsLength = actions.length();
                        for (int i = 0; i < actionsLength; i++) {
                            final JSONObject change = actions.getJSONObject(i);
                            final String targetActivity = JSONUtils.optionalStringKey(change, "target_activity");
                            final VariantChange variantChange = new VariantChange(targetActivity, change, variantId);
                            mPersistentChanges.add(variantChange);
                        }

                        final JSONArray tweaks = nextVariant.getJSONArray("tweaks");
                        final int tweaksLength = tweaks.length();
                        for (int i = 0; i < tweaksLength; i++) {
                            final JSONObject tweakDesc = tweaks.getJSONObject(i);
                            final VariantTweak variantTweak = new VariantTweak(tweakDesc, variantId);
                            mPersistentTweaks.add(variantTweak);
                        }

                        if (actionsLength == 0 && tweaksLength == 0) {
                            final Pair<Integer, Integer> emptyVariantId = new Pair<Integer, Integer>(experimentIdPart, variantIdPart);
                            emptyVariantIds.add(emptyVariantId);
                        }
                    }
                }

                if (null != storedBindings) {
                    final JSONArray bindings = new JSONArray(storedBindings);

                    mPersistentEventBindings.clear();
                    for (int i = 0; i < bindings.length(); i++) {
                        final JSONObject event = bindings.getJSONObject(i);
                        final String targetActivity = JSONUtils.optionalStringKey(event, "target_activity");
                        mPersistentEventBindings.add(new Pair<String, JSONObject>(targetActivity, event));
                    }
                }
            } catch (final JSONException e) {
                Log.i(LOGTAG, "JSON error when initializing saved changes, clearing persistent memory", e);
                final SharedPreferences.Editor editor = preferences.edit();
                editor.remove(SHARED_PREF_CHANGES_KEY);
                editor.remove(SHARED_PREF_BINDINGS_KEY);
                editor.remove(SHARED_PREF_H5_BINDINGS_KEY);
                editor.remove(SP_EVENT_BINDING_VERSION);
                editor.apply();
            }

            applyVariantsAndEventBindings(emptyVariantIds);
        }


        private String getHeatMapData() {
            HttpService httpService = new HttpService();

            SystemInformation mSystemInformation = new SystemInformation(mContext);
            String unescapedDistinctId = SugoAPI.getInstance(mContext).getDistinctId();
            final String escapedToken;
            final String escapedId;
            try {
                escapedToken = URLEncoder.encode(mToken, "utf-8");
                if (null != unescapedDistinctId) {
                    escapedId = URLEncoder.encode(unescapedDistinctId, "utf-8");
                } else {
                    escapedId = null;
                }
            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException("Sugo library requires utf-8 string encoding to be available", e);
            }

            final StringBuilder queryBuilder = new StringBuilder()
                    .append("?version=1&lib=android&token=")
                    .append(escapedToken)
                    .append("&projectId=").append(SGConfig.getInstance(mContext).getProjectId());

            SharedPreferences preferences = mContext.getSharedPreferences(ViewCrawler.SHARED_PREF_EDITS_FILE + mToken, Context.MODE_PRIVATE);
            int oldEventBindingVersion = preferences.getInt(ViewCrawler.SP_EVENT_BINDING_VERSION, -1);
            queryBuilder.append("&event_bindings_version=").append(oldEventBindingVersion);

            if (null != escapedId) {
                queryBuilder.append("&distinct_id=").append(escapedId);
            }

            JSONObject properties = new JSONObject();
            try {
                String appVersion = URLEncoder.encode(mSystemInformation.getAppVersionName(), "utf-8");
                queryBuilder.append("&app_version=").append(appVersion);

                properties.putOpt("$android_lib_version", SGConfig.VERSION);
                properties.putOpt("$android_app_version", mSystemInformation.getAppVersionName());
                properties.putOpt("$android_version", Build.VERSION.RELEASE);
                properties.putOpt("$android_app_release", mSystemInformation.getAppVersionCode());
                properties.putOpt("$android_device_model", Build.MODEL);

                queryBuilder.append("&properties=");
                queryBuilder.append(URLEncoder.encode(properties.toString(), "utf-8"));
            } catch (Exception e) {
                Log.e(LOGTAG, "Exception constructing properties JSON", e.getCause());
            }

            final String checkQuery = queryBuilder.toString();
            final String[] urls;
            urls = new String[]{mConfig.getHeatMapEndpoint() + checkQuery};

            if (SGConfig.DEBUG) {
                Log.v(LOGTAG, "Querying heat map server, urls:");
                for (int i = 0; i < urls.length; i++) {
                    Log.v(LOGTAG, "    >> " + urls[i]);
                }
            }

            try {
                final byte[] response = getUrls(httpService, mContext, urls);
                if (null == response) {
                    return null;
                }
                return new String(response, "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException("UTF not supported on this platform?", e);
            } catch (RemoteService.ServiceUnavailableException e) {
                e.printStackTrace();
            }
            return null;
        }

        private byte[] getUrls(RemoteService poster, Context context, String[] urls)
                throws RemoteService.ServiceUnavailableException {
            final SGConfig config = SGConfig.getInstance(context);

            if (!poster.isOnline(context, config.getOfflineMode())) {
                return null;
            }

            byte[] response = null;
            for (String url : urls) {
                try {
                    final SSLSocketFactory socketFactory = config.getSSLSocketFactory();
                    response = poster.performRequest(url, null, socketFactory);
                    break;
                } catch (final MalformedURLException e) {
                    Log.e(LOGTAG, "Cannot interpret " + url + " as a URL.", e);
                } catch (final FileNotFoundException e) {
                    if (SGConfig.DEBUG) {
                        Log.v(LOGTAG, "Cannot get " + url + ", file not found.", e);
                    }
                } catch (final IOException e) {
                    if (SGConfig.DEBUG) {
                        Log.v(LOGTAG, "Cannot get " + url + ".", e);
                    }
                } catch (final OutOfMemoryError e) {
                    Log.e(LOGTAG, "Out of memory when getting to " + url + ".", e);
                    break;
                }
            }

            return response;
        }


        /**
         * Try to connect to the remote interactive editor, if a connection does not already exist.
         */
        private void connectToEditor() {
            if (SGConfig.DEBUG) {
                Log.v(LOGTAG, "connecting to editor");
            }

            if (mEditorConnection != null && mEditorConnection.isValid()) {
                if (SGConfig.DEBUG) {
                    Log.v(LOGTAG, "There is already a valid connection to an events editor.");
                }
                return;
            }

            final SSLSocketFactory socketFactory = mConfig.getSSLSocketFactory();
            if (null == socketFactory) {
                if (SGConfig.DEBUG) {
                    Log.v(LOGTAG, "SSL is not available on this device, no connection will be attempted to the events editor.");
                }
                return;
            }

            final String url = SGConfig.getInstance(mContext).getEditorUrl() + mToken;
            try {
                Socket socket;
                if (url.startsWith("wss://"))
                    socket = socketFactory.createSocket();
                else
                    socket = new Socket();
                mEditorConnection = new EditorConnection(new URI(url), new Editor(), socket);
            } catch (final URISyntaxException e) {
                Log.e(LOGTAG, "Error parsing URI " + url + " for editor websocket", e);
            } catch (final EditorConnection.EditorConnectionException e) {
                Log.e(LOGTAG, "Error connecting to URI " + url, e);
            } catch (IOException e) {
                Log.i(LOGTAG, "Can't create SSL Socket to connect to editor service", e);
            }
        }

        /**
         * Send a string error message to the connected web UI.
         */
        private void sendError(String errorMessage) {
            if (mEditorConnection == null) {
                return;
            }

            final JSONObject errorObject = new JSONObject();
            try {
                errorObject.put("error_message", errorMessage);
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Apparently impossible JSONException", e);
            }

            final OutputStreamWriter writer = new OutputStreamWriter(mEditorConnection.getBufferedOutputStream());
            try {
                writer.write("{\"type\": \"error\", ");
                writer.write("\"payload\": ");
                writer.write(errorObject.toString());
                writer.write("}");
            } catch (final IOException e) {
                Log.e(LOGTAG, "Can't write error message to editor", e);
            } finally {
                try {
                    writer.close();
                } catch (final IOException e) {
                    Log.e(LOGTAG, "Could not close output writer to editor", e);
                }
            }
        }

        /**
         * Report on device info to the connected web UI.
         */
        private void sendDeviceInfo() {
            if (mEditorConnection == null) {
                return;
            }

            final OutputStream out = mEditorConnection.getBufferedOutputStream();
            final JsonWriter j = new JsonWriter(new OutputStreamWriter(out));
            try {
                j.beginObject();
                j.name("type").value("device_info_response");
                j.name("payload").beginObject();
                if (secretKey != null) {
                    j.name("secret_key").value(secretKey);
                }
                j.name("device_type").value("Android");
                j.name("device_name").value(Build.BRAND + "/" + Build.MODEL);
                j.name("scaled_density").value(mScaledDensity);
                for (final Map.Entry<String, String> entry : mDeviceInfo.entrySet()) {
                    j.name(entry.getKey()).value(entry.getValue());
                }

                final Map<String, Tweaks.TweakValue> tweakDescs = mTweaks.getAllValues();
                j.name("tweaks").beginArray();
                for (Map.Entry<String, Tweaks.TweakValue> tweak : tweakDescs.entrySet()) {
                    final Tweaks.TweakValue desc = tweak.getValue();
                    final String tweakName = tweak.getKey();
                    j.beginObject();
                    j.name("name").value(tweakName);
                    j.name("minimum").value((Number) null);
                    j.name("maximum").value((Number) null);
                    switch (desc.type) {
                        case Tweaks.BOOLEAN_TYPE:
                            j.name("type").value("boolean");
                            j.name("value").value(desc.getBooleanValue());
                            break;
                        case Tweaks.DOUBLE_TYPE:
                            j.name("type").value("number");
                            j.name("encoding").value("d");
                            j.name("value").value(desc.getNumberValue().doubleValue());
                            break;
                        case Tweaks.LONG_TYPE:
                            j.name("type").value("number");
                            j.name("encoding").value("l");
                            j.name("value").value(desc.getNumberValue().longValue());
                            break;
                        case Tweaks.STRING_TYPE:
                            j.name("type").value("string");
                            j.name("value").value(desc.getStringValue());
                            break;
                        default:
                            Log.wtf(LOGTAG, "Unrecognized Tweak Type " + desc.type + " encountered.");
                    }
                    j.endObject();
                }
                j.endArray();
                j.endObject(); // payload
                j.endObject();
            } catch (final IOException e) {
                Log.e(LOGTAG, "Can't write device_info to server", e);
            } finally {
                try {
                    j.close();
                } catch (final IOException e) {
                    Log.e(LOGTAG, "Can't close websocket writer", e);
                }
            }
        }

        /**
         * Send a snapshot response, with crawled views and screenshot image, to the connected web UI.
         */
        private void sendSnapshot(JSONObject message) {
            final long startSnapshot = System.currentTimeMillis();
            String bitmapHash = null;
            try {
                final JSONObject payload = message.getJSONObject("payload");
                if (payload.has("config")) {
                    mSnapshot = mProtocol.readSnapshotConfig(payload);
                    if (SGConfig.DEBUG) {
                        Log.v(LOGTAG, "Initializing snapshot with configuration");
                    }
                }
                if (payload.has("image_hash")) {
                    bitmapHash = payload.getString("image_hash");
                }
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Payload with snapshot config required with snapshot request", e);
                sendError("Payload with snapshot config required with snapshot request");
                return;
            } catch (final EditProtocol.BadInstructionsException e) {
                Log.e(LOGTAG, "Editor sent malformed message with snapshot request", e);
                sendError(e.getMessage());
                return;
            }

            if (null == mSnapshot) {
                sendError("No snapshot configuration (or a malformed snapshot configuration) was sent.");
                Log.w(LOGTAG, "Mixpanel editor is misconfigured, sent a snapshot request without a valid configuration.");
                return;
            }
            // ELSE config is valid:

            final OutputStream out = mEditorConnection.getBufferedOutputStream();
            final OutputStreamWriter writer = new OutputStreamWriter(out);

            try {
                writer.write("{");
                writer.write("\"type\": \"snapshot_response\",");
                writer.write("\"payload\": {");
                {
                    writer.write("\"activities\":");
                    writer.flush();
                    mSnapshot.snapshots(mEditState, out, bitmapHash);
                }

                final long snapshotTime = System.currentTimeMillis() - startSnapshot;

                writer.write(",\"snapshot_time_millis\": ");
                writer.write(Long.toString(snapshotTime));

                writer.write(",\"classAttr\":{");
                Map<String,String> classMap = SugoAPI.getInstance(mContext).getClassAttributeDict();
                boolean isFirstMap = true;
                for (String key : classMap.keySet()) {
                    if (isFirstMap){
                        writer.write("\"");
                        isFirstMap = false;
                    }else {
                        writer.write(",\"");
                    }
                    writer.write(key);
                    writer.write("\":");
                    writer.write("\"");
                    writer.write(classMap.get(key));
                    writer.write("\"");
                    writer.flush();
                }
                writer.write("}"); // } classAttr

                writer.write("}"); // } payload
                writer.write("}"); // } whole message

            } catch (final IOException e) {
                Log.e(LOGTAG, "Can't write snapshot request to server", e);
            } finally {
                try {
                    writer.close();
                } catch (final IOException e) {
                    Log.e(LOGTAG, "Can't close writer.", e);
                }
            }

            SugoAPI.developmentMode = true;
        }

        /**
         * Report that a track has occurred to the connected web UI.
         */
        private void sendReportTrackToEditor(String eventName) {
            if (mEditorConnection == null) {
                return;
            }

            final OutputStream out = mEditorConnection.getBufferedOutputStream();
            final OutputStreamWriter writer = new OutputStreamWriter(out);
            final JsonWriter j = new JsonWriter(writer);

            try {
                j.beginObject();
                j.name("type").value("track_message");
                j.name("payload");
                {
                    j.beginObject();
                    j.name(SGConfig.FIELD_EVENT_NAME).value(eventName);
                    j.endObject();
                }
                j.endObject();
                j.flush();
            } catch (final IOException e) {
                Log.e(LOGTAG, "Can't write track_message to server", e);
            } finally {
                try {
                    j.close();
                } catch (final IOException e) {
                    Log.e(LOGTAG, "Can't close writer.", e);
                }
            }
        }

        private void sendLayoutError(ViewVisitor.LayoutErrorMessage exception) {
            if (mEditorConnection == null) {
                return;
            }

            final OutputStream out = mEditorConnection.getBufferedOutputStream();
            final OutputStreamWriter writer = new OutputStreamWriter(out);
            final JsonWriter j = new JsonWriter(writer);

            try {
                j.beginObject();
                j.name("type").value("layout_error");
                j.name("exception_type").value(exception.getErrorType());
                j.name("cid").value(exception.getName());
                j.endObject();
            } catch (final IOException e) {
                Log.e(LOGTAG, "Can't write track_message to server", e);
            } finally {
                try {
                    j.close();
                } catch (final IOException e) {
                    Log.e(LOGTAG, "Can't close writer.", e);
                }
            }
        }

        /**
         * Accept and apply a change from the connected UI.
         */
        private void handleEditorChangeReceived(JSONObject changeMessage) {
            try {
                final JSONObject payload = changeMessage.getJSONObject("payload");
                final JSONArray actions = payload.getJSONArray("actions");

                for (int i = 0; i < actions.length(); i++) {
                    final JSONObject change = actions.getJSONObject(i);
                    final String targetActivity = JSONUtils.optionalStringKey(change, "target_activity");
                    final String name = change.getString("name");
                    mEditorChanges.put(name, new Pair<String, JSONObject>(targetActivity, change));
                }

                applyVariantsAndEventBindings(Collections.<Pair<Integer, Integer>>emptyList());
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Bad change request received", e);
            }
        }

        /**
         * Remove a change from the connected UI.
         */
        private void handleEditorBindingsCleared(JSONObject clearMessage) {
            try {
                final JSONObject payload = clearMessage.getJSONObject("payload");
                final JSONArray actions = payload.getJSONArray("actions");

                // Don't throw any JSONExceptions after this, or you'll leak the item
                for (int i = 0; i < actions.length(); i++) {
                    final String changeId = actions.getString(i);
                    mEditorChanges.remove(changeId);
                }
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Bad clear request received", e);
            }

            applyVariantsAndEventBindings(Collections.<Pair<Integer, Integer>>emptyList());
        }

        private void handleEditorTweaksReceived(JSONObject tweaksMessage) {
            try {
                mEditorTweaks.clear();
                final JSONObject payload = tweaksMessage.getJSONObject("payload");
                final JSONArray tweaks = payload.getJSONArray("tweaks");
                final int length = tweaks.length();
                for (int i = 0; i < length; i++) {
                    final JSONObject tweakDesc = tweaks.getJSONObject(i);
                    mEditorTweaks.add(tweakDesc);
                }
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Bad tweaks received", e);
            }

            applyVariantsAndEventBindings(Collections.<Pair<Integer, Integer>>emptyList());
        }

        /**
         * Accept and apply variant changes from a non-interactive source.
         */
        private void handleVariantsReceived(JSONArray variants) {
            final SharedPreferences preferences = getSharedPreferences();
            final SharedPreferences.Editor editor = preferences.edit();
            if (variants.length() > 0) {
                editor.putString(SHARED_PREF_CHANGES_KEY, variants.toString());
            } else {
                editor.remove(SHARED_PREF_CHANGES_KEY);
            }
            editor.apply();

            initializeChanges();
        }

        private void handleHeatMapData(String heatmapData) {
            SugoHeatMap.setHeatMapData(mContext, heatmapData);
            initializeChanges();
            SugoWebEventListener.updateWebViewInject();
            SugoWebEventListener.updateXWalkViewInject();
        }

        /**
         * Accept and apply a persistent event binding from a non-interactive source.
         */
        private void handleEventBindingsReceived(JSONArray eventBindings) {
            final SharedPreferences preferences = getSharedPreferences();
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(SHARED_PREF_BINDINGS_KEY, eventBindings.toString());
            editor.apply();
            initializeChanges();
        }

        /**
         * Accept and apply a persistent h5 event binding from a non-interactive source.
         */
        private void handleH5EventBindingsReceived(JSONArray eventBindings) {
            if (!SugoAPI.developmentMode) {
                final SharedPreferences preferences = getSharedPreferences();
                final SharedPreferences.Editor editor = preferences.edit();
                editor.putString(SHARED_PREF_H5_BINDINGS_KEY, eventBindings.toString());
                editor.apply();
                SugoWebEventListener.bindEvents(mToken, eventBindings);
            }
        }

        private void handlePageInfoReceived(JSONArray pageInfos) {
            if (!SugoAPI.developmentMode) {
                final SharedPreferences preferences = getSharedPreferences();
                final SharedPreferences.Editor editor = preferences.edit();
                editor.putString(SHARED_PREF_PAGE_INFO_KEY, pageInfos.toString());
                editor.apply();
                SugoPageManager.getInstance().setPageInfos(pageInfos);
            }
        }

        private void handleDimensions(JSONArray array) {
            if (!SugoAPI.developmentMode) {
                final SharedPreferences preferences = getSharedPreferences();
                final SharedPreferences.Editor editor = preferences.edit();
                editor.putString(SHARED_PREF_DIMENSIONS_KEY, array.toString());
                editor.apply();
                SugoDimensionManager.getInstance().setDimensions(array);
            }
        }

        private void handleSendTestEvent(JSONArray events) {
            Log.i(LOGTAG, events.toString());
            if (mEditorConnection == null) {
                return;
            }


            final OutputStream out = mEditorConnection.getBufferedOutputStream();


            try {
                JSONObject eventpkg = new JSONObject();
                eventpkg.put("events", events);
                JSONObject pkg = new JSONObject();
                pkg.put("type", "track_message");
                pkg.put("payload", eventpkg);
                out.write(pkg.toString().getBytes());
            } catch (final JSONException e) {
                Log.e(LOGTAG, "JSON convert Exception", e);
            } catch (final IOException e) {
                Log.e(LOGTAG, "Can't write device_info to server", e);
            } finally {
                try {
                    out.close();
                } catch (final IOException e) {
                    Log.e(LOGTAG, "Can't close websocket writer", e);
                }
            }

//            if(!SugoAPI.developmentMode) {
//                SugoWebEventListener.bindEvents(mToken, eventBindings);
//            }
        }

        /**
         * Accept and apply a temporary event binding from the connected UI.
         */
        private void handleEditorBindingsReceived(JSONObject message) {
            final JSONArray eventBindings;
            final JSONArray h5EventBindings;
            final JSONArray pageInfoBindings;
            final JSONArray dimensionsBindings;
            try {
                final JSONObject payload = message.getJSONObject("payload");
                eventBindings = payload.optJSONArray("events");
                h5EventBindings = payload.optJSONArray("h5_events");
                pageInfoBindings = payload.optJSONArray("page_info");
                dimensionsBindings = payload.optJSONArray("dimensions");
                SugoPageManager.getInstance().setPageInfos(pageInfoBindings);
                SugoWebEventListener.bindEvents(mToken, h5EventBindings);
                SugoDimensionManager.getInstance().setDimensions(dimensionsBindings);
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Bad event bindings received", e);
                return;
            }

            final int eventCount = eventBindings.length();

            mEditorEventBindings.clear();
            for (int i = 0; i < eventCount; i++) {
                try {
                    final JSONObject event = eventBindings.getJSONObject(i);
                    final String targetActivity = JSONUtils.optionalStringKey(event, "target_activity");
                    mEditorEventBindings.add(new Pair<String, JSONObject>(targetActivity, event));
                } catch (final JSONException e) {
                    Log.e(LOGTAG, "Bad event binding received from editor in " + eventBindings.toString(), e);
                }
            }

            applyVariantsAndEventBindings(Collections.<Pair<Integer, Integer>>emptyList());
        }

        /**
         * Clear state associated with the editor now that the editor is gone.
         */
        private void handleEditorClosed() {
            mEditorChanges.clear();
            mEditorEventBindings.clear();

            // Free (or make available) snapshot memory
            mSnapshot = null;

            if (SGConfig.DEBUG) {
                Log.v(LOGTAG, "Editor closed- freeing snapshot");
            }

            applyVariantsAndEventBindings(Collections.<Pair<Integer, Integer>>emptyList());
            for (final String assetUrl : mEditorAssetUrls) {
                mImageStore.deleteStorage(assetUrl);
            }

            SugoAPI.developmentMode = false;
        }

        /**
         * Reads our JSON-stored edits from memory and submits them to our EditState. Overwrites
         * any existing edits at the time that it is run.
         * <p>
         * applyVariantsAndEventBindings should be called any time we load new edits, event bindings,
         * or tweaks from disk or when we receive new edits from the interactive UI editor.
         * Changes and event bindings from our persistent storage and temporary changes
         * received from interactive editing will all be submitted to our EditState, tweaks
         * will be updated, and experiment statuses will be tracked.
         */
        private void applyVariantsAndEventBindings(List<Pair<Integer, Integer>> emptyVariantIds) {
            final List<Pair<String, ViewVisitor>> newVisitors = new ArrayList<Pair<String, ViewVisitor>>();
            final Set<Pair<Integer, Integer>> toTrack = new HashSet<Pair<Integer, Integer>>();

            {
                final int size = mPersistentChanges.size();
                for (int i = 0; i < size; i++) {
                    final VariantChange changeInfo = mPersistentChanges.get(i);
                    try {
                        final EditProtocol.Edit edit = mProtocol.readEdit(changeInfo.change);
                        newVisitors.add(new Pair<String, ViewVisitor>(changeInfo.activityName, edit.visitor));
                        if (!mSeenExperiments.contains(changeInfo.variantId)) {
                            toTrack.add(changeInfo.variantId);
                        }
                    } catch (final EditProtocol.CantGetEditAssetsException e) {
                        Log.v(LOGTAG, "Can't load assets for an edit, won't apply the change now", e);
                    } catch (final EditProtocol.InapplicableInstructionsException e) {
                        Log.i(LOGTAG, e.getMessage());
                    } catch (final EditProtocol.BadInstructionsException e) {
                        Log.e(LOGTAG, "Bad persistent change request cannot be applied.", e);
                    }
                }
            }

            {
                boolean isTweaksUpdated = false;
                final int size = mPersistentTweaks.size();
                for (int i = 0; i < size; i++) {
                    final VariantTweak tweakInfo = mPersistentTweaks.get(i);
                    try {
                        final Pair<String, Object> tweakValue = mProtocol.readTweak(tweakInfo.tweak);

                        if (!mSeenExperiments.contains(tweakInfo.variantId)) {
                            toTrack.add(tweakInfo.variantId);
                            isTweaksUpdated = true;
                        } else if (mTweaks.isNewValue(tweakValue.first, tweakValue.second)) {
                            isTweaksUpdated = true;
                        }

                        mTweaks.set(tweakValue.first, tweakValue.second);
                    } catch (EditProtocol.BadInstructionsException e) {
                        Log.e(LOGTAG, "Bad editor tweak cannot be applied.", e);
                    }
                }

                if (isTweaksUpdated) {
                    for (OnMixpanelTweaksUpdatedListener listener : mTweaksUpdatedListeners) {
                        listener.onMixpanelTweakUpdated();
                    }
                }

                if (size == 0) { // there are no new tweaks, so reset to default values
                    final Map<String, Tweaks.TweakValue> tweakDefaults = mTweaks.getDefaultValues();
                    for (Map.Entry<String, Tweaks.TweakValue> tweak : tweakDefaults.entrySet()) {
                        final Tweaks.TweakValue tweakValue = tweak.getValue();
                        final String tweakName = tweak.getKey();
                        mTweaks.set(tweakName, tweakValue);
                    }
                }
            }

            {
                for (Pair<String, JSONObject> changeInfo : mEditorChanges.values()) {
                    try {
                        final EditProtocol.Edit edit = mProtocol.readEdit(changeInfo.second);
                        newVisitors.add(new Pair<String, ViewVisitor>(changeInfo.first, edit.visitor));
                        mEditorAssetUrls.addAll(edit.imageUrls);
                    } catch (final EditProtocol.CantGetEditAssetsException e) {
                        Log.v(LOGTAG, "Can't load assets for an edit, won't apply the change now", e);
                    } catch (final EditProtocol.InapplicableInstructionsException e) {
                        Log.i(LOGTAG, e.getMessage());
                    } catch (final EditProtocol.BadInstructionsException e) {
                        Log.e(LOGTAG, "Bad editor change request cannot be applied.", e);
                    }
                }
            }

            {
                final int size = mEditorTweaks.size();
                for (int i = 0; i < size; i++) {
                    final JSONObject tweakDesc = mEditorTweaks.get(i);

                    try {
                        final Pair<String, Object> tweakValue = mProtocol.readTweak(tweakDesc);
                        mTweaks.set(tweakValue.first, tweakValue.second);
                    } catch (final EditProtocol.BadInstructionsException e) {
                        Log.e(LOGTAG, "Strange tweaks received", e);
                    }
                }
            }

            {
                final int size = mEditorEventBindings.size();
                for (int i = 0; i < size; i++) {
                    final Pair<String, JSONObject> changeInfo = mEditorEventBindings.get(i);
                    try {
                        final ViewVisitor visitor = mProtocol.readEventBinding(changeInfo.second, mDynamicEventTracker);
                        newVisitors.add(new Pair<String, ViewVisitor>(changeInfo.first, visitor));
                    } catch (final EditProtocol.InapplicableInstructionsException e) {
                        Log.i(LOGTAG, e.getMessage());
                    } catch (final EditProtocol.BadInstructionsException e) {
                        Log.e(LOGTAG, "Bad editor event binding cannot be applied.", e);
                    }
                }
            }

            {
                if (mEditorEventBindings.size() == 0) {
                    final int size = mPersistentEventBindings.size();
                    for (int i = 0; i < size; i++) {
                        final Pair<String, JSONObject> changeInfo = mPersistentEventBindings.get(i);
                        try {
                            final ViewVisitor visitor = mProtocol.readEventBinding(changeInfo.second, mDynamicEventTracker);
                            newVisitors.add(new Pair<String, ViewVisitor>(changeInfo.first, visitor));
                        } catch (final EditProtocol.InapplicableInstructionsException e) {
                            Log.i(LOGTAG, e.getMessage());
                        } catch (final EditProtocol.BadInstructionsException e) {
                            Log.e(LOGTAG, "Bad persistent event binding cannot be applied.", e);
                        }
                    }
                } else {
                    SugoAPI.developmentMode = true;
                }
            }


            final Map<String, List<ViewVisitor>> editMap = new HashMap<String, List<ViewVisitor>>();
            final int totalEdits = newVisitors.size();
            for (int i = 0; i < totalEdits; i++) {
                final Pair<String, ViewVisitor> next = newVisitors.get(i);
                final List<ViewVisitor> mapElement;
                if (editMap.containsKey(next.first)) {
                    mapElement = editMap.get(next.first);
                } else {
                    mapElement = new ArrayList<ViewVisitor>();
                    editMap.put(next.first, mapElement);
                }
                mapElement.add(next.second);
            }

            mEditState.setEdits(editMap);

            for (Pair<Integer, Integer> id : emptyVariantIds) {
                if (!mSeenExperiments.contains(id)) {
                    toTrack.add(id);
                }
            }

            mSeenExperiments.addAll(toTrack);

            if (toTrack.size() > 0) {
                final JSONObject variantObject = new JSONObject();

                try {
                    for (Pair<Integer, Integer> variant : toTrack) {
                        final int experimentId = variant.first;
                        final int variantId = variant.second;

                        final JSONObject trackProps = new JSONObject();
                        trackProps.put("experiment_id", experimentId);
                        trackProps.put("variant_id", variantId);

                        variantObject.put(Integer.toString(experimentId), variantId);

                        mMixpanel.updateSuperProperties(new SuperPropertyUpdate() {
                            public JSONObject update(JSONObject in) {
                                try {
                                    in.put("$experiments", variantObject);
                                } catch (JSONException e) {
                                    Log.wtf(LOGTAG, "Can't write $experiments super property", e);
                                }
                                return in;
                            }
                        });

                        mMixpanel.track(null, "experiment_started", trackProps);
                    }
                } catch (JSONException e) {
                    Log.wtf(LOGTAG, "Could not build JSON for reporting experiment start", e);
                }
            }
        }

        private SharedPreferences getSharedPreferences() {
            final String sharedPrefsName = SHARED_PREF_EDITS_FILE + mToken;
            return mContext.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        }

        private EditorConnection mEditorConnection;
        private ViewSnapshot mSnapshot;
        private final String mToken;
        private final Lock mStartLock;
        private final EditProtocol mProtocol;
        private final ImageStore mImageStore;

        private final Map<String, Pair<String, JSONObject>> mEditorChanges;
        private final List<JSONObject> mEditorTweaks;
        private final List<String> mEditorAssetUrls;
        private final List<Pair<String, JSONObject>> mEditorEventBindings;
        private final List<VariantChange> mPersistentChanges;
        private final List<VariantTweak> mPersistentTweaks;
        private final List<Pair<String, JSONObject>> mPersistentEventBindings;
        private final Set<Pair<Integer, Integer>> mSeenExperiments;
    }

    private class Editor implements EditorConnection.Editor {

        @Override
        public void sendSnapshot(JSONObject message) {
            final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_SEND_STATE_FOR_EDITING);
            msg.obj = message;
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void performEdit(JSONObject message) {
            final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_HANDLE_EDITOR_CHANGES_RECEIVED);
            msg.obj = message;
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void clearEdits(JSONObject message) {
            final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_HANDLE_EDITOR_CHANGES_CLEARED);
            msg.obj = message;
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void setTweaks(JSONObject message) {
            final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_HANDLE_EDITOR_TWEAKS_RECEIVED);
            msg.obj = message;
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void bindEvents(JSONObject message) {
            final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_HANDLE_EDITOR_BINDINGS_RECEIVED);
            msg.obj = message;
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void sendDeviceInfo() {
            final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_SEND_DEVICE_INFO);
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void cleanup() {
            final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_HANDLE_EDITOR_CLOSED);
            mMessageThreadHandler.sendMessage(msg);
        }
    }

    private static class VariantChange {
        public VariantChange(String anActivityName, JSONObject someChange, Pair<Integer, Integer> aVariantId) {
            activityName = anActivityName;
            change = someChange;
            variantId = aVariantId;
        }

        public final String activityName;
        public final JSONObject change;
        public final Pair<Integer, Integer> variantId;
    }

    private static class VariantTweak {
        public VariantTweak(JSONObject aTweak, Pair<Integer, Integer> aVariantId) {
            tweak = aTweak;
            variantId = aVariantId;
        }

        public final JSONObject tweak;
        public final Pair<Integer, Integer> variantId;
    }

    private String secretKey = null;
    private String scanUrlType = null;
    private final SGConfig mConfig;
    private final Context mContext;
    private final SugoAPI mMixpanel;
    private final DynamicEventTracker mDynamicEventTracker;
    private final EditState mEditState;
    private final Tweaks mTweaks;
    private final Map<String, String> mDeviceInfo;
    private final ViewCrawlerHandler mMessageThreadHandler;
    private final float mScaledDensity;

    private final List<OnMixpanelTweaksUpdatedListener> mTweaksUpdatedListeners;

    public static final String SHARED_PREF_EDITS_FILE = "mixpanel.viewcrawler.changes";
    public static final String SHARED_PREF_CHANGES_KEY = "mixpanel.viewcrawler.changes";
    public static final String SHARED_PREF_BINDINGS_KEY = "mixpanel.viewcrawler.bindings";
    public static final String SHARED_PREF_H5_BINDINGS_KEY = "mixpanel.viewcrawler.h5_bindings";
    public static final String SHARED_PREF_PAGE_INFO_KEY = "mixpanel.viewcrawler.page_info";
    public static final String SHARED_PREF_DIMENSIONS_KEY = "mixpanel.viewcrawler.dimensions";
    public static final String SP_EVENT_BINDING_VERSION = "sugo.event_bindings_version";
    public static final String SP_DIMENSION_VERSION = "sugo.dimension_version";
    public static final String SP_EVENT_BINDINGS_APP_VERSION = "sugo.event_bindings_app_version";
    public static final String POSITION_CONFIG = "sugo_position_config";
    public static final String LAST_REPORT_LOCATION = "sugo_last_report_location";

    private static final int MESSAGE_INITIALIZE_CHANGES = 0;
    private static final int MESSAGE_CONNECT_TO_EDITOR = 1;
    private static final int MESSAGE_SEND_STATE_FOR_EDITING = 2;
    private static final int MESSAGE_HANDLE_EDITOR_CHANGES_RECEIVED = 3;
    private static final int MESSAGE_SEND_DEVICE_INFO = 4;
    private static final int MESSAGE_EVENT_BINDINGS_RECEIVED = 5;
    private static final int MESSAGE_HANDLE_EDITOR_BINDINGS_RECEIVED = 6;
    private static final int MESSAGE_SEND_EVENT_TRACKED = 7;
    private static final int MESSAGE_HANDLE_EDITOR_CLOSED = 8;
    private static final int MESSAGE_VARIANTS_RECEIVED = 9;
    private static final int MESSAGE_HANDLE_EDITOR_CHANGES_CLEARED = 10;
    private static final int MESSAGE_HANDLE_EDITOR_TWEAKS_RECEIVED = 11;
    private static final int MESSAGE_SEND_LAYOUT_ERROR = 12;
    private static final int MESSAGE_H5_EVENT_BINDINGS_RECEIVED = 13;
    private static final int MESSAGE_SEND_TEST_EVENT = 14;
    private static final int MESSAGE_HANDLE_PAGE_INFO_EVENT = 15;
    private static final int MESSAGE_HANDLE_DIMENSIONS_EVENT = 16;
    private static final int MESSAGE_GET_HEAT_MAP_DATA = 17;
    private static final int EMULATOR_CONNECT_ATTEMPT_INTERVAL_MILLIS = 1000 * 30;

    @SuppressWarnings("unused")
    private static final String LOGTAG = "SugoAPI.ViewCrawler";
}
