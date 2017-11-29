package io.sugo.android.mpmetrics;

import org.json.JSONArray;

import io.sugo.android.viewcrawler.UpdatesFromSugo;

// Will be called from both customer threads and the Sugo worker thread.
class ApiMessages {

    @SuppressWarnings("unused")
    private static final String LOGTAG = "SugoAPI.DecideUpdts";

    // Mutable, must be synchronized
    private String mDistinctId;

    private final String mToken;
    private final OnNewResultsListener mListener;
    private final UpdatesFromSugo mUpdatesFromSugo;

    public interface OnNewResultsListener {
        void onNewResults();
    }

    public ApiMessages(String token, OnNewResultsListener listener, UpdatesFromSugo updatesFromSugo) {
        mToken = token;
        mListener = listener;
        mUpdatesFromSugo = updatesFromSugo;

        mDistinctId = null;
    }

    public String getToken() {
        return mToken;
    }

    // Called from other synchronized code. Do not call into other synchronized code or you'll
    // risk deadlock
    public synchronized void setDistinctId(String distinctId) {
        mDistinctId = distinctId;
    }

    public synchronized String getDistinctId() {
        return mDistinctId;
    }

    public synchronized void reportResults(JSONArray eventBindings,
                                           JSONArray h5EventBindings,
                                           JSONArray pageInfos,
                                           JSONArray dimensions) {
        boolean newContent = false;
        mUpdatesFromSugo.setEventBindings(eventBindings);
        mUpdatesFromSugo.setH5EventBindings(h5EventBindings);
        mUpdatesFromSugo.setPageInfos(pageInfos);
        mUpdatesFromSugo.setDimensions(dimensions);

        if (newContent && null != mListener) {
            mListener.onNewResults();
        }
    }

}
