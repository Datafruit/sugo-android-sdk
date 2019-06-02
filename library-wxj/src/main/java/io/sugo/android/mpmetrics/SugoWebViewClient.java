package io.sugo.android.mpmetrics;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.core.XWalkView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import io.sugo.android.util.ExceptionInfoUtils;
import io.sugo.android.util.FileUtils;
import io.sugo.android.util.StringEscapeUtils;
import io.sugo.android.viewcrawler.ViewCrawler;

/**
 * Created by fengxj on 10/31/16.
 */

public class SugoWebViewClient extends WebViewClient {

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        handlePageFinished(view, url);
    }


    public static void handlePageFinished(WebView view, String url) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.setWebContentsDebuggingEnabled(true);
        }

        Context context = view.getContext();

        Activity activity = (Activity) context;
        String script = getInjectScript(activity, url);
        view.loadUrl("javascript:" + script);

        SugoWebEventListener.addCurrentWebView(view);
    }

    public static void handlePageFinished(XWalkView view, String url) {
        Context context = view.getContext();
        Activity activity = (Activity) context;
        String script = getInjectScript(activity, url);
        view.load("javascript:" + script, "");

        SugoWebEventListener.addCurrentXWalkView(view);
    }

    public static void handlePageFinished(WebViewDelegate delegate, Activity activity, String url) {
        String script = getInjectScript(activity, url);
        delegate.loadUrl("javascript:" + script);
    }

    public static String getInjectScript(Activity activity, String url) {
        SugoAPI sugoAPI = SugoAPI.getInstance(activity);

        String webRoot = sugoAPI.getConfig().getWebRoot();

        String filePath = activity.getFilesDir().getPath(); // /data/user/0/io.sugo.xxx/files
        String dataPkgPath = filePath.substring(0, filePath.indexOf("/files"));      // /data/user/0/io.sugo.xxx

        JSONObject pageInfo = getPageInfo(activity, url, dataPkgPath);
        String initCode = "";
        String pageName = "";
        String pageCategory = "";
        if (pageInfo != null) {
            initCode = pageInfo.optString("code", "");
            pageName = pageInfo.optString("page_name", "");
            pageCategory = pageInfo.optString("category", "");
        }
        if (!TextUtils.isEmpty(initCode)) {
            initCode = StringEscapeUtils.escapeJava(initCode);
        }

        String activityName = activity.getClass().getName();

        String bindingEvents = getBindingEvents(activity);

        String tempTrackJS = FileUtils.getAssetsFileContent(activity,"Sugo.js",true);
        tempTrackJS = tempTrackJS.replace("$sugo_enable_page_event$", sugoAPI.getConfig().isEnablePageEvent() + "");
        tempTrackJS = tempTrackJS.replace("$sugo_webroot$", webRoot);
        tempTrackJS = tempTrackJS.replace("$sugo_remove_path$", dataPkgPath);
        tempTrackJS = tempTrackJS.replace("$sugo_init_code$", initCode);
        tempTrackJS = tempTrackJS.replace("$sugo_init_page_name$", pageName);
        tempTrackJS = tempTrackJS.replace("$sugo_init_page_category$", pageCategory);
        tempTrackJS = tempTrackJS.replace("$sugo_activity_name$", activityName);
        tempTrackJS = tempTrackJS.replace("$sugo_path_name$", SGConfig.FIELD_PAGE);
        tempTrackJS = tempTrackJS.replace("$sugo_page_name$", SGConfig.FIELD_PAGE_NAME);
        tempTrackJS = tempTrackJS.replace("$sugo_page_category_key$", SGConfig.FIELD_PAGE_CATEGORY);
        tempTrackJS = tempTrackJS.replace("$sugo_h5_event_bindings$", bindingEvents);

        StringBuffer scriptBuf = new StringBuffer();
        scriptBuf.append(FileUtils.getAssetsFileContent(activity,"SugoCss.js",true));
        scriptBuf.append(tempTrackJS);
        return scriptBuf.toString();
    }

    private static JSONObject getPageInfo(Activity activity, String url, String dataPkgPath) {
        SugoAPI sugoAPI = SugoAPI.getInstance(activity.getApplicationContext());
        String realPath = "";
        try {
            Pattern pattern = Pattern.compile("^[A-Za-z0-9]*://.*", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(url).matches()) {
                URL urlObj = new URL(url);
                realPath = urlObj.getPath();
                realPath = realPath.replaceFirst(dataPkgPath, "");
                String ref = urlObj.getRef();
                if (!TextUtils.isEmpty(ref)) {
                    if (ref.contains("?")) {
                        int qIndex = ref.indexOf("?");
                        ref = ref.substring(0, qIndex);
                    }
                    realPath = realPath + "#" + ref;
                }
                realPath = realPath.replace("#/", "#");
                realPath = realPath.replace("//", "/");
            }
            realPath = realPath.replace(sugoAPI.getConfig().getWebRoot(), "");
        } catch (MalformedURLException e) {
            sugoAPI.track(null,ExceptionInfoUtils.EVENTNAME,ExceptionInfoUtils.ExceptionInfo(sugoAPI.getCurrentContext(),e));
            e.printStackTrace();
        }
        return SugoPageManager.getInstance().getCurrentPageInfo(realPath);
    }

    private static String getBindingEvents(Activity activity) {
        String token = SugoAPI.getInstance(activity.getApplicationContext()).getConfig().getToken();
        JSONArray eventBindings = SugoWebEventListener.getBindEvents(token);

        if (eventBindings == null) {
            final String sharedPrefsName = ViewCrawler.SHARED_PREF_EDITS_FILE + token;
            SharedPreferences preferences = activity.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
            final String storedBindings = preferences.getString(ViewCrawler.SHARED_PREF_H5_BINDINGS_KEY, null);
            if (storedBindings != null && !storedBindings.equals("")) {
                try {
                    eventBindings = new JSONArray(storedBindings);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (eventBindings == null) {
            eventBindings = new JSONArray();
        }
        return eventBindings.toString();
    }

}
