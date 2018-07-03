package io.sugo.android.viewcrawler;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLSocketFactory;

import io.sugo.android.metrics.ResourceIds;
import io.sugo.android.metrics.AbsResourceReader;
import io.sugo.android.metrics.SGConfig;
import io.sugo.android.metrics.SugoAPI;
import io.sugo.android.metrics.SugoDimensionManager;
import io.sugo.android.metrics.SugoPageManager;
import io.sugo.android.metrics.SugoWebEventListener;
import io.sugo.android.metrics.SystemInformation;
import io.sugo.android.util.HttpService;
import io.sugo.android.util.ImageStore;
import io.sugo.android.util.JSONUtils;
import io.sugo.android.util.RemoteService;

/**
 * This class is for internal use by the Sugo API, and should
 * not be called directly by your code.
 */
@TargetApi(SGConfig.UI_FEATURES_MIN_API)
public class ViewCrawler implements UpdatesFromSugo, TrackingDebug, ViewVisitor.OnLayoutErrorListener {

    private static final String LOGTAG = "SugoAPI.ViewCrawler";

    public static final String SHARED_PREF_EDITS_FILE = "sugo.viewcrawler.changes";

    public static final String SHARED_PREF_BINDINGS_KEY = "sugo.viewcrawler.bindings";
    public static final String SHARED_PREF_H5_BINDINGS_KEY = "sugo.viewcrawler.h5_bindings";
    public static final String SHARED_PREF_PAGE_INFO_KEY = "sugo.viewcrawler.page_info";
    public static final String SHARED_PREF_DIMENSIONS_KEY = "sugo.viewcrawler.dimensions";
    public static final String SP_EVENT_BINDING_VERSION = "sugo.event_bindings_version";
    public static final String SP_EVENT_BINDINGS_APP_VERSION = "sugo.event_bindings_app_version";

    private static final int MESSAGE_INITIALIZE_CHANGES = 0;
    private static final int MESSAGE_CONNECT_TO_EDITOR = 1;
    private static final int MESSAGE_SEND_STATE_FOR_EDITING = 2;
    private static final int MESSAGE_HANDLE_EDITOR_CHANGES_RECEIVED = 3;
    private static final int MESSAGE_SEND_DEVICE_INFO = 4;
    private static final int MESSAGE_EVENT_BINDINGS_RECEIVED = 5;
    private static final int MESSAGE_HANDLE_EDITOR_BINDINGS_RECEIVED = 6;
    private static final int MESSAGE_SEND_EVENT_TRACKED = 7;
    private static final int MESSAGE_HANDLE_EDITOR_CLOSED = 8;
    private static final int MESSAGE_HANDLE_EDITOR_CHANGES_CLEARED = 10;
    private static final int MESSAGE_HANDLE_EDITOR_TWEAKS_RECEIVED = 11;
    private static final int MESSAGE_SEND_LAYOUT_ERROR = 12;
    private static final int MESSAGE_H5_EVENT_BINDINGS_RECEIVED = 13;
    private static final int MESSAGE_SEND_TEST_EVENT = 14;
    private static final int MESSAGE_HANDLE_PAGE_INFO_EVENT = 15;
    private static final int MESSAGE_HANDLE_DIMENSIONS_EVENT = 16;
    private static final int MESSAGE_GET_HEAT_MAP_DATA = 17;


    private String secretKey = null;
    private String scanUrlType = null;
    private final SGConfig mConfig;
    private final Context mContext;
    private final DynamicEventTracker mDynamicEventTracker;
    private final BindingState mBindingState;
    private final Map<String, String> mDeviceInfo;
    private final ViewCrawlerHandler mMessageThreadHandler;
    private final float mScaledDensity;
    private XWalkViewListener mXWalkViewListener;

    public ViewCrawler(Context context, String token, SugoAPI sugo, XWalkViewListener XWalkViewListener) {
        this(context, token, sugo);
        this.mXWalkViewListener = XWalkViewListener;
    }

    public ViewCrawler(Context context, String token, SugoAPI sugo) {
        mConfig = SGConfig.getInstance(context);

        Context appContext = context.getApplicationContext();
        mContext = appContext;
        restorePageInfo();
        restoreDimensions();

        mBindingState = new BindingState();
        mDeviceInfo = getDeviceInfo();
        mScaledDensity = Resources.getSystem().getDisplayMetrics().scaledDensity;

        final Application app = (Application) context.getApplicationContext();
        app.registerActivityLifecycleCallbacks(new LifecycleCallbacks());

        final HandlerThread thread = new HandlerThread(ViewCrawler.class.getCanonicalName());
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mMessageThreadHandler = new ViewCrawlerHandler(appContext, token, thread.getLooper(), this);

        mDynamicEventTracker = new DynamicEventTracker(sugo, mMessageThreadHandler);
    }

    private Map<String, String> getDeviceInfo() {
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
        return Collections.unmodifiableMap(deviceInfo);
    }

    private SharedPreferences getSharedPreferences() {
        final String sharedPrefsName = SHARED_PREF_EDITS_FILE + mConfig.getToken();
        return mContext.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
    }

    private void restorePageInfo() {
        final SharedPreferences preferences = getSharedPreferences();
        final String storeInfo = preferences.getString(ViewCrawler.SHARED_PREF_PAGE_INFO_KEY, null);
        if (!TextUtils.isEmpty(storeInfo)) {
            try {
                SugoPageManager.getInstance().setPageInfos(new JSONArray(storeInfo));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void restoreDimensions() {
        final SharedPreferences preferences = getSharedPreferences();
        String storeInfo = preferences.getString(ViewCrawler.SHARED_PREF_DIMENSIONS_KEY, null);
        if (!TextUtils.isEmpty(storeInfo)) {
            try {
                SugoDimensionManager.getInstance().setDimensions(new JSONArray(storeInfo));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
    public void setXWalkViewListener(XWalkViewListener XWalkViewListener) {
        mXWalkViewListener = XWalkViewListener;
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

    @Override
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

    private class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        public LifecycleCallbacks() {
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
            //bindComponent(activity);
            tryToConnectToEditor(activity);
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            bindComponent(activity);
            tryToConnectToEditor(activity);
        }

        private void tryToConnectToEditor(Activity activity){
            if(!SugoAPI.editorConnected){
                Uri data = activity.getIntent().getData();
                if (data != null) {
                    sendConnectEditor(data);
                }
            }
        }

        private void bindComponent(Activity activity){
            if (!mBindingState.getAll().contains(activity))
                mBindingState.add(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            mBindingState.remove(activity);
        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            mBindingState.destroyBindings(activity);
            SugoWebEventListener.cleanUnuseWebView(activity);
            if (mXWalkViewListener != null) {
                mXWalkViewListener.recyclerXWalkView(activity);
            }
        }

    }

    private class ViewCrawlerHandler extends Handler {

        private EditorConnection mEditorConnection;
        private ViewSnapshot mSnapshot;
        private final String mToken;
        private final Lock mStartLock;
        private final BindingProtocol mProtocol;
        private final ImageStore mImageStore;

        private final List<String> mEditorAssetUrls;
        private final List<Pair<String, JSONObject>> mEditorEventBindings;

        /**
         * key:target_activity, value:event_binding
         */
        private final List<Pair<String, JSONObject>> mPersistentEventBindings;

        public ViewCrawlerHandler(Context context, String token, Looper looper, ViewVisitor.OnLayoutErrorListener layoutErrorListener) {
            super(looper);
            mToken = token;
            mSnapshot = null;

            String resourcePackage = mConfig.getResourcePackageName();
            if (null == resourcePackage) {
                resourcePackage = context.getPackageName();
            }

            final ResourceIds resourceIds = new AbsResourceReader.Ids(resourcePackage, context);

            mImageStore = new ImageStore(context, "ViewCrawler");
            mProtocol = new BindingProtocol(resourceIds, mImageStore, layoutErrorListener);
            mEditorAssetUrls = new ArrayList<>();
            mEditorEventBindings = new ArrayList<>();
            mPersistentEventBindings = new ArrayList<>();
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
                        initializeBindings();
                        break;
                    case MESSAGE_GET_HEAT_MAP_DATA:
                        // 热图模式下，无需再次热图
                        // 可视化埋点模式下，无法进入热图
                        if (!SugoHeatMap.isShowHeatMap() && (!SugoAPI.editorConnected)) {
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
                    case MESSAGE_HANDLE_EDITOR_CLOSED:
                        handleEditorClosed();
                        break;
                    default:
                        break;
                }
            } finally {
                mStartLock.unlock();
            }
        }

        /**
         * Load stored changes from persistent storage and apply them to the application.
         */
        private void initializeBindings() {
            final SharedPreferences preferences = getSharedPreferences();
            final String storedBindings = preferences.getString(SHARED_PREF_BINDINGS_KEY, null);

            try {
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
                Log.i(LOGTAG, "JSON error when initializing saved bindings, clearing persistent memory", e);
                final SharedPreferences.Editor editor = preferences.edit();
                editor.remove(SHARED_PREF_BINDINGS_KEY);
                editor.remove(SHARED_PREF_H5_BINDINGS_KEY);
                editor.remove(SP_EVENT_BINDING_VERSION);
                editor.apply();
            }

            applyEventBindings();
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

            final SharedPreferences preferences = getSharedPreferences();
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
                if (url.startsWith("wss://")) {
                    socket = socketFactory.createSocket();
                } else {
                    socket = new Socket();
                }
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
                j.endObject(); // payload
                j.endObject();
            } catch (final IOException e) {
                Log.e(LOGTAG, "Can't write device_info to server", e);
            } finally {
                try {
                    j.close();
                } catch (final IOException e) {
                    Log.e(LOGTAG, "Can't close WebSocket writer", e);
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
                    if (mXWalkViewListener != null) {
                        mSnapshot.setXWalkViewListener(mXWalkViewListener);
                    } else {
                        mSnapshot.setXWalkViewListener(null);
                    }
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
            } catch (final BindingProtocol.BadInstructionsException e) {
                Log.e(LOGTAG, "Editor sent malformed message with snapshot request", e);
                sendError(e.getMessage());
                return;
            }

            if (null == mSnapshot) {
                sendError("No snapshot configuration (or a malformed snapshot configuration) was sent.");
                Log.w(LOGTAG, "Sugo editor is misconfigured, sent a snapshot request without a valid configuration.");
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
                    mSnapshot.snapshots(mBindingState, out, bitmapHash);
                }

                final long snapshotTime = System.currentTimeMillis() - startSnapshot;
                writer.write(",\"snapshot_time_millis\": ");
                writer.write(Long.toString(snapshotTime));

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

        private void handleHeatMapData(String heatmapData) {
            SugoHeatMap.setHeatMapData(mContext, heatmapData);
            initializeBindings();
            SugoWebEventListener.updateWebViewInject();
        }


        /**
         * Accept and apply a persistent event binding from a non-interactive source.
         */
        private void handleEventBindingsReceived(JSONArray eventBindings) {
            final SharedPreferences preferences = getSharedPreferences();
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(SHARED_PREF_BINDINGS_KEY, eventBindings.toString());
            editor.apply();
            initializeBindings();
        }

        /**
         * Accept and apply a persistent h5 event binding from a non-interactive source.
         */
        private void handleH5EventBindingsReceived(JSONArray eventBindings) {
            final SharedPreferences preferences = getSharedPreferences();
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(SHARED_PREF_H5_BINDINGS_KEY, eventBindings.toString());
            editor.apply();
            SugoWebEventListener.bindEvents(mToken, eventBindings);
        }

        private void handlePageInfoReceived(JSONArray pageInfos) {
            final SharedPreferences preferences = getSharedPreferences();
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(SHARED_PREF_PAGE_INFO_KEY, pageInfos.toString());
            editor.apply();
            SugoPageManager.getInstance().setPageInfos(pageInfos);
        }

        private void handleDimensions(JSONArray array) {
            final SharedPreferences preferences = getSharedPreferences();
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(SHARED_PREF_DIMENSIONS_KEY, array.toString());
            editor.apply();
            SugoDimensionManager.getInstance().setDimensions(array);
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
                if (mXWalkViewListener != null) {
                    mXWalkViewListener.bindEvents(mToken, eventBindings);
                }
                if (dimensionsBindings != null) {
                    SugoDimensionManager.getInstance().setDimensions(dimensionsBindings);
                }
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

            applyEventBindings();
        }

        /**
         * Clear state associated with the editor now that the editor is gone.
         */
        private void handleEditorClosed() {
            mEditorEventBindings.clear();

            // Free (or make available) snapshot memory
            mSnapshot = null;

            if (SGConfig.DEBUG) {
                Log.v(LOGTAG, "Editor closed- freeing snapshot");
            }

            applyEventBindings();
            for (final String assetUrl : mEditorAssetUrls) {
                mImageStore.deleteStorage(assetUrl);
            }
        }

        /**
         * Reads our JSON-stored edits from memory and submits them to our BindingState. Overwrites
         * any existing edits at the time that it is run.
         * <p>
         * applyEventBindings should be called any time we load new edits, event bindings,
         * or tweaks from disk or when we receive new edits from the interactive UI editor.
         * Changes and event bindings from our persistent storage and temporary changes
         * received from interactive editing will all be submitted to our BindingState, tweaks
         * will be updated, and experiment statuses will be tracked.
         */
        private void applyEventBindings() {
            // key:target_activity, value:visitor(根据 event_binding 生成)
            final List<Pair<String, ViewVisitor>> newVisitors = new ArrayList<Pair<String, ViewVisitor>>();

            if (SugoAPI.editorConnected) {
                final int size = mEditorEventBindings.size();
                for (int i = 0; i < size; i++) {
                    final Pair<String, JSONObject> changeInfo = mEditorEventBindings.get(i);
                    try {
                        final ViewVisitor visitor = mProtocol.readEventBinding(changeInfo.second, mDynamicEventTracker);
                        newVisitors.add(new Pair<>(changeInfo.first, visitor));
                    } catch (final BindingProtocol.InapplicableInstructionsException e) {
                        Log.i(LOGTAG, e.getMessage());
                    } catch (final BindingProtocol.BadInstructionsException e) {
                        Log.e(LOGTAG, "Bad editor event binding cannot be applied.", e);
                    }
                }
            } else {
                // 没有可视化埋点下发的临时测试事件，则使用持久的绑定事件
                final int size = mPersistentEventBindings.size();
                for (int i = 0; i < size; i++) {
                    final Pair<String, JSONObject> changeInfo = mPersistentEventBindings.get(i);
                    try {
                        final ViewVisitor visitor = mProtocol.readEventBinding(changeInfo.second, mDynamicEventTracker);
                        newVisitors.add(new Pair<>(changeInfo.first, visitor));
                    } catch (final BindingProtocol.InapplicableInstructionsException e) {
                        Log.i(LOGTAG, e.getMessage());
                    } catch (final BindingProtocol.BadInstructionsException e) {
                        Log.e(LOGTAG, "Bad persistent event binding cannot be applied.", e);
                    }
                }
            }


            // 根据 target_activity ，List 转 Map
            final Map<String, List<ViewVisitor>> bindingMap = new HashMap<String, List<ViewVisitor>>();
            final int totalBindings = newVisitors.size();
            for (int i = 0; i < totalBindings; i++) {
                final Pair<String, ViewVisitor> next = newVisitors.get(i);
                final List<ViewVisitor> mapElement;
                if (bindingMap.containsKey(next.first)) {
                    mapElement = bindingMap.get(next.first);
                } else {
                    mapElement = new ArrayList<ViewVisitor>();
                    bindingMap.put(next.first, mapElement);
                }
                mapElement.add(next.second);
            }

            mBindingState.setBindings(bindingMap);

        }

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

    public BindingState getmBindingState() {
        return mBindingState;
    }


}
