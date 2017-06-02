package io.sugo.android.mpmetrics;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Administrator on 2017/1/22.
 */

public class SugoPageManager {

    private static SugoPageManager sInstance = new SugoPageManager();
    private HashMap<String, JSONObject> mPageInfos;
    private HashMap<String, JSONObject> mTempPageInfos = new HashMap<>();

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
        mTempPageInfos = new HashMap<>(mPageInfos);
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

    public String getCurrentPage(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activityManager.getRunningTasks(1);
        if (runningTaskInfos != null && !runningTaskInfos.isEmpty()) {
            return runningTaskInfos.get(0).topActivity.getClassName();
        }
        return null;
    }

    /**
     * 因为点击事件是按照 Activity 的路径走的，当点击 Fragment 的时候，
     * 获取的并不是 Fragment 的页面名称，所以要 Activity 的页面名称改成 Fragment 的页面名称
     *
     * @param activity
     * @param pageName
     */
    public void replaceCurrentActivityPageName(Activity activity, String pageName) {
        String activityPage = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            activityPage = activity.getClass().getCanonicalName();
        } else {
            activityPage = SugoPageManager.getInstance().getCurrentPage(activity);
        }
        try {
            JSONObject obj = mPageInfos.get(activityPage);
            if (obj == null) {
                obj = new JSONObject();
                mPageInfos.put(activityPage, obj);
            }
            obj.put("page_name", pageName);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 如果当前 Activity 的页面名称被替换了，可以调用这个方法恢复
     *
     * @param activity
     */
    public void restoreCurrentActivityPageName(Activity activity) {
        String page = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            page = activity.getClass().getCanonicalName();
        } else {
            page = SugoPageManager.getInstance().getCurrentPage(activity);
        }
        if (mTempPageInfos != null) {
            JSONObject obj = mTempPageInfos.get(page);
            mPageInfos.put(page, obj);
        }
    }
}
