package io.sugo.android.viewcrawler;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import io.sugo.android.metrics.SugoAPI;

import static io.sugo.android.viewcrawler.ViewCrawler.SHARED_PREF_EDITS_FILE;

/**
 *
 * @author Ouwenjie
 * @date 2017/5/11
 */

public class SugoHeatMap {

    public static final int sColdColor = 0x34ffffff;
    public static final int sHotColor = 0x34ff2d51;

    private static boolean showHeatMap = false;
    private static HashMap<String, Integer> sEventCount = new HashMap<>();      // eventId:int
    private static HashMap<String, Integer> sPageMaxCount = new HashMap<>();    // page:int
    private static HashMap<String, String> sEventPage = new HashMap<>();        // eventId:page

    private static HashMap<String, Integer> sEventHeat = new HashMap<>();

    public static boolean isShowHeatMap() {
        return showHeatMap;
    }

    public static void setShowHeatMap(boolean showHeatMap) {
        SugoHeatMap.showHeatMap = showHeatMap;
    }

    public static void setHeatMapData(Context context, String heatMapData) {
        sEventCount.clear();
        sPageMaxCount.clear();
        sEventPage.clear();
        sEventHeat.clear();
        try {
            JSONObject object = new JSONObject(heatMapData).getJSONObject("heat_map");
            Iterator<String> ids = object.keys();
            String eventId;
            int count = 0;
            while (ids.hasNext()) {
                eventId = ids.next();
                count = object.getInt(eventId);
                sEventCount.put(eventId, count);
            }

            final String sharedPrefsName = SHARED_PREF_EDITS_FILE + SugoAPI.getInstance(context).getConfig().getToken();
            SharedPreferences sp = context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
            String eventBinding = sp.getString(ViewCrawler.SHARED_PREF_BINDINGS_KEY, "[]");
            JSONArray events = new JSONArray(eventBinding);
            JSONObject event;
            String page;
            String pageEventId;
            Integer maxCountObj;
            int maxCount;
            Integer eventCountObj;
            int eventCount;
            for (int i = 0; i < events.length(); i++) {
                event = events.getJSONObject(i);
                page = event.getString("target_activity");
                pageEventId = event.getString("event_id");

                maxCountObj = sPageMaxCount.get(page);
                if (maxCountObj == null) {
                    maxCountObj = 0;
                }
                maxCount = maxCountObj;
                eventCountObj = sEventCount.get(pageEventId);
                if (eventCountObj == null) {
                    eventCountObj = 0;
                }
                eventCount = eventCountObj;
                if (eventCount > maxCount) {
                    sPageMaxCount.put(page, eventCount);
                }
                sEventPage.put(pageEventId, page);
            }
            String h5EventBinding = sp.getString(ViewCrawler.SHARED_PREF_H5_BINDINGS_KEY, "[]");
            JSONArray h5Events = new JSONArray(h5EventBinding);
            for (int i = 0; i < h5Events.length(); i++) {
                event = h5Events.getJSONObject(i);
                page = event.getString("target_activity");
                pageEventId = event.getString("event_id");
                maxCountObj = sPageMaxCount.get(page);
                if (maxCountObj == null) {
                    maxCountObj = 0;
                }
                maxCount = maxCountObj;
                eventCountObj = sEventCount.get(pageEventId);
                if (eventCountObj == null) {
                    eventCountObj = 0;
                }
                eventCount = eventCountObj;
                if (eventCount > maxCount) {
                    sPageMaxCount.put(page, eventCount);
                }
                sEventPage.put(pageEventId, page);
            }

            calEventsHeat();

            setShowHeatMap(true);

        } catch (JSONException e) {
            e.printStackTrace();
            sEventCount.clear();
            sPageMaxCount.clear();
            sEventPage.clear();
            sEventHeat.clear();
            setShowHeatMap(false);
        }
    }

    private static void calEventsHeat() {
        int redRange = (sHotColor & 0x00ff0000) - (sColdColor & 0x00ff0000);
        int greenRange = (sHotColor & 0x0000ff00) - (sColdColor & 0x0000ff00);
        int blueRange = (sHotColor & 0x000000ff) - (sColdColor & 0x000000ff);

        String page;

        for (String eventId : sEventCount.keySet()) {
            page = sEventPage.get(eventId);
            int pageMaxCount = sPageMaxCount.get(page);
            int eventCount = sEventCount.get(eventId);

            float countRate = (eventCount * 1.0f) / (pageMaxCount * 1.0f);

            int redOffset = (int) (redRange * countRate);
            int greenOffset = (int) (greenRange * countRate);
            int blueOffset = (int) (blueRange * countRate);

            int eventHeat = sColdColor + redOffset + greenOffset + blueOffset;
            sEventHeat.put(eventId, eventHeat);
        }

    }


    public static int getEventHeat(String eventId) {
        Integer heatColorObj = sEventHeat.get(eventId);
        if (heatColorObj == null) {
            heatColorObj = 0;
        }
        int heatColor = heatColorObj;
        if (heatColor <= 0) {
            return sColdColor;
        }
        return heatColor;
    }


}
