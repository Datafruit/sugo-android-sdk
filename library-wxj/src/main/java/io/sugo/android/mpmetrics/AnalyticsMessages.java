package io.sugo.android.mpmetrics;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
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
import io.sugo.android.util.ExceptionInfoUtils;
import io.sugo.android.util.HttpService;
import io.sugo.android.util.RemoteService;
import io.sugo.android.viewcrawler.ViewCrawler;

/**
 * Manage communication of events with the internal database and the Mixpanel servers.
 * <p>
 * <p>This class straddles the thread boundary between user threads and
 * a logical Mixpanel thread.
 */
/* package */ class AnalyticsMessages {

    /**
     * Do not call directly. You should call AnalyticsMessages.getInstance()
     */
    /* package */ AnalyticsMessages(final Context context) {
        mContext = context;
        mConfig = getConfig(context);
        mSystemInformation = new SystemInformation(mContext);
        mWorker = createWorker();
        getPoster().checkIsMixpanelBlocked();
    }

    protected Worker createWorker() {
        return new Worker();
    }

    /**
     * Use this to get an instance of AnalyticsMessages instead of creating one directly
     * for yourself.
     *
     * @param messageContext should be the Main Activity of the application
     *                       associated with these messages.
     */
    public static AnalyticsMessages getInstance(final Context messageContext) {
        synchronized (sInstances) {
            final Context appContext = messageContext.getApplicationContext();
            AnalyticsMessages ret;
            if (!sInstances.containsKey(appContext)) {
                ret = new AnalyticsMessages(appContext);
                sInstances.put(appContext, ret);
            } else {
                ret = sInstances.get(appContext);
            }
            return ret;
        }
    }

    public void eventsMessage(final EventDescription eventDescription) {
        final Message m = Message.obtain();
        m.what = ENQUEUE_EVENTS;
        m.obj = eventDescription;
        mWorker.runMessage(m);
    }

    // Must be thread safe.
    public void peopleMessage(final JSONObject peopleJson) {
        final Message m = Message.obtain();
        m.what = ENQUEUE_PEOPLE;
        m.obj = peopleJson;

        mWorker.runMessage(m);
    }

    public void postToServer() {
        final Message m = Message.obtain();
        m.what = FLUSH_QUEUE;

        mWorker.runMessage(m);
    }

    public void installDecideCheck(final DecideMessages check) {
        final Message m = Message.obtain();
        m.what = INSTALL_DECIDE_CHECK;
        m.obj = check;

        mWorker.runMessage(m);
    }

    public void registerForGCM(final String senderID) {
        final Message m = Message.obtain();
        m.what = REGISTER_FOR_GCM;
        m.obj = senderID;

        mWorker.runMessage(m);
    }

    public void hardKill() {
        final Message m = Message.obtain();
        m.what = KILL_WORKER;

        mWorker.runMessage(m);
    }

    public void sendDataForInitSugo(Exception exception){
        try {
            JSONObject props = ExceptionInfoUtils.ExceptionInfo2(mContext,exception);
            final RemoteService poster = getPoster();
            String url = mConfig.getExceptionTopicEndpoint();
            final String encodedData = Base64Coder.encodeString(props.toString());
            final SSLSocketFactory socketFactory = mConfig.getSSLSocketFactory();
            byte[] response;
            response = poster.performRawRequest(url, encodedData, socketFactory);
            String parsedResponse = new String(response, "UTF-8");
            Log.d("SUGO_TAG", "sendDataForInitSugo: "+parsedResponse);
        }catch (Exception e){
            Log.e("SUGO_TAG", "sendDataForInitSugo: " + e);
        }
    }

    /////////////////////////////////////////////////////////
    // For testing, to allow for Mocking.

    /* package */ boolean isDead() {
        return mWorker.isDead();
    }

    protected MPDbAdapter makeDbAdapter(Context context) {
        return new MPDbAdapter(context);
    }

    protected SGConfig getConfig(Context context) {
        return SGConfig.getInstance(context);
    }

    protected RemoteService getPoster() {
        return new HttpService();
    }

    ////////////////////////////////////////////////////

    static class EventDescription {
        public EventDescription(String eventId, String eventName, JSONObject properties, String token) {
            this.eventName = eventName;
            this.eventId = eventId;
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

        private final String eventName;
        private final String eventId;
        private final JSONObject properties;
        private final String token;
    }

    // Sends a message if and only if we are running with Mixpanel Message log enabled.
    // Will be called from the Mixpanel thread.
    private void logAboutMessageToMixpanel(String message) {
        if (SGConfig.DEBUG) {
            Log.v(LOGTAG, message + " (Thread " + Thread.currentThread().getId() + ")");
        }
    }

    private void logAboutMessageToMixpanel(String message, Throwable e) {
        if (SGConfig.DEBUG) {
            Log.v(LOGTAG, message + " (Thread " + Thread.currentThread().getId() + ")", e);
        }
    }

    public JSONObject getDefaultEventProperties()
            throws JSONException {
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
        ret.put(SGConfig.FIELD_SCREEN_HEIGHT, displayMetrics.heightPixels);
        ret.put(SGConfig.FIELD_SCREEN_WIDTH, displayMetrics.widthPixels);

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

    // Worker will manage the (at most single) IO thread associated with
    // this AnalyticsMessages instance.
    // XXX: Worker class is unnecessary, should be just a subclass of HandlerThread
    class Worker {
        public Worker() {
            mHandler = restartWorkerThread();
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
                    logAboutMessageToMixpanel("Dead mixpanel worker dropping a message: " + msg.what);
                } else {
                    mHandler.sendMessage(msg);
                }
            }
        }

        // NOTE that the returned worker will run FOREVER, unless you send a hard kill
        // (which you really shouldn't)
        protected Handler restartWorkerThread() {
            final HandlerThread thread = new HandlerThread("com.mixpanel.android.AnalyticsWorker", Thread.MIN_PRIORITY);
            thread.start();
            final Handler ret = new AnalyticsMessageHandler(thread.getLooper());
            return ret;
        }

        class AnalyticsMessageHandler extends Handler {
            public AnalyticsMessageHandler(Looper looper) {
                super(looper);
                mDbAdapter = null;
                mDecideChecker = createDecideChecker();
                mDisableFallback = mConfig.getDisableFallback();
                mFlushInterval = mConfig.getFlushInterval();
                mUpdateDecideInterval = mConfig.getUpdateDecideInterval();
            }

            protected DecideChecker createDecideChecker() {
                return new DecideChecker(mContext, mConfig, mSystemInformation);
            }

            @Override
            public void handleMessage(Message msg) {
                if (mDbAdapter == null) {
                    mDbAdapter = makeDbAdapter(mContext);
                    mDbAdapter.cleanupEvents(System.currentTimeMillis() - mConfig.getDataExpiration(), MPDbAdapter.Table.EVENTS);
                    mDbAdapter.cleanupEvents(System.currentTimeMillis() - mConfig.getDataExpiration(), MPDbAdapter.Table.PEOPLE);
                }

                try {
                    int returnCode = MPDbAdapter.DB_UNDEFINED_CODE;

                    if (msg.what == ENQUEUE_PEOPLE) {
                        final JSONObject message = (JSONObject) msg.obj;

                        logAboutMessageToMixpanel("Queuing people record for sending later");
                        logAboutMessageToMixpanel("    " + message.toString());

                        returnCode = mDbAdapter.addJSON(message, MPDbAdapter.Table.PEOPLE);
                    } else if (msg.what == ENQUEUE_EVENTS) {
                        final EventDescription eventDescription = (EventDescription) msg.obj;
                        try {
                            final JSONObject message = prepareEventObject(eventDescription);
                            logAboutMessageToMixpanel("Queuing event for sending later");
                            logAboutMessageToMixpanel("    " + message.toString());
                            returnCode = mDbAdapter.addJSON(message, MPDbAdapter.Table.EVENTS);
                        } catch (final JSONException e) {
                            SugoAPI.getInstance(mContext).track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(mContext,e));
                            Log.e(LOGTAG, "Exception tracking event " + eventDescription.getEventName(), e);
                        }
                    } else if (msg.what == FLUSH_QUEUE) {
                        // 没有 dimensions 配置，则不发送数据
                        final String sharedPrefsName = ViewCrawler.SHARED_PREF_EDITS_FILE + SGConfig.getInstance(mContext).getToken();
                        SharedPreferences preferences = mContext.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
                        final String storeInfo = preferences.getString(ViewCrawler.SHARED_PREF_DIMENSIONS_KEY, null);
                        if (storeInfo == null || storeInfo.equals("") || storeInfo.equals("[]")) {
                            logAboutMessageToMixpanel("empty dimensions, flush stop !!!");
                        } else {
                            logAboutMessageToMixpanel("Flushing queue due to scheduled or forced flush");
                            updateFlushFrequency();
                            sendAllData(mDbAdapter);
                        }
                    } else if (msg.what == INSTALL_DECIDE_CHECK) {
                        logAboutMessageToMixpanel("Installing a check for surveys and in-app notifications");
                        final DecideMessages check = (DecideMessages) msg.obj;
                        mDecideChecker.addDecideCheck(check);
                        if (SystemClock.elapsedRealtime() >= mDecideRetryAfter) {
                            try {
                                mDecideChecker.runDecideChecks(getPoster());
                            } catch (RemoteService.ServiceUnavailableException e) {
                                SugoAPI.getInstance(mContext).track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(mContext,e));
                                mDecideRetryAfter = SystemClock.elapsedRealtime() + e.getRetryAfter() * 1000;
                            }
                            if (mUpdateDecideInterval > 0) {     // 不允许低于 1s 的值
                                if (mUpdateDecideInterval < 1000) {
                                    mUpdateDecideInterval = 1000;
                                }
                                sendEmptyMessageDelayed(UPDATE_DECIDE_CHECK, mUpdateDecideInterval);
                            }
                        }
                    } else if (msg.what == UPDATE_DECIDE_CHECK) {
                        if (SystemClock.elapsedRealtime() >= mDecideRetryAfter) {
                            try {
                                if (!SugoAPI.developmentMode) {
                                    mDecideChecker.runDecideChecks(getPoster());
                                }
                            } catch (RemoteService.ServiceUnavailableException e) {
                                SugoAPI.getInstance(mContext).track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(mContext,e));
                                mDecideRetryAfter = SystemClock.elapsedRealtime() + e.getRetryAfter() * 1000;
                            }
                            if (mUpdateDecideInterval > 0) {     // 不允许低于 1s 的值
                                if (mUpdateDecideInterval < 1000) {
                                    mUpdateDecideInterval = 1000;
                                }
                                sendEmptyMessageDelayed(UPDATE_DECIDE_CHECK, mUpdateDecideInterval);
                            }
                        }
                    } else if (msg.what == KILL_WORKER) {
                        Log.w(LOGTAG, "Worker received a hard kill. Dumping all events and force-killing. Thread id " + Thread.currentThread().getId());
                        synchronized (mHandlerLock) {
                            mDbAdapter.deleteDB();
                            mHandler = null;
                            Looper.myLooper().quit();
                        }
                    } else {
                        Log.e(LOGTAG, "Unexpected message received by Mixpanel worker: " + msg);
                    }

                    ///////////////////////////
                    if ((returnCode >= mConfig.getBulkUploadLimit()
                            || returnCode == MPDbAdapter.DB_OUT_OF_MEMORY_ERROR)
                            && mFailedRetries <= 0) {
                        logAboutMessageToMixpanel("Flushing queue due to bulk upload limit");
                        // 没有 dimensions 配置，则不发送数据
                        final String sharedPrefsName = ViewCrawler.SHARED_PREF_EDITS_FILE + SGConfig.getInstance(mContext).getToken();
                        SharedPreferences preferences = mContext.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
                        final String storeInfo = preferences.getString(ViewCrawler.SHARED_PREF_DIMENSIONS_KEY, null);
                        if (storeInfo == null || storeInfo.equals("") || storeInfo.equals("[]")) {
                            logAboutMessageToMixpanel("empty dimensions, flush stop !!!");
                        } else {
                            updateFlushFrequency();
                            sendAllData(mDbAdapter);
                        }
                    } else if (returnCode > 0 && !hasMessages(FLUSH_QUEUE)) {
                        // The !hasMessages(FLUSH_QUEUE) check is a courtesy for the common case
                        // of delayed flushes already enqueued from inside of this thread.
                        // Callers outside of this thread can still send
                        // a flush right here, so we may end up with two flushes
                        // in our queue, but we're OK with that.
                        long interval = mFlushInterval;
                        if (SugoAPI.developmentMode) {
                            interval = 1000;
                        }
                        logAboutMessageToMixpanel("Queue depth " + returnCode + " - Adding flush in " + interval);
                        if (interval >= 0) {
                            sendEmptyMessageDelayed(FLUSH_QUEUE, interval);
                        }
                    }
                } catch (final RuntimeException e) {
                    SugoAPI.getInstance(mContext).track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(mContext,e));
                    Log.e(LOGTAG, "Worker threw an unhandled exception", e);
                    synchronized (mHandlerLock) {
                        mHandler = null;
                        try {
                            Looper.myLooper().quit();
                            Log.e(LOGTAG, "Mixpanel will not process any more analytics messages", e);
                        } catch (final Exception tooLate) {
                            SugoAPI.getInstance(mContext).track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(mContext,e));
                            Log.e(LOGTAG, "Could not halt looper", tooLate);
                        }
                    }
                }
            }// handleMessage

            protected long getTrackEngageRetryAfter() {
                return mTrackEngageRetryAfter;
            }


            private void sendAllData(MPDbAdapter dbAdapter) {
                final RemoteService poster = getPoster();
                if (!poster.isOnline(mContext, mConfig.getOfflineMode())) {
                    logAboutMessageToMixpanel("Not flushing data to Mixpanel because the device is not connected to the internet.");
                    return;
                }

                if (mDisableFallback) {
                    sendData(dbAdapter, MPDbAdapter.Table.EVENTS, new String[]{mConfig.getEventsEndpoint()});
                    sendData(dbAdapter, MPDbAdapter.Table.PEOPLE, new String[]{mConfig.getPeopleEndpoint()});
                } else {
                    sendData(dbAdapter, MPDbAdapter.Table.EVENTS,
                            new String[]{mConfig.getEventsEndpoint(), mConfig.getEventsFallbackEndpoint()});
                    sendData(dbAdapter, MPDbAdapter.Table.PEOPLE,
                            new String[]{mConfig.getPeopleEndpoint(), mConfig.getPeopleFallbackEndpoint()});
                }
            }



            private void sendData(MPDbAdapter dbAdapter, MPDbAdapter.Table table, String[] urls) {
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
//                    final Map<String, Object> params = new HashMap<String, Object>();
//                    params.put("data", encodedData);
//                    if (SGConfig.DEBUG) {
//                        params.put("verbose", "1");
//                    }

                    boolean deleteEvents = true;
                    byte[] response;
                    for (String url : urls) {
                        try {
                            final SSLSocketFactory socketFactory = mConfig.getSSLSocketFactory();
                            response = poster.performRawRequest(url, encodedData, socketFactory);
                            if (null == response) {
                                deleteEvents = false;
                                logAboutMessageToMixpanel("Response was null, unexpected failure posting to " + url + ".");
                            } else {
                                deleteEvents = true; // Delete events on any successful post, regardless of 1 or 0 response
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

                                logAboutMessageToMixpanel("Successfully posted to " + url + ": \n" + rawMessage);
                                logAboutMessageToMixpanel("Response was " + parsedResponse);
                            }
                            try {
                                String wrongEvent = eventsData[3];
                                if (!wrongEvent.equals("[]")){
                                    response = poster.performRawRequest(url + "_filter", Base64Coder.encodeString(wrongEvent), socketFactory);
                                }
                            } catch (Exception e){
                                SugoAPI.getInstance(mContext).track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(mContext,e));
                                Log.e(LOGTAG, "Cannot post message to " + url + "_filter" + ".", e);
                            }

                            break;
                        } catch (final OutOfMemoryError e) {
                            Log.e(LOGTAG, "Out of memory when posting to " + url + ".", e);
                            break;
                        } catch (final MalformedURLException e) {
                            SugoAPI.getInstance(mContext).track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(mContext,e));
                            Log.e(LOGTAG, "Cannot interpret " + url + " as a URL.", e);
                            break;
                        } catch (final RemoteService.ServiceUnavailableException e) {
                            SugoAPI.getInstance(mContext).track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(mContext,e));
                            logAboutMessageToMixpanel("Cannot post message to " + url + ".", e);
                            deleteEvents = false;
                            mTrackEngageRetryAfter = e.getRetryAfter() * 1000;
                        } catch (final SocketTimeoutException e) {
                            SugoAPI.getInstance(mContext).track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(mContext,e));
                            logAboutMessageToMixpanel("Cannot post message to " + url + ".", e);
                            deleteEvents = false;
                        } catch (final IOException e) {
                            SugoAPI.getInstance(mContext).track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(mContext,e));
                            logAboutMessageToMixpanel("Cannot post message to " + url + ".", e);
                            deleteEvents = false;
                        }
                    }

                    if (deleteEvents) {
                        logAboutMessageToMixpanel("Not retrying this batch of events, deleting them from DB.");
                        dbAdapter.cleanupEvents(lastId, table);
                    } else {
                        removeMessages(FLUSH_QUEUE);
                        mTrackEngageRetryAfter = Math.max((long) Math.pow(2, mFailedRetries) * 60000, mTrackEngageRetryAfter);
                        mTrackEngageRetryAfter = Math.min(mTrackEngageRetryAfter, 10 * 60 * 1000); // limit 10 min
                        sendEmptyMessageDelayed(FLUSH_QUEUE, mTrackEngageRetryAfter);
                        mFailedRetries++;
                        logAboutMessageToMixpanel("Retrying this batch of events in " + mTrackEngageRetryAfter + " ms");
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

            private MPDbAdapter mDbAdapter;
            private final DecideChecker mDecideChecker;
            private final long mFlushInterval;
            private final boolean mDisableFallback;
            private long mDecideRetryAfter;
            private long mTrackEngageRetryAfter;
            private int mFailedRetries;
        }// AnalyticsMessageHandler

        private void updateFlushFrequency() {
            final long now = System.currentTimeMillis();
            final long newFlushCount = mFlushCount + 1;

            if (mLastFlushTime > 0) {
                final long flushInterval = now - mLastFlushTime;
                final long totalFlushTime = flushInterval + (mAveFlushFrequency * mFlushCount);
                mAveFlushFrequency = totalFlushTime / newFlushCount;

                final long seconds = mAveFlushFrequency / 1000;
                logAboutMessageToMixpanel("Average send frequency approximately " + seconds + " seconds.");
            }

            mLastFlushTime = now;
            mFlushCount = newFlushCount;
        }

        private final Object mHandlerLock = new Object();
        private Handler mHandler;
        private long mFlushCount = 0;
        private long mUpdateDecideInterval = 0;
        private long mAveFlushFrequency = 0;
        private long mLastFlushTime = -1;
    }

    public long getTrackEngageRetryAfter() {
        return ((Worker.AnalyticsMessageHandler) mWorker.mHandler).getTrackEngageRetryAfter();
    }
    /////////////////////////////////////////////////////////

    // Used across thread boundaries
    private final Worker mWorker;
    protected final Context mContext;
    protected final SGConfig mConfig;
    private SystemInformation mSystemInformation;

    // Messages for our thread
    private static final int ENQUEUE_PEOPLE = 0; // submit events and people data
    private static final int ENQUEUE_EVENTS = 1; // push given JSON message to people DB
    private static final int FLUSH_QUEUE = 2; // push given JSON message to events DB
    private static final int KILL_WORKER = 5; // Hard-kill the worker thread, discarding all events on the event queue. This is for testing, or disasters.
    private static final int INSTALL_DECIDE_CHECK = 12; // Run this DecideCheck at intervals until it isDestroyed()
    private static final int UPDATE_DECIDE_CHECK = 14; // Run this DecideCheck at intervals until it isDestroyed()
    private static final int REGISTER_FOR_GCM = 13; // Register for GCM using Google Play Services

    private static final String LOGTAG = "SugoAPI.Messages";

    private static final Map<Context, AnalyticsMessages> sInstances = new HashMap<Context, AnalyticsMessages>();


}
