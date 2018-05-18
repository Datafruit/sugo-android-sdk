package io.sugo.android.metrics;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

import javax.net.ssl.SSLSocketFactory;

import io.sugo.android.util.RemoteService;
import io.sugo.android.viewcrawler.UpdatesFromSugo;
import io.sugo.android.viewcrawler.ViewCrawler;

class ApiChecker {

    private static final String LOGTAG = "SugoAPI.ApiChecker";

    private final SGConfig mConfig;
    private final Context mContext;
    private final SystemInformation mSystemInformation;
    private final UpdatesFromSugo mUpdatesFromSugo;

    private static final JSONArray EMPTY_JSON_ARRAY = new JSONArray();


    static class Result {
        public Result() {
            eventBindings = EMPTY_JSON_ARRAY;
            h5EventBindings = EMPTY_JSON_ARRAY;
            pageInfo = EMPTY_JSON_ARRAY;
            dimensions = EMPTY_JSON_ARRAY;
        }

        public JSONArray eventBindings;
        public JSONArray h5EventBindings;
        public JSONArray pageInfo;
        public JSONArray dimensions;
    }

    public ApiChecker(final Context context, final SGConfig config,
                      final SystemInformation systemInformation, final UpdatesFromSugo updatesFromSugo) {
        mContext = context;
        mConfig = config;
        mSystemInformation = systemInformation;
        mUpdatesFromSugo = updatesFromSugo;
    }

    public void runDecideChecks(final RemoteService poster) throws RemoteService.ServiceUnavailableException {
        final String token = mConfig.getToken();
        final String distinctId = mConfig.getDistinctId();
        try {
            final Result result = runApiRequest(token, distinctId, poster);
            if (result != null) {
                reportResults(result.eventBindings, result.h5EventBindings,
                        result.pageInfo, result.dimensions);
            }
        } catch (final UnintelligibleMessageException e) {
            Log.e(LOGTAG, e.getMessage(), e);
        }
    }

    public synchronized void reportResults(JSONArray eventBindings,
                                           JSONArray h5EventBindings,
                                           JSONArray pageInfos,
                                           JSONArray dimensions) {
        mUpdatesFromSugo.setEventBindings(eventBindings);
        mUpdatesFromSugo.setH5EventBindings(h5EventBindings);
        mUpdatesFromSugo.setPageInfos(pageInfos);
        mUpdatesFromSugo.setDimensions(dimensions);

    }

    static class UnintelligibleMessageException extends Exception {
        private static final long serialVersionUID = -6501269367559104957L;

        public UnintelligibleMessageException(String message, JSONException cause) {
            super(message, cause);
        }
    }

    private Result runApiRequest(final String token, final String distinctId, final RemoteService poster)
            throws RemoteService.ServiceUnavailableException, UnintelligibleMessageException {
        final String responseString = getApiResponseFromServer(token, distinctId, poster);
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
    }// runApiRequest

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
        return ret;
    }

    private String getApiResponseFromServer(@NonNull String unescapedToken, String unescapedDistinctId, RemoteService poster)
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
            urls = new String[]{mConfig.getDecideEndpoint() + checkQuery};
        } else {
            urls = new String[]{mConfig.getDecideEndpoint() + checkQuery,
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

}
