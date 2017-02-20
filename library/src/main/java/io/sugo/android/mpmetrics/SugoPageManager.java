package io.sugo.android.mpmetrics;

import android.app.ActivityManager;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Administrator on 2017/1/22.
 */

public class SugoPageManager {

    private static SugoPageManager sInstance = new SugoPageManager();
    private HashMap<String, JSONObject> mPageInfos;

    private SugoPageManager() {

    }

    public static SugoPageManager getInstance() {
        return sInstance;
    }

    public void setPageInfos(JSONArray pageInfos) {
        if (pageInfos == null) {
            return;
        }
        if (mPageInfos == null) {
            mPageInfos = new HashMap<>();
        } else {
            mPageInfos.clear();
        }
        JSONObject pageObj = null;
        for (int i = 0; i < pageInfos.length(); i++) {
            try {
                pageObj = pageInfos.getJSONObject(i);
                mPageInfos.put(pageObj.optString("page"), pageObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    public String getCurrentPageName(String currentPage) {
        String currentPageName = "";
        JSONObject pageObj = getCurrentPageInfo(currentPage);
        if (pageObj != null) {
            currentPageName = pageObj.optString("page_name", "");
        }
        return currentPageName;
    }

    public JSONObject getCurrentPageInfo(String currentPage) {
        if (mPageInfos != null && (mPageInfos.size() != 0)) {
            return mPageInfos.get(currentPage);
        }
        return null;
    }

    public String getCurrentPageName(Context context) {
        String currentPageName = "";
        JSONObject pageObj = getCurrentPageInfo(context);
        if (pageObj != null) {
            currentPageName = pageObj.optString("page_name", "");
        }
        return currentPageName;
    }

    public JSONObject getCurrentPageInfo(Context context) {
        String currentPage = getCurrentPage(context);
        if (mPageInfos != null && (mPageInfos.size() != 0)) {
            return mPageInfos.get(currentPage);
        }
        return null;
    }

    public String getCurrentPage(Context context){
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
    }

}
