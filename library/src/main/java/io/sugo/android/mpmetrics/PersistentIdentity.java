package io.sugo.android.mpmetrics;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

// In order to use writeEdits, we have to suppress the linter's check for commit()/apply()
@SuppressLint("CommitPrefEdits")
class PersistentIdentity {

    private static final String LOGTAG = "SugoAPI.PIdentity";

    private final Future<SharedPreferences> mLoadStoredPreferences;
    private final Future<SharedPreferences> mTimeEventsPreferences;
    private final Future<SharedPreferences> mSugoPreferences;
    private JSONObject mSuperPropertiesCache;
    private boolean mIdentitiesLoaded;
    private String mEventsDistinctId;
    private boolean mTrackedIntegration;

    PersistentIdentity(
            Future<SharedPreferences> storedPreferences,
            Future<SharedPreferences> timeEventsPreferences,
            Future<SharedPreferences> sugoPreferences) {
        mLoadStoredPreferences = storedPreferences;
        mTimeEventsPreferences = timeEventsPreferences;
        mSugoPreferences = sugoPreferences;
        mSuperPropertiesCache = null;
        mIdentitiesLoaded = false;
    }

    void writeUserLoginTime(String userId, long time) {
        SharedPreferences preferences = null;
        try {
            preferences = mSugoPreferences.get();
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putLong("first_login_time_" + userId, time);
            editor.commit();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    long readUserLoginTime(String userId) {
        SharedPreferences preferences = null;
        try {
            preferences = mSugoPreferences.get();
            return preferences.getLong("first_login_time_" + userId, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return 0;
    }


    void writeUserIdKey(String userIdKey) {
        SharedPreferences preferences = null;
        try {
            preferences = mSugoPreferences.get();
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString("sugo_user_id_key", userIdKey);
            editor.commit();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    String readUserIdKey() {
        SharedPreferences preferences = null;
        try {
            preferences = mSugoPreferences.get();
            return preferences.getString("sugo_user_id_key", null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void addSuperPropertiesToObject(JSONObject ob) {
        final JSONObject superProperties = this.getSuperPropertiesCache();
        final Iterator<?> superIter = superProperties.keys();
        while (superIter.hasNext()) {
            final String key = (String) superIter.next();

            try {
                ob.put(key, superProperties.get(key));
            } catch (JSONException e) {
                Log.wtf(LOGTAG, "Object read from one JSON Object cannot be written to another", e);
            }
        }
    }

    public synchronized void updateSuperProperties(SuperPropertyUpdate updates) {
        final JSONObject oldPropCache = getSuperPropertiesCache();
        final JSONObject copy = new JSONObject();

        try {
            final Iterator<String> keys = oldPropCache.keys();
            while (keys.hasNext()) {
                final String k = keys.next();
                final Object v = oldPropCache.get(k);
                copy.put(k, v);
            }
        } catch (JSONException e) {
            Log.wtf(LOGTAG, "Can't copy from one JSONObject to another", e);
            return;
        }

        final JSONObject replacementCache = updates.update(copy);
        if (null == replacementCache) {
            Log.w(LOGTAG, "An update to Sugo's super properties returned null, and will have no effect.");
            return;
        }

        mSuperPropertiesCache = replacementCache;
        storeSuperProperties();
    }

    public synchronized String getEventsDistinctId() {
        if (!mIdentitiesLoaded) {
            readIdentities();
        }
        return mEventsDistinctId;
    }

    public synchronized void setEventsDistinctId(String eventsDistinctId) {
        if (!mIdentitiesLoaded) {
            readIdentities();
        }
        mEventsDistinctId = eventsDistinctId;
        writeIdentities();
    }

    public synchronized boolean hasTrackedIntegration() {
        if (!mIdentitiesLoaded) {
            readIdentities();
        }
        return mTrackedIntegration;
    }

    public synchronized void setTrackedIntegration(boolean trackedIntegration) {
        if (!mIdentitiesLoaded) {
            readIdentities();
        }
        mTrackedIntegration = trackedIntegration;
        writeIdentities();
    }

    public synchronized void clearPreferences() {
        // Will clear distinct_ids, superProperties,
        // and waiting People Analytics properties. Will have no effect
        // on messages already queued to send with AnalyticsMessages.

        try {
            final SharedPreferences prefs = mLoadStoredPreferences.get();
            final SharedPreferences.Editor prefsEdit = prefs.edit();
            prefsEdit.clear();
            writeEdits(prefsEdit);
            readSuperProperties();
            readIdentities();
        } catch (final ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (final InterruptedException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public synchronized void registerSuperProperties(JSONObject superProperties) {
        final JSONObject propCache = getSuperPropertiesCache();

        for (final Iterator<?> iter = superProperties.keys(); iter.hasNext(); ) {
            final String key = (String) iter.next();
            try {
                propCache.put(key, superProperties.get(key));
            } catch (final JSONException e) {
                Log.e(LOGTAG, "Exception registering super property.", e);
            }
        }

        storeSuperProperties();
    }

    public synchronized void unregisterSuperProperty(String superPropertyName) {
        final JSONObject propCache = getSuperPropertiesCache();
        propCache.remove(superPropertyName);

        storeSuperProperties();
    }

    public Map<String, Long> getTimeEvents() {
        Map<String, Long> timeEvents = new HashMap<>();

        try {
            final SharedPreferences prefs = mTimeEventsPreferences.get();

            Map<String, ?> allEntries = prefs.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                timeEvents.put(entry.getKey(), Long.valueOf(entry.getValue().toString()));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return timeEvents;
    }

    // access is synchronized outside (mEventTimings)
    public void removeTimeEvent(String timeEventName) {
        try {
            final SharedPreferences prefs = mTimeEventsPreferences.get();
            final SharedPreferences.Editor editor = prefs.edit();
            editor.remove(timeEventName);
            writeEdits(editor);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    // access is synchronized outside (mEventTimings)
    public void addTimeEvent(String timeEventName, Long timeEventTimestamp) {
        try {
            final SharedPreferences prefs = mTimeEventsPreferences.get();
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(timeEventName, timeEventTimestamp);
            writeEdits(editor);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public synchronized void registerSuperPropertiesOnce(JSONObject superProperties) {
        final JSONObject propCache = getSuperPropertiesCache();

        for (final Iterator<?> iter = superProperties.keys(); iter.hasNext(); ) {
            final String key = (String) iter.next();
            if (!propCache.has(key)) {
                try {
                    propCache.put(key, superProperties.get(key));
                } catch (final JSONException e) {
                    Log.e(LOGTAG, "Exception registering super property.", e);
                }
            }
        }// for

        storeSuperProperties();
    }

    public synchronized void clearSuperProperties() {
        mSuperPropertiesCache = new JSONObject();
        storeSuperProperties();
    }

    //////////////////////////////////////////////////

    // Must be called from a synchronized setting
    private JSONObject getSuperPropertiesCache() {
        if (null == mSuperPropertiesCache) {
            readSuperProperties();
        }
        return mSuperPropertiesCache;
    }

    // All access should be synchronized on this
    private void readSuperProperties() {
        try {
            final SharedPreferences prefs = mLoadStoredPreferences.get();
            final String props = prefs.getString("super_properties", "{}");
            if (SGConfig.DEBUG) {
                Log.v(LOGTAG, "Loading Super Properties " + props);
            }
            mSuperPropertiesCache = new JSONObject(props);
        } catch (final ExecutionException e) {
            Log.e(LOGTAG, "Cannot load superProperties from SharedPreferences.", e.getCause());
        } catch (final InterruptedException e) {
            Log.e(LOGTAG, "Cannot load superProperties from SharedPreferences.", e);
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Cannot parse stored superProperties");
            storeSuperProperties();
        } finally {
            if (null == mSuperPropertiesCache) {
                mSuperPropertiesCache = new JSONObject();
            }
        }
    }

    // All access should be synchronized on this
    private void storeSuperProperties() {
        if (null == mSuperPropertiesCache) {
            Log.e(LOGTAG, "storeSuperProperties should not be called with uninitialized superPropertiesCache.");
            return;
        }

        final String props = mSuperPropertiesCache.toString();
        if (SGConfig.DEBUG) {
            Log.v(LOGTAG, "Storing Super Properties " + props);
        }

        try {
            final SharedPreferences prefs = mLoadStoredPreferences.get();
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString("super_properties", props);
            writeEdits(editor);
        } catch (final ExecutionException e) {
            Log.e(LOGTAG, "Cannot store superProperties in shared preferences.", e.getCause());
        } catch (final InterruptedException e) {
            Log.e(LOGTAG, "Cannot store superProperties in shared preferences.", e);
        }
    }

    // All access should be synchronized on this
    private void readIdentities() {
        SharedPreferences prefs = null;
        try {
            prefs = mLoadStoredPreferences.get();
        } catch (final ExecutionException e) {
            Log.e(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e.getCause());
        } catch (final InterruptedException e) {
            Log.e(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e);
        }

        if (null == prefs) {
            return;
        }

        mEventsDistinctId = prefs.getString("events_distinct_id", null);
        mTrackedIntegration = prefs.getBoolean("tracked_integration", false);

        if (null == mEventsDistinctId) {
            mEventsDistinctId = UUID.randomUUID().toString();
            writeIdentities();
        }

        mIdentitiesLoaded = true;
    }

    // All access should be synchronized on this
    private void writeIdentities() {
        try {
            final SharedPreferences prefs = mLoadStoredPreferences.get();
            final SharedPreferences.Editor prefsEditor = prefs.edit();

            prefsEditor.putString("events_distinct_id", mEventsDistinctId);
            prefsEditor.putBoolean("tracked_integration", mTrackedIntegration);
            writeEdits(prefsEditor);
        } catch (final ExecutionException e) {
            Log.e(LOGTAG, "Can't write distinct ids to shared preferences.", e.getCause());
        } catch (final InterruptedException e) {
            Log.e(LOGTAG, "Can't write distinct ids to shared preferences.", e);
        }
    }

    private static void writeEdits(final SharedPreferences.Editor editor) {
        editor.apply();
    }

}
