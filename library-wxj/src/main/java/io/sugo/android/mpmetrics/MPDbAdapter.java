package io.sugo.android.mpmetrics;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * SQLite database adapter for SugoAPI.
 *
 * <p>Not thread-safe. Instances of this class should only be used
 * by a single thread.
 *
 */
/* package */ class MPDbAdapter {
    private static final String LOGTAG = "SugoAPI.Database";

    public enum Table {
        EVENTS ("events"),
        PEOPLE ("people");

        Table(String name) {
            mTableName = name;
        }

        public String getName() {
            return mTableName;
        }

        private final String mTableName;
    }

    public static final String KEY_DATA = "data";
    public static final String KEY_CREATED_AT = "created_at";

    public static final int DB_UPDATE_ERROR = -1;
    public static final int DB_OUT_OF_MEMORY_ERROR = -2;
    public static final int DB_UNDEFINED_CODE = -3;

    private static final String DATABASE_NAME = "mixpanel";
    private static final int DATABASE_VERSION = 4;

    private static final String CREATE_EVENTS_TABLE =
       "CREATE TABLE " + Table.EVENTS.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        KEY_DATA + " STRING NOT NULL, " +
        KEY_CREATED_AT + " INTEGER NOT NULL);";
    private static final String CREATE_PEOPLE_TABLE =
       "CREATE TABLE " + Table.PEOPLE.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        KEY_DATA + " STRING NOT NULL, " +
        KEY_CREATED_AT + " INTEGER NOT NULL);";
    private static final String EVENTS_TIME_INDEX =
        "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.EVENTS.getName() +
        " (" + KEY_CREATED_AT + ");";
    private static final String PEOPLE_TIME_INDEX =
        "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.PEOPLE.getName() +
        " (" + KEY_CREATED_AT + ");";

    private final MPDatabaseHelper mDb;

    private static class MPDatabaseHelper extends SQLiteOpenHelper {
        MPDatabaseHelper(Context context, String dbName) {
            super(context, dbName, null, DATABASE_VERSION);
            mDatabaseFile = context.getDatabasePath(dbName);
            mConfig = SGConfig.getInstance(context);
        }

        /**
         * Completely deletes the DB file from the file system.
         */
        public void deleteDatabase() {
            close();
            mDatabaseFile.delete();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (SGConfig.DEBUG) {
                Log.v(LOGTAG, "Creating a new Mixpanel events DB");
            }

            db.execSQL(CREATE_EVENTS_TABLE);
            db.execSQL(CREATE_PEOPLE_TABLE);
            db.execSQL(EVENTS_TIME_INDEX);
            db.execSQL(PEOPLE_TIME_INDEX);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (SGConfig.DEBUG) {
                Log.v(LOGTAG, "Upgrading app, replacing Mixpanel events DB");
            }

            db.execSQL("DROP TABLE IF EXISTS " + Table.EVENTS.getName());
            db.execSQL("DROP TABLE IF EXISTS " + Table.PEOPLE.getName());
            db.execSQL(CREATE_EVENTS_TABLE);
            db.execSQL(CREATE_PEOPLE_TABLE);
            db.execSQL(EVENTS_TIME_INDEX);
            db.execSQL(PEOPLE_TIME_INDEX);
        }

        public boolean belowMemThreshold() {
            if (mDatabaseFile.exists()) {
                return Math.max(mDatabaseFile.getUsableSpace(), mConfig.getMinimumDatabaseLimit()) >= mDatabaseFile.length();
            }
            return true;
        }

        public boolean outOfLimit(int count) {

            return count > mConfig.getMaxEventLimit();

        }

        private final File mDatabaseFile;
        private final SGConfig mConfig;
    }

    public MPDbAdapter(Context context) {
        this(context, DATABASE_NAME);
    }

    public MPDbAdapter(Context context, String dbName) {
        mDb = new MPDatabaseHelper(context, dbName);
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     * @param j the JSON to record
     * @param table the table to insert into, either "events" or "people"
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(JSONObject j, Table table) {
        // we are aware of the race condition here, but what can we do..?
        if (!this.belowMemThreshold()) {
            Log.e(LOGTAG, "There is not enough space left on the device to store Mixpanel data, so data was discarded");
            return DB_OUT_OF_MEMORY_ERROR;
        }

        final String tableName = table.getName();

        Cursor c = null;
        int count = DB_UPDATE_ERROR;

        try {
            final SQLiteDatabase db = mDb.getWritableDatabase();
            c = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            c.moveToFirst();
            count = c.getInt(0);

            if (!mDb.outOfLimit(count)){
                final ContentValues cv = new ContentValues();
                cv.put(KEY_DATA, j.toString());
                cv.put(KEY_CREATED_AT, System.currentTimeMillis());
                db.insert(tableName, null, cv);
                count ++;
            } else {
                Log.i(LOGTAG, "events outOfLimit!!");
                return DB_OUT_OF_MEMORY_ERROR;
            }

        } catch (final SQLiteException e) {
            Log.e(LOGTAG, "Could not add Mixpanel data to table " + tableName + ". Re-initializing database.", e);

            // We assume that in general, the results of a SQL exception are
            // unrecoverable, and could be associated with an oversized or
            // otherwise unusable DB. Better to bomb it and get back on track
            // than to leave it junked up (and maybe filling up the disk.)
            if (c != null) {
                c.close();
                c = null;
            }
            mDb.deleteDatabase();
        } finally {
            if (c != null) {
                c.close();
            }
            mDb.close();
        }
        return count;
    }

    /**
     * Removes events with an _id <= last_id from table
     * @param last_id the last id to delete
     * @param table the table to remove events from, either "events" or "people"
     */
    public void cleanupEvents(String last_id, Table table) {
        final String tableName = table.getName();

        try {
            final SQLiteDatabase db = mDb.getWritableDatabase();
            db.delete(tableName, "_id <= " + last_id, null);
        } catch (final SQLiteException e) {
            Log.e(LOGTAG, "Could not clean sent Mixpanel records from " + tableName + ". Re-initializing database.", e);

            // We assume that in general, the results of a SQL exception are
            // unrecoverable, and could be associated with an oversized or
            // otherwise unusable DB. Better to bomb it and get back on track
            // than to leave it junked up (and maybe filling up the disk.)
            mDb.deleteDatabase();
        } finally {
            mDb.close();
        }
    }

    /**
     * Removes events before time.
     * @param time the unix epoch in milliseconds to remove events before
     * @param table the table to remove events from, either "events" or "people"
     */
    public void cleanupEvents(long time, Table table) {
        final String tableName = table.getName();

        try {
            final SQLiteDatabase db = mDb.getWritableDatabase();
            db.delete(tableName, KEY_CREATED_AT + " <= " + time, null);
        } catch (final SQLiteException e) {
            Log.e(LOGTAG, "Could not clean timed-out Mixpanel records from " + tableName + ". Re-initializing database.", e);

            // We assume that in general, the results of a SQL exception are
            // unrecoverable, and could be associated with an oversized or
            // otherwise unusable DB. Better to bomb it and get back on track
            // than to leave it junked up (and maybe filling up the disk.)
            mDb.deleteDatabase();
        } finally {
            mDb.close();
        }
    }

    public void deleteDB() {
        mDb.deleteDatabase();
    }


    /**
     * Returns the data string to send to Mixpanel and the maximum ID of the row that
     * we're sending, so we know what rows to delete when a track request was successful.
     *
     * @param table the table to read the JSON from, either "events" or "people"
     * @return String array containing the maximum ID, the data string
     * representing the events (or null if none could be successfully retrieved) and the total
     * current number of events in the queue.
     */
    public String[] generateDataString(Table table) {
        Cursor c = null;
        Cursor queueCountCursor = null;
        String data = null;
        String last_id = null;
        String queueCount = null;
        final String tableName = table.getName();
        final SQLiteDatabase db = mDb.getReadableDatabase();

        JSONArray wrongEvents = new JSONArray();
        try {
            c = db.rawQuery("SELECT * FROM " + tableName  +
                    " ORDER BY " + KEY_CREATED_AT + " ASC LIMIT 50", null);

            queueCountCursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            queueCountCursor.moveToFirst();
            queueCount = String.valueOf(queueCountCursor.getInt(0));

            final JSONArray arr = new JSONArray();

            while (c.moveToNext()) {
                if (c.isLast()) {
                    last_id = c.getString(c.getColumnIndex("_id"));
                }
                try {
                    final JSONObject j = new JSONObject(c.getString(c.getColumnIndex(KEY_DATA)));
                    arr.put(j);
                } catch (final JSONException e) {
                    // Ignore this object
                }
            }


            int length = arr.length();
            if (length > 0) {
                Map<String, String> dimMap = new HashMap<>();
                Map<String, String> configDimMap = new HashMap<>(SugoDimensionManager.getInstance().getDimensionTypes());
                for (int i = 0; i < length; i++) {
                    JSONObject event = arr.getJSONObject(i);
                    for (final Iterator<?> iter = event.keys(); iter.hasNext(); ) {
                        final String dimName = (String) iter.next();
                        final String dimType = configDimMap.get(dimName);
                        if (!dimMap.containsKey(dimName) && configDimMap.containsKey(dimName)) {
                            dimMap.put(dimName, dimType);
                        }
                    }
                }

                StringBuffer buf = new StringBuffer();
                Set<String> keySet = dimMap.keySet();
                int setSize = keySet.size();
                int count = 0;
                for (String dimName: keySet) {
                    buf.append(dimMap.get(dimName)).append("|").append(dimName);
                    if(count < setSize - 1) {
                        buf.append(",");
                    }
                    count ++;
                }
                buf.append('\002');
                for (int i = 0; i < length; i++) {
                    JSONObject event = arr.getJSONObject(i);
                    int dimCount = 0;
                    boolean isWrongEvent = false;
                    for (String dimName : keySet) {
                        final String dimType = dimMap.get(dimName);
                        if (event.has(dimName)) {
                            Object value = event.get(dimName);
                            switch (dimType) {
                                case "l":
                                    if (!(value instanceof Long)) {
                                        try {
                                            value = Long.parseLong(value.toString());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            value = 0;
                                            isWrongEvent = true;
                                        }
                                    }
                                    break;
                                case "s":
                                    if (!(value instanceof String)) {
                                        try {
                                            value = value.toString();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            value = "";
                                        }
                                    }
                                    if (value.toString().length() > 100){
                                        value = ((String) value).substring(0,100);
                                    }
                                    break;
                                case "f":
                                    if (!(value instanceof Float)) {
                                        try {
                                            value = Float.parseFloat(value.toString());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            value = 0;
                                            isWrongEvent = true;
                                        }
                                    }
                                    break;
                                case "d":
                                    if (!(value instanceof Long)) {
                                        try {
                                            value = Long.parseLong(value.toString());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            value = 0;
                                            isWrongEvent = true;
                                        }
                                    }
                                    break;
                                case "i":
                                    if (!(value instanceof Integer)) {
                                        try {
                                            value = Integer.parseInt(value.toString());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            value = 0;
                                            isWrongEvent = true;
                                        }
                                    }
                                    break;
                                default:
                                    break;
                            }
                            buf.append(value);
                        } else {
                            switch (dimType) {
                                case "s":
                                    buf.append("");
                                    break;
                                default:
                                    buf.append(0);
                            }
                        }
                        if (dimCount < setSize - 1) {
                            buf.append('\001');
                        }
                        dimCount++;
                    }
                    if (isWrongEvent){
                        wrongEvents.put(event);
                    }
                    buf.append('\002');
                }
                data = buf.toString();
            }
        } catch (final SQLiteException e) {
            Log.e(LOGTAG, "Could not pull records for Mixpanel out of database " + tableName + ". Waiting to send.", e);

            // We'll dump the DB on write failures, but with reads we can
            // let things ride in hopes the issue clears up.
            // (A bit more likely, since we're opening the DB for read and not write.)
            // A corrupted or disk-full DB will be cleaned up on the next write or clear call.
            last_id = null;
            data = null;
        } catch (JSONException e){
            Log.e(LOGTAG, "", e);
        }finally {
            mDb.close();
            if (c != null) {
                c.close();
            }
            if (queueCountCursor != null) {
                queueCountCursor.close();
            }
        }

        if (last_id != null && data != null) {
            final String[] ret = {last_id, data, queueCount, wrongEvents.toString()};
            return ret;
        }
        return null;
    }

    /* For testing use only, do not call from in production code */
    protected boolean belowMemThreshold() {
        return mDb.belowMemThreshold();
    }
}
