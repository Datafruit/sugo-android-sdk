package io.sugo.android.mpmetrics;

import android.app.ActivityManager;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Administrator on 2017/1/22.
 */

public class SugoPageManager {

    private static SugoPageManager sInstance = new SugoPageManager();
    private JSONArray mPageInfos;

    private SugoPageManager() {

    }

    public static SugoPageManager getInstance() {
        return sInstance;
    }

    public void setPageInfos(JSONArray pageInfos) {
        this.mPageInfos = pageInfos;
    }


    public String getCurrentPageName(String currentPage) {
        String currentPageName = "";
        if (mPageInfos != null && !(mPageInfos.length() == 0)) {
            for (int i = 0; i < mPageInfos.length(); i++) {
                try {
                    JSONObject pageObj = mPageInfos.getJSONObject(i);
                    if (currentPage.equals(pageObj.optString("page"))) {
                        currentPageName = pageObj.optString("page_name");
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return currentPageName;
    }

    public String getCurrentPageName(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String currentPage = activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
        String currentPageName = "";
        if (mPageInfos != null && !(mPageInfos.length() == 0)) {
//            String currentPage = currentActivity.getClass().getCanonicalName();
            for (int i = 0; i < mPageInfos.length(); i++) {
                try {
                    JSONObject pageObj = mPageInfos.getJSONObject(i);
                    if (currentPage.equals(pageObj.optString("page"))) {
                        currentPageName = pageObj.optString("page_name");
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return currentPageName;
    }

}
