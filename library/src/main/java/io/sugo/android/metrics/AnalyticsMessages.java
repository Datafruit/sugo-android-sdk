package io.sugo.android.metrics;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import io.sugo.android.util.Base64Coder;
import io.sugo.android.util.HttpService;
import io.sugo.android.util.RemoteService;
import io.sugo.android.viewcrawler.UpdatesFromSugo;
import io.sugo.android.viewcrawler.ViewCrawler;

/**
 * 管理 events 与内部数据、Sugo 服务器的沟通
 * 将用户线程的操作，转移到 worker 中执行
 */
class AnalyticsMessages {

    private static final String LOGTAG = "SugoAPI.Messages";

    private static final Map<Context, AnalyticsMessages> sInstances = new HashMap<Context, AnalyticsMessages>();

    // Used across thread boundaries
    private final Worker mWorker;
    private final Context mContext;
    private final SGConfig mConfig;
    private final SystemInformation mSystemInformation;
    private final UpdatesFromSugo mUpdatesFromSugo;

    // Messages for our thread

    // push given JSON message to people DB
    private static final int ENQUEUE_EVENTS = 1;
    // push given JSON message to events DB
    private static final int FLUSH_QUEUE = 2;
    // Hard-kill the worker thread, discarding all events on the event queue. This is for testing, or disasters.
    private static final int KILL_WORKER = 5;
    // Run this DecideCheck at intervals until it isDestroyed()
    private static final int START_API_CHECK = 12;
    // Run this DecideCheck at intervals until it isDestroyed()
    private static final int UPDATE_API_CHECK = 14;

    private static final int LOGIN_API_CKECK = 16;


    /**
     * Do not call directly. You should call AnalyticsMessages.getInstance()
     */
    private AnalyticsMessages(final Context context, final UpdatesFromSugo updatesFromSugo) {
        mContext = context;
        mConfig = getConfig(context);
        mUpdatesFromSugo = updatesFromSugo;
        mSystemInformation = new SystemInformation(mContext);
        mWorker = createWorker();
//        getPoster().checkIsSugoBlocked();
    }

    /**
     * Use this to get an instance of AnalyticsMessages instead of creating one directly
     * for yourself.
     *
     * @param messageContext should be the Main Activity of the application
     *                       associated with these messages.
     */
    public static AnalyticsMessages getInstance(final Context messageContext, final UpdatesFromSugo updatesFromSugo) {
        synchronized (sInstances) {
            final Context appContext = messageContext.getApplicationContext();
            AnalyticsMessages ret;
            if (!sInstances.containsKey(appContext)) {
                ret = new AnalyticsMessages(appContext, updatesFromSugo);
                sInstances.put(appContext, ret);
            } else {
                ret = sInstances.get(appContext);
            }
            return ret;
        }
    }

    private SGConfig getConfig(Context context) {
        return SGConfig.getInstance(context);
    }

    private RemoteService getPoster() {
        return new HttpService();
    }

    private Worker createWorker() {
        return new Worker();
    }

    public JSONObject getDefaultEventProperties() throws JSONException {
        final JSONObject ret = new JSONObject();

        ret.put(SGConfig.FIELD_MP_LIB, "android");
        ret.put(SGConfig.FIELD_LIB_VERSION, SGConfig.VERSION);

        // For querying together with data from other libraries
        ret.put(SGConfig.FIELD_OS, "Android");
        ret.put(SGConfig.FIELD_OS_VERSION, Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);

        ret.put(SGConfig.FIELD_MANUFACTURER, Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
        ret.put(SGConfig.FIELD_BRAND, Build.BRAND == null ? "UNKNOWN" : Build.BRAND);
        ret.put(SGConfig.FIELD_MODEL, Build.MODEL == null ? "UNKNOWN" : Build.MODEL);

        final DisplayMetrics displayMetrics = mSystemInformation.getDisplayMetrics();
        ret.put(SGConfig.FIELD_SCREEN_DPI, displayMetrics.densityDpi);
        ret.put(SGConfig.FIELD_SCREEN_PIXEL, displayMetrics.widthPixels + "*" + displayMetrics.heightPixels);

        final String applicationVersionName = mSystemInformation.getAppVersionName();
        if (null != applicationVersionName) {
            //ret.put("app_version", applicationVersionName);
            ret.put(SGConfig.FIELD_APP_VERSION_STRING, applicationVersionName);
        }

        final Integer applicationVersionCode = mSystemInformation.getAppVersionCode();
        if (null != applicationVersionCode) {
            //ret.put("app_release", applicationVersionCode);
            ret.put(SGConfig.FIELD_APP_BUILD_NUMBER, applicationVersionCode);
        }

        final Boolean hasNFC = mSystemInformation.hasNFC();
        if (null != hasNFC) {
            ret.put(SGConfig.FIELD_HAS_NFC, hasNFC.booleanValue());
        }
        final Boolean hasTelephony = mSystemInformation.hasTelephony();
        if (null != hasTelephony) {
            ret.put(SGConfig.FIELD_HAS_TELEPHONE, hasTelephony.booleanValue());
        }
        final String carrier = mSystemInformation.getCurrentNetworkOperator();
        if (null != carrier) {
            ret.put(SGConfig.FIELD_CARRIER, carrier);
        }
        final String networkType = mSystemInformation.getNetworkType();
        if (null != networkType) {
            ret.put(SGConfig.FIELD_CLIENT_NETWORK, networkType);
        }

        final Boolean isWifi = mSystemInformation.isWifiConnected();
        if (null != isWifi) {
            ret.put(SGConfig.FIELD_WIFI, isWifi.booleanValue());
        }
        final Boolean isBluetoothEnabled = mSystemInformation.isBluetoothEnabled();
        if (isBluetoothEnabled != null) {
            ret.put(SGConfig.FIELD_BLUETOOTH_ENABLED, isBluetoothEnabled);
        }
        final String bluetoothVersion = mSystemInformation.getBluetoothVersion();
        if (bluetoothVersion != null) {
            ret.put(SGConfig.FIELD_BLUETOOTH_VERSION, bluetoothVersion);
        }
        final String deviceId = mSystemInformation.getDeviceId();
        if (null != deviceId) {
            ret.put(SGConfig.FIELD_DEVICE_ID, deviceId);
        }
        return ret;
    }

    public void eventsMessage(final EventDescription eventDescription) {
        final Message m = Message.obtain();
        m.what = ENQUEUE_EVENTS;
        m.obj = eventDescription;
        mWorker.runMessage(m);
    }

    public void postToServer() {
        final Message m = Message.obtain();
        m.what = FLUSH_QUEUE;
        mWorker.runMessage(m);
    }

    public void startApiCheck() {
        final Message m = Message.obtain();
        m.what = START_API_CHECK;
        mWorker.runMessage(m);
    }

    public void login(String userIdKey, String userIdValue){
        final Message m = Message.obtain();
        m.what = LOGIN_API_CKECK;
        Map<String,String> map = new HashMap<>();
        map.put("userIdkey",userIdKey);
        map.put("userIdValue",userIdValue);
        m.obj=map;
        mWorker.runMessage(m);
    }

    public void hardKill() {
        final Message m = Message.obtain();
        m.what = KILL_WORKER;
        mWorker.runMessage(m);
    }

    boolean isDead() {
        return mWorker.isDead();
    }

    protected SugoDbAdapter makeDbAdapter(Context context) {
        return new SugoDbAdapter(context);
    }

    ////////////////////////////////////////////////////

    // Worker will manage the (at most single) IO thread associated with
    // this AnalyticsMessages instance.
    // XXX: Worker class is unnecessary, should be just a subclass of HandlerThread
    class Worker {

        private final Object mHandlerLock = new Object();
        private Handler mHandler;
        private long mFlushCount = 0;
        private long mAveFlushFrequency = 0;
        private long mLastFlushTime = -1;

        public Worker() {
            mHandler = restartWorkerThread();
        }

        protected Handler restartWorkerThread() {
            final HandlerThread thread = new HandlerThread("com.sugo.android.AnalyticsWorker", Thread.MIN_PRIORITY);
            thread.start();
            final Handler ret = new AnalyticsMessageHandler(thread.getLooper());
            return ret;
        }

        public boolean isDead() {
            synchronized (mHandlerLock) {
                return mHandler == null;
            }
        }

        public void runMessage(Message msg) {
            synchronized (mHandlerLock) {
                if (mHandler == null) {
                    // We died under suspicious circumstances. Don't try to send any more events.
                    log("Dead sugo worker dropping a message: " + msg.what);
                } else {
                    mHandler.sendMessage(msg);
                }
            }
        }

        private void updateFlushFrequency() {
            final long now = System.currentTimeMillis();
            final long newFlushCount = mFlushCount + 1;

            if (mLastFlushTime > 0) {
                final long flushInterval = now - mLastFlushTime;
                final long totalFlushTime = flushInterval + (mAveFlushFrequency * mFlushCount);
                mAveFlushFrequency = totalFlushTime / newFlushCount;

                final long seconds = mAveFlushFrequency / 1000;
                if (SGConfig.DEBUG) {
                    log("Average send frequency approximately " + seconds + " seconds.");
                }
            }

            mLastFlushTime = now;
            mFlushCount = newFlushCount;
        }

        class AnalyticsMessageHandler extends Handler {

            private SugoDbAdapter mDbAdapter;
            private final ApiChecker mApiChecker;
            private final long mFlushInterval;
            private final long mUpdateDecideInterval;
            private final boolean mDisableFallback;
            private long mDecideRetryAfter;
            private long mTrackEngageRetryAfter;
            private int mFailedRetries;

            public AnalyticsMessageHandler(Looper looper) {
                super(looper);
                mDbAdapter = null;
                mApiChecker = createApiChecker();
                mDisableFallback = mConfig.getDisableFallback();
                mFlushInterval = mConfig.getFlushInterval();
                mUpdateDecideInterval = mConfig.getUpdateDecideInterval();
            }

            protected ApiChecker createApiChecker() {
                return new ApiChecker(mContext, mConfig, mSystemInformation, mUpdatesFromSugo);
            }

            private void sendAllData(SugoDbAdapter dbAdapter) {
                final RemoteService poster = getPoster();
                if (!poster.isOnline(mContext, mConfig.getOfflineMode())) {
                    log("Not flushing data to Sugo because the device is not connected to the internet.");
                    return;
                }

                if (mDisableFallback) {
                    sendData(dbAdapter, SugoDbAdapter.Table.EVENTS, new String[]{mConfig.getEventsEndpoint()});
                } else {
                    sendData(dbAdapter, SugoDbAdapter.Table.EVENTS,
                            new String[]{mConfig.getEventsEndpoint(), mConfig.getEventsFallbackEndpoint()});
                }
            }

            private void sendData(SugoDbAdapter dbAdapter, SugoDbAdapter.Table table, String[] urls) {
                final RemoteService poster = getPoster();
                String[] eventsData = dbAdapter.generateDataString(table);
                Integer queueCount = 0;
                if (eventsData != null) {
                    queueCount = Integer.valueOf(eventsData[2]);
                }

                while (eventsData != null && queueCount > 0) {
                    final String lastId = eventsData[0];
                    final String rawMessage = eventsData[1];

                    final String encodedData = Base64Coder.encodeString(rawMessage);
                    boolean deleteEvents = true;
                    byte[] response;
                    for (String url : urls) {
                        try {
                            final SSLSocketFactory socketFactory = mConfig.getSSLSocketFactory();
                            response = poster.performRawRequest(url, encodedData, socketFactory);
                            if (null == response) {
                                deleteEvents = false;
                                log("Response was null, unexpected failure posting to " + url + ".");
                            } else {
                                // Delete events on any successful post, regardless of 1 or 0 response
                                deleteEvents = true;
                                String parsedResponse;
                                try {
                                    parsedResponse = new String(response, "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    throw new RuntimeException("UTF not supported on this platform?", e);
                                }
                                if (mFailedRetries > 0) {
                                    mFailedRetries = 0;
                                    removeMessages(FLUSH_QUEUE);
                                }

                                log("Successfully posted to " + url + ": \n" + rawMessage);
                                log("Response was " + parsedResponse);
                            }
                            break;
                        } catch (final OutOfMemoryError e) {
                            Log.e(LOGTAG, "Out of memory when posting to " + url + ".", e);
                            break;
                        } catch (final MalformedURLException e) {
                            Log.e(LOGTAG, "Cannot interpret " + url + " as a URL.", e);
                            break;
                        } catch (final RemoteService.ServiceUnavailableException e) {
                            log("Cannot post message to " + url + ".", e);
                            deleteEvents = false;
                            mTrackEngageRetryAfter = e.getRetryAfter() * 1000;
                        } catch (final SocketTimeoutException e) {
                            log("Cannot post message to " + url + ".", e);
                            deleteEvents = false;
                        } catch (final IOException e) {
                            log("Cannot post message to " + url + ".", e);
                            deleteEvents = false;
                        }
                    }

                    if (deleteEvents) {
                        log("Not retrying this batch of events, deleting them from DB.");
                        dbAdapter.cleanupEvents(lastId, table);
                    } else {
                        removeMessages(FLUSH_QUEUE);
                        mTrackEngageRetryAfter = Math.max((long) Math.pow(2, mFailedRetries) * 60000, mTrackEngageRetryAfter);
                        mTrackEngageRetryAfter = Math.min(mTrackEngageRetryAfter, 10 * 60 * 1000);
                        sendEmptyMessageDelayed(FLUSH_QUEUE, mTrackEngageRetryAfter);
                        mFailedRetries++;
                        log("Retrying this batch of events in " + mTrackEngageRetryAfter + " ms");
                        break;
                    }

                    eventsData = dbAdapter.generateDataString(table);
                    if (eventsData != null) {
                        queueCount = Integer.valueOf(eventsData[2]);
                    }
                }
            }

            private JSONObject prepareEventObject(EventDescription eventDescription) throws JSONException {
                final JSONObject eventProperties = eventDescription.getProperties();
                final JSONObject sendProperties = getDefaultEventProperties();
                sendProperties.put(SGConfig.FIELD_TOKEN, eventDescription.getToken());
                if (eventProperties != null) {
                    for (final Iterator<?> iter = eventProperties.keys(); iter.hasNext(); ) {
                        final String key = (String) iter.next();
                        sendProperties.put(key, eventProperties.get(key));
                    }
                }
                String eventId = eventDescription.getEventId();
                if (eventId != null) {
                    sendProperties.put(SGConfig.FIELD_EVENT_ID, eventId);
                }
                sendProperties.put(SGConfig.FIELD_EVENT_NAME, eventDescription.getEventName());
                return sendProperties;
            }

            @Override
            public void handleMessage(Message msg) {
                if (mDbAdapter == null) {
                    mDbAdapter = makeDbAdapter(mContext);
                    mDbAdapter.cleanupEvents(System.currentTimeMillis() - mConfig.getDataExpiration(), SugoDbAdapter.Table.EVENTS);
                }

                try {
                    int returnCode = SugoDbAdapter.DB_UNDEFINED_CODE;
                    if (msg.what == ENQUEUE_EVENTS) {
                        // 将事件存入数据库
                        final EventDescription eventDescription = (EventDescription) msg.obj;
                        try {
                            final JSONObject message = prepareEventObject(eventDescription);
                            log("Queuing event for sending later");
                            log("    " + message.toString());
                            returnCode = mDbAdapter.addJSON(message, SugoDbAdapter.Table.EVENTS);
                        } catch (final JSONException e) {
                            Log.e(LOGTAG, "Exception tracking event " + eventDescription.getEventName(), e);
                        }
                    } else if (msg.what == FLUSH_QUEUE) {
                        // 将数据库的事件推向服务器
                        final String storeInfo = getStoreInfo();
                        if (TextUtils.isEmpty(storeInfo) || storeInfo.equals("[]")) {
                            log("empty dimensions, Flushing do not work !!!");
                        } else {
                            log("Flushing queue due to scheduled or forced flush");
                            updateFlushFrequency();
                            sendAllData(mDbAdapter);
                        }
                    } else if (msg.what == START_API_CHECK) {
                        log("start a check for api");
                        try {
                            mApiChecker.runDecideChecks(getPoster());
                            if (mUpdateDecideInterval < 1000) {
                                sendEmptyMessageDelayed(UPDATE_API_CHECK, 600000);
                            } else {
                                sendEmptyMessageDelayed(UPDATE_API_CHECK, mUpdateDecideInterval);
                            }
                        } catch (RemoteService.ServiceUnavailableException e) {
                            int retryAfterSecond = e.getRetryAfter();
                            mDecideRetryAfter = SystemClock.elapsedRealtime() + retryAfterSecond * 1000;
                            sendEmptyMessageDelayed(UPDATE_API_CHECK, retryAfterSecond * 1000);
                        }
                    } else if (msg.what == UPDATE_API_CHECK) {
                        if (SystemClock.elapsedRealtime() >= mDecideRetryAfter) {
                            try {
                                // 当处于可视化埋点编辑模式时，不请求 API 配置
                                if (!SugoAPI.editorConnected) {
                                    mApiChecker.runDecideChecks(getPoster());
                                    if (mUpdateDecideInterval < 1000) {
                                        sendEmptyMessageDelayed(UPDATE_API_CHECK, 600000);
                                    } else {
                                        sendEmptyMessageDelayed(UPDATE_API_CHECK, mUpdateDecideInterval);
                                    }
                                }
                            } catch (RemoteService.ServiceUnavailableException e) {
                                int retryAfterSecond = e.getRetryAfter();
                                mDecideRetryAfter = SystemClock.elapsedRealtime() + retryAfterSecond * 1000;
                                sendEmptyMessageDelayed(UPDATE_API_CHECK, retryAfterSecond * 1000);
                            }
                        }
                    } else if (msg.what == KILL_WORKER) {
                        Log.w(LOGTAG, "Worker received a hard kill. \n" +
                                "Delete all events and force-killing. \n" +
                                "Thread id " + Thread.currentThread().getId());
                        synchronized (mHandlerLock) {
                            mDbAdapter.deleteDB();
                            mHandler.removeCallbacksAndMessages(null);
                            mHandler = null;
                            Looper.myLooper().quit();
                        }
                    } else if(msg.what == LOGIN_API_CKECK ){
                        Map<String,String> map = new HashMap<>();
                        map=(Map<String,String>)msg.obj;
                        try {
                            mApiChecker.login(getPoster(),map.get("userIdValue"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (RemoteService.ServiceUnavailableException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    } else{
                        Log.e(LOGTAG, "Unexpected message received by Sugo worker: " + msg);
                    }

                    ///////////////////////////
                    if ((returnCode >= mConfig.getBulkUploadLimit() || returnCode == SugoDbAdapter.DB_OUT_OF_MEMORY_ERROR)
                            && mFailedRetries <= 0) {
                        final String storeInfo = getStoreInfo();
                        if (TextUtils.isEmpty(storeInfo) || storeInfo.equals("[]")) {
                            log("empty dimensions, Flushing do not work !!!");
                        } else {
                            log("Flushing queue due to bulk upload limit");
                            updateFlushFrequency();
                            sendAllData(mDbAdapter);
                        }
                    } else if (returnCode > 0 && !hasMessages(FLUSH_QUEUE)) {
                        // hasMessages(FLUSH_QUEUE) 检查该线程内部已经排队的延迟 flush
                        // 在这个线程之外的调用者仍然可以在这里发送一个 flush
                        // 这样我们就可以在队列中得到两个 flush，但是我们可以接受。
                        long interval = mFlushInterval;
                        log("Queue depth " + returnCode + " - Adding flush in " + interval);
                        if (interval >= 0) {
                            sendEmptyMessageDelayed(FLUSH_QUEUE, interval);
                        }
                    }
                } catch (final RuntimeException e) {
                    Log.e(LOGTAG, "Worker threw an unhandled exception", e);
                    synchronized (mHandlerLock) {
                        mHandler = null;
                        try {
                            Looper.myLooper().quit();
                            Log.e(LOGTAG, "Sugo will not process any more analytics messages", e);
                        } catch (final Exception tooLate) {
                            Log.e(LOGTAG, "Could not halt looper", tooLate);
                        }
                    }
                }
            }// handleMessage

        }
    }

    static class EventDescription {

        private final String eventName;
        private final String eventId;
        private final JSONObject properties;
        private final String token;

        public EventDescription(String eventId, String eventName, JSONObject properties, String token) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.properties = properties;
            this.token = token;
        }

        public String getEventName() {
            return eventName;
        }

        public String getEventId() {
            return eventId;
        }

        public JSONObject getProperties() {
            return properties;
        }

        public String getToken() {
            return token;
        }

    }

    private String getStoreInfo() {
        final String sharedPrefsName = ViewCrawler.SHARED_PREF_EDITS_FILE + SGConfig.getInstance(mContext).getToken();
        SharedPreferences preferences = mContext.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        return preferences.getString(ViewCrawler.SHARED_PREF_DIMENSIONS_KEY, null);
    }

    private void log(String message) {
        if (SGConfig.DEBUG) {
            Log.v(LOGTAG, message + " (Thread " + Thread.currentThread().getId() + ")");
        }
    }

    private void log(String message, Throwable e) {
        if (SGConfig.DEBUG) {
            Log.v(LOGTAG, message + " (Thread " + Thread.currentThread().getId() + ")", e);
        }
    }

}
