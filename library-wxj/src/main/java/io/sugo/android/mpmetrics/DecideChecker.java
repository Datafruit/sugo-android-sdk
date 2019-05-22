package io.sugo.android.mpmetrics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

import io.sugo.android.util.ImageStore;
import io.sugo.android.util.RemoteService;
import io.sugo.android.viewcrawler.ViewCrawler;

/* package */ class DecideChecker {

    /* package */ static class Result {
        public Result() {
            surveys = new ArrayList<Survey>();
            notifications = new ArrayList<InAppNotification>();
            eventBindings = EMPTY_JSON_ARRAY;
            h5EventBindings = EMPTY_JSON_ARRAY;
            variants = EMPTY_JSON_ARRAY;
            pageInfo = EMPTY_JSON_ARRAY;
            dimensions = EMPTY_JSON_ARRAY;
            positionConfig = -1;

        }

        public final List<Survey> surveys;
        public final List<InAppNotification> notifications;
        public JSONArray eventBindings;
        public JSONArray h5EventBindings;
        public JSONArray variants;
        public JSONArray pageInfo;
        public JSONArray dimensions;
        public int positionConfig;
    }

    public DecideChecker(final Context context, final SGConfig config, final SystemInformation systemInformation) {
        mContext = context;
        mConfig = config;
        mChecks = new LinkedList<DecideMessages>();
        mImageStore = createImageStore(context);
        mSystemInformation = systemInformation;
    }

    protected ImageStore createImageStore(final Context context) {
        return new ImageStore(context, "DecideChecker");
    }

    public void addDecideCheck(final DecideMessages check) {
        mChecks.add(check);
    }

    public void runDecideChecks(final RemoteService poster) throws RemoteService.ServiceUnavailableException {
        final Iterator<DecideMessages> itr = mChecks.iterator();
        while (itr.hasNext()) {
            final DecideMessages updates = itr.next();
            final String token = mConfig.getToken();
            final String distinctId = updates.getDistinctId();
            try {

                final Result eventRes = runEventApiRequest(token, distinctId, poster);
                if (eventRes != null) {
                    updates.reportResults(eventRes.surveys, eventRes.notifications, eventRes.eventBindings,
                            eventRes.h5EventBindings, eventRes.variants, eventRes.pageInfo);
                }
                final Result dimRes = runDimApiRequest(token, distinctId, poster);
                if (dimRes != null) {
                    SGConfig.positionConfig = dimRes.positionConfig;
                    updates.reportDims(dimRes.dimensions);
                }
            } catch (final UnintelligibleMessageException e) {
                Log.e(LOGTAG, e.getMessage(), e);
            }
        }
    }

    /* package */ static class UnintelligibleMessageException extends Exception {
        private static final long serialVersionUID = -6501269367559104957L;

        public UnintelligibleMessageException(String message, JSONException cause) {
            super(message, cause);
        }
    }


    private Result runEventApiRequest(final String token, final String distinctId, final RemoteService poster)
            throws RemoteService.ServiceUnavailableException, UnintelligibleMessageException {
        final String responseString = getEventApiResponseFromServer(token, distinctId, poster);
        if (SGConfig.DEBUG) {
            Log.v(LOGTAG, "Sugo decide server response was:\n" + responseString);
        }

        Result parsed = null;
        if (!TextUtils.isEmpty(responseString)) {
            JSONObject response;
            int newEventBindingVersion;
            try {
                response = new JSONObject(responseString);
                if (response.has("event_bindings_version")) {
                    newEventBindingVersion = response.optInt("event_bindings_version", 0);
                    SharedPreferences preferences = mContext.getSharedPreferences(ViewCrawler.SHARED_PREF_EDITS_FILE + token, Context.MODE_PRIVATE);
                    int oldEventBindingVersion = preferences.getInt(ViewCrawler.SP_EVENT_BINDING_VERSION, -1);
                    String oldEventBindingsAppVersion = preferences.getString(ViewCrawler.SP_EVENT_BINDINGS_APP_VERSION, null);
                    String currentEventBindingsAppVersion = mSystemInformation.getAppVersionName();
                    if ((newEventBindingVersion != oldEventBindingVersion) || !(currentEventBindingsAppVersion.equals(oldEventBindingsAppVersion))) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(ViewCrawler.SP_EVENT_BINDINGS_APP_VERSION, currentEventBindingsAppVersion);
                        editor.putInt(ViewCrawler.SP_EVENT_BINDING_VERSION, newEventBindingVersion);
                        editor.apply();
                    } else {
                        // 配置没有更新内容，不覆盖旧配置
                        return null;
                    }
                }
                parsed = parseApiResponse(responseString);
                return parsed;
            } catch (final JSONException e) {
                final String message = "Sugo endpoint returned unparsable result:\n" + responseString;
                throw new UnintelligibleMessageException(message, e);
            }
        } else {
            // 没有返回配置，不覆盖旧配置
            return null;
        }
    }

    private Result runDimApiRequest(final String token, final String distinctId, final RemoteService poster)
            throws RemoteService.ServiceUnavailableException, UnintelligibleMessageException {
        final String responseString = getDimApiResponseFromServer(token, distinctId, poster);
        if (SGConfig.DEBUG) {
            Log.v(LOGTAG, "Sugo dimensions response was:\n" + responseString);
        }

        Result parsed = null;
        if (!TextUtils.isEmpty(responseString)) {
            JSONObject response;
            long newEventBindingVersion;
            try {
                response = new JSONObject(responseString);
                if (response.has("dimension_version")) {
                    newEventBindingVersion = response.optLong("dimension_version", 0);
                    SharedPreferences preferences = mContext.getSharedPreferences(ViewCrawler.SHARED_PREF_EDITS_FILE + token, Context.MODE_PRIVATE);
                    long oldEventBindingVersion = preferences.getLong(ViewCrawler.SP_DIMENSION_VERSION, -1);
                    if (newEventBindingVersion != oldEventBindingVersion) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putLong(ViewCrawler.SP_DIMENSION_VERSION, newEventBindingVersion);
                        if (response.has("position_config")){
                            int positionConfig= response.optInt("position_config", -1);
                            editor.putInt(ViewCrawler.POSITION_CONFIG, positionConfig);
                        }
                        editor.apply();
                    } else {
                        // 配置没有更新内容，不覆盖旧配置
                        return null;
                    }
                }
                parsed = parseApiResponse(responseString);
                return parsed;
            } catch (final JSONException e) {
                final String message = "Sugo dimensions endpoint returned unparsable result:\n" + responseString;
                throw new UnintelligibleMessageException(message, e);
            }
        } else {
            // 没有返回配置，不覆盖旧配置
            return null;
        }
    }
    static Result parseApiResponse(String responseString) throws UnintelligibleMessageException {
        JSONObject response;
        final Result ret = new Result();

        try {
            response = new JSONObject(responseString);
        } catch (final JSONException e) {
            final String message = "Sugo endpoint returned unparsable result:\n" + responseString;
            throw new UnintelligibleMessageException(message, e);
        }

        if (response.has("event_bindings")) {
            try {
                ret.eventBindings = response.getJSONArray("event_bindings");
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Sugo endpoint returned non-array JSON for event bindings: " + response);
            }
        }

        if (response.has("h5_event_bindings")) {
            try {
                ret.h5EventBindings = response.getJSONArray("h5_event_bindings");
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Sugo endpoint returned non-array JSON for event bindings: " + response);
            }
        }

        if (response.has("page_info")) {
            try {
                ret.pageInfo = response.getJSONArray("page_info");
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Sugo endpoint returned non-array JSON for page_info: " + response);
            }
        }

        if (response.has("dimensions")) {
            try {
                ret.dimensions = response.getJSONArray("dimensions");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(LOGTAG, "Sugo endpoint returned non-array JSON for dimensions: " + response);
            }
        }
        if (response.has("position_config")) {
            try {
                ret.positionConfig = response.getInt("position_config");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(LOGTAG, "Sugo endpoint returned not int  for position_config: " + response);
            }
        }
        return ret;
    }


    public String getSugoInitializeEndpointFromServer(@NonNull String token,@NonNull String projectId,@NonNull String appVersion,RemoteService poster)throws RemoteService.ServiceUnavailableException{
        try {
            token = URLEncoder.encode(token, "utf-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("Sugo library requires utf-8 string encoding to be available", e);
        }
        final StringBuilder queryBuilder = new StringBuilder()
                .append("?tokenId=").append(token)
                .append("&projectId=").append(projectId)
                .append("&appVersion=").append(appVersion);
        final String[] urls = new String[]{mConfig.getSugoInitializeEndpoint() + queryBuilder.toString()};
        final byte[] response = getUrls(poster, mContext, urls);
        if (null == response) {
            return null;
        }
        try {
            return new String(response, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("UTF not supported on this platform?", e);
        }
    }

    private String getEventApiResponseFromServer(@NonNull String unescapedToken, String unescapedDistinctId, RemoteService poster)
            throws RemoteService.ServiceUnavailableException {
        final String escapedToken;
        final String escapedId;
        try {
            escapedToken = URLEncoder.encode(unescapedToken, "utf-8");
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
                .append("&projectId=")
                .append(SGConfig.getInstance(mContext).getProjectId());

        SharedPreferences preferences = mContext.getSharedPreferences(ViewCrawler.SHARED_PREF_EDITS_FILE + unescapedToken, Context.MODE_PRIVATE);
        String oldEventBindingsAppVersion = preferences.getString(ViewCrawler.SP_EVENT_BINDINGS_APP_VERSION, null);
        String currentEventBindingsAppVersion = mSystemInformation.getAppVersionName();
        int oldEventBindingVersion = -1;
        if (currentEventBindingsAppVersion.equals(oldEventBindingsAppVersion)) {
            oldEventBindingVersion = preferences.getInt(ViewCrawler.SP_EVENT_BINDING_VERSION, -1);
        }
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
        if (mConfig.getDisableFallback()) {
            urls = new String[]{mConfig.getEventDecideEndpoint() + checkQuery};
        } else {
            urls = new String[]{mConfig.getEventDecideEndpoint() + checkQuery,
                    mConfig.getDecideFallbackEndpoint() + checkQuery};
        }

        if (SGConfig.DEBUG) {
            Log.v(LOGTAG, "Querying decide server, urls:");
            for (int i = 0; i < urls.length; i++) {
                Log.v(LOGTAG, "    >> " + urls[i]);
            }
        }

        final byte[] response = getUrls(poster, mContext, urls);
        if (null == response) {
            return null;
        }
        try {
            return new String(response, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("UTF not supported on this platform?", e);
        }
    }

    private String getDimApiResponseFromServer(@NonNull String unescapedToken, String unescapedDistinctId, RemoteService poster)
            throws RemoteService.ServiceUnavailableException {
        final String escapedToken;
        final String escapedId;
        try {
            escapedToken = URLEncoder.encode(unescapedToken, "utf-8");
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
                .append("&projectId=")
                .append(SGConfig.getInstance(mContext).getProjectId());

        SharedPreferences preferences = mContext.getSharedPreferences(ViewCrawler.SHARED_PREF_EDITS_FILE + unescapedToken, Context.MODE_PRIVATE);

        long oldDimensionVersion = preferences.getLong(ViewCrawler.SP_DIMENSION_VERSION, -1);
        queryBuilder.append("&dimension_version=").append(oldDimensionVersion);

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
        if (mConfig.getDisableFallback()) {
            urls = new String[]{mConfig.getDimDecideEndpoint() + checkQuery};
        } else {
            urls = new String[]{mConfig.getDimDecideEndpoint() + checkQuery,
                    mConfig.getDecideFallbackEndpoint() + checkQuery};
        }

        if (SGConfig.DEBUG) {
            Log.v(LOGTAG, "Querying dim decide server, urls:");
            for (int i = 0; i < urls.length; i++) {
                Log.v(LOGTAG, "    >> " + urls[i]);
            }
        }

        final byte[] response = getUrls(poster, mContext, urls);
        if (null == response) {
            return null;
        }
        try {
            return new String(response, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("UTF not supported on this platform?", e);
        }
    }


    /* package */
    static Result parseDecideResponse(String responseString)
            throws UnintelligibleMessageException {
        JSONObject response;
        final Result ret = new Result();

        try {
            response = new JSONObject(responseString);
        } catch (final JSONException e) {
            final String message = "Mixpanel endpoint returned unparsable result:\n" + responseString;
            throw new UnintelligibleMessageException(message, e);
        }

        JSONArray surveys = null;
        if (response.has("surveys")) {
            try {
                surveys = response.getJSONArray("surveys");
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Mixpanel endpoint returned non-array JSON for surveys: " + response);
            }
        }

        if (null != surveys) {
            for (int i = 0; i < surveys.length(); i++) {
                try {
                    final JSONObject surveyJson = surveys.getJSONObject(i);
                    final Survey survey = new Survey(surveyJson);
                    ret.surveys.add(survey);
                } catch (final JSONException e) {
                    Log.e(LOGTAG, "Received a strange response from surveys service: " + surveys.toString());
                } catch (final BadDecideObjectException e) {
                    Log.e(LOGTAG, "Received a strange response from surveys service: " + surveys.toString());
                }
            }
        }

        JSONArray notifications = null;
        if (response.has("notifications")) {
            try {
                notifications = response.getJSONArray("notifications");
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Mixpanel endpoint returned non-array JSON for notifications: " + response);
            }
        }

        if (null != notifications) {
            final int notificationsToRead = Math.min(notifications.length(), SGConfig.MAX_NOTIFICATION_CACHE_COUNT);
            for (int i = 0; i < notificationsToRead; i++) {
                try {
                    final JSONObject notificationJson = notifications.getJSONObject(i);
                    final InAppNotification notification = new InAppNotification(notificationJson);
                    ret.notifications.add(notification);
                } catch (final JSONException e) {
                    Log.e(LOGTAG, "Received a strange response from notifications service: " + notifications.toString(), e);
                } catch (final BadDecideObjectException e) {
                    Log.e(LOGTAG, "Received a strange response from notifications service: " + notifications.toString(), e);
                } catch (final OutOfMemoryError e) {
                    Log.e(LOGTAG, "Not enough memory to show load notification from package: " + notifications.toString(), e);
                }
            }
        }

        if (response.has("event_bindings")) {
            try {
                ret.eventBindings = response.getJSONArray("event_bindings");
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Mixpanel endpoint returned non-array JSON for event bindings: " + response);
            }
        }

        if (response.has("h5_event_bindings")) {
            try {
                ret.h5EventBindings = response.getJSONArray("h5_event_bindings");
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Mixpanel endpoint returned non-array JSON for event bindings: " + response);
            }
        }
        if (response.has("variants")) {
            try {
                ret.variants = response.getJSONArray("variants");
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Mixpanel endpoint returned non-array JSON for variants: " + response);
            }
        }

        if (response.has("page_info")) {
            try {
                ret.pageInfo = response.getJSONArray("page_info");
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Mixpanel endpoint returned non-array JSON for page_info: " + response);
            }
        }

        if (response.has("dimensions")) {
            try {
                ret.dimensions = response.getJSONArray("dimensions");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(LOGTAG, "Sugo endpoint returned non-array JSON for dimensions: " + response);
            }
        }

        return ret;
    }


    private Bitmap getNotificationImage(InAppNotification notification, Context context, RemoteService poster)
            throws RemoteService.ServiceUnavailableException {
        String[] urls = {notification.getImage2xUrl(), notification.getImageUrl()};

        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display display = wm.getDefaultDisplay();
        final int displayWidth = getDisplayWidth(display);

        if (notification.getType() == InAppNotification.Type.TAKEOVER && displayWidth >= 720) {
            urls = new String[]{notification.getImage4xUrl(), notification.getImage2xUrl(), notification.getImageUrl()};
        }

        for (String url : urls) {
            try {
                return mImageStore.getImage(url);
            } catch (ImageStore.CantGetImageException e) {
                Log.v(LOGTAG, "Can't load image " + url + " for a notification", e);
            }
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private static int getDisplayWidth(final Display display) {
        if (Build.VERSION.SDK_INT < 13) {
            return display.getWidth();
        } else {
            final Point displaySize = new Point();
            display.getSize(displaySize);
            return displaySize.x;
        }
    }

    private static byte[] getUrls(RemoteService poster, Context context, String[] urls)
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

    private final SGConfig mConfig;
    private final Context mContext;
    private final List<DecideMessages> mChecks;
    private final ImageStore mImageStore;
    private final SystemInformation mSystemInformation;

    private static final JSONArray EMPTY_JSON_ARRAY = new JSONArray();

    private static final String LOGTAG = "SugoAPI.DChecker";
}
