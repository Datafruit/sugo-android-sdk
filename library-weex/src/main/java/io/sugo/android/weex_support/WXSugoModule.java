package io.sugo.android.weex_support;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.taobao.weex.WXSDKManager;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXWeb;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.sugo.android.metrics.SGConfig;
import io.sugo.android.metrics.SugoAPI;
import io.sugo.android.metrics.SugoWebViewClient;

/**
 * @author going
 * @date 2018/1/2
 */

public class WXSugoModule extends WXModule {

    public static final String LOG_TAG = WXSugoModule.class.getSimpleName();

    private SugoAPI sugoAPI;

    @JSMethod
    public void track(String eventName, Map<String, Object> props) {
        getSugoAPI().trackMap(eventName, props);
    }

    @JSMethod
    public void timeEvent(String eventName) {
        getSugoAPI().timeEvent(eventName, "", 0);
    }

    @JSMethod
    public void registerSuperProperties(Map<String, Object> superProperties) {
        getSugoAPI().registerSuperPropertiesMap(superProperties);
    }

    @JSMethod
    public void registerSuperPropertiesOnce(Map<String, Object> superProperties) {
        getSugoAPI().registerSuperPropertiesOnceMap(superProperties);
    }

    @JSMethod
    public void unregisterSuperProperty(String superPropertyName) {
        getSugoAPI().unregisterSuperProperty(superPropertyName);
    }

    @JSMethod
    public void getSuperProperties(JSCallback callback) {
        JSONObject superProps = getSugoAPI().getSuperProperties();
        Map<String, Object> props = new HashMap<>();

        String key;
        Object value;
        Iterator<String> keys = superProps.keys();
        while (keys.hasNext()) {
            key = keys.next();
            value = superProps.opt(key);
            if (key != null && value != null) {
                props.put(key, value);
            }
        }
        callback.invoke(props);
    }

    @JSMethod
    public void clearSuperProperties() {
        getSugoAPI().clearSuperProperties();
    }

    @JSMethod
    public void login(String userIdKey, String userIdValue) {
        getSugoAPI().login(userIdKey, userIdValue);
    }

    @JSMethod
    public void logout() {
        getSugoAPI().logout();
    }

    /**
     * @param path
     * @param ref
     */
    @JSMethod
    public void handleWebView(String path, String ref) {
        WXComponent webComponent =
                WXSDKManager.getInstance()
                        .getWXRenderManager()
                        .getWXComponent(mWXSDKInstance.getInstanceId(), ref);
        if (webComponent instanceof WXWeb) {
            WebView webView = null;
            View hostView = ((WXWeb) webComponent).getHostView();
            if (hostView instanceof ViewGroup) {
                int count = ((ViewGroup) hostView).getChildCount();
                for (int i = 0; i < count; i++) {
                    View childView = ((ViewGroup) hostView).getChildAt(i);
                    if (childView instanceof WebView) {
                        webView = (WebView) childView;
                        break;
                    }
                }
                if (webView != null) {
                    webView.getSettings().setJavaScriptEnabled(true);
                    getSugoAPI().addWebViewJavascriptInterface(webView);
                    SugoWebViewClient.handlePageFinished(webView, path);
                }
            }
        }
    }

    private synchronized SugoAPI getSugoAPI() {
        if (sugoAPI == null) {
            if (SGConfig.DEBUG) {
                Log.d(LOG_TAG, "SugoAPI.getInstance");
            }
            sugoAPI = SugoAPI.getInstance(mWXSDKInstance.getContext());
        }
        return sugoAPI;
    }

}
