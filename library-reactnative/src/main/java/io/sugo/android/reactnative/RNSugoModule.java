package io.sugo.android.reactnative;

import android.app.Activity;

import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.UiThreadUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.sugo.android.metrics.SugoAPI;
import io.sugo.android.metrics.SugoWebEventListener;
import io.sugo.android.metrics.SugoWebViewClient;

/**
 * Sugo React Native module.
 * Note that synchronized(instance) is used in methods because that's what SugoAPI.java recommends you do if you are keeping instances.
 */
public class RNSugoModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private Map<String, SugoAPI> instances;
    private SugoAPI sugoAPI;

    public RNSugoModule(ReactApplicationContext reactContext) {
        super(reactContext);

        // Get lifecycle notifications to flush sugo on pause or destroy
        reactContext.addLifecycleEventListener(this);
    }

    /*
    Gets the sugo api instance for the given token.  It must have been created in sharedInstanceWithToken first.
     */
    private SugoAPI getInstance(final String apiToken) {
        if (instances == null) {
            return null;
        }
        return instances.get(apiToken);
    }

    @Override
    public String getName() {
        return "Sugo";
    }

    // Is there a better way to convert ReadableMap to JSONObject?
    // I only found this:
    // https://github.com/andpor/react-native-sqlite-storage/blob/master/src/android/src/main/java/org/pgsqlite/SQLitePluginConverter.java
    static JSONObject reactToJSON(ReadableMap readableMap) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            ReadableType valueType = readableMap.getType(key);
            switch (valueType) {
                case Null:
                    jsonObject.put(key, JSONObject.NULL);
                    break;
                case Boolean:
                    jsonObject.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    jsonObject.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    jsonObject.put(key, readableMap.getString(key));
                    break;
                case Map:
                    jsonObject.put(key, reactToJSON(readableMap.getMap(key)));
                    break;
                case Array:
                    jsonObject.put(key, reactToJSON(readableMap.getArray(key)));
                    break;
            }
        }

        return jsonObject;
    }

    static JSONArray reactToJSON(ReadableArray readableArray) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < readableArray.size(); i++) {
            ReadableType valueType = readableArray.getType(i);
            switch (valueType) {
                case Null:
                    jsonArray.put(JSONObject.NULL);
                    break;
                case Boolean:
                    jsonArray.put(readableArray.getBoolean(i));
                    break;
                case Number:
                    jsonArray.put(readableArray.getDouble(i));
                    break;
                case String:
                    jsonArray.put(readableArray.getString(i));
                    break;
                case Map:
                    jsonArray.put(reactToJSON(readableArray.getMap(i)));
                    break;
                case Array:
                    jsonArray.put(reactToJSON(readableArray.getArray(i)));
                    break;
            }
        }
        return jsonArray;
    }


    @Override
    public void onHostResume() {
    }

    @Override
    public void onHostPause() {
        if (instances != null) {
            for (SugoAPI instance : instances.values()) {
                instance.flush();
            }
        }
    }

    @Override
    public void onHostDestroy() {
        if (instances != null) {
            for (SugoAPI instance : instances.values()) {
                instance.flush();
            }
        }
    }

    @ReactMethod
    public void track(String eventName, ReadableMap props) {
        getSugoAPI().trackMap(eventName, props.toHashMap());
    }

    @ReactMethod
    public void timeEvent(String eventName) {
        getSugoAPI().timeEvent(eventName, "", 0);
    }

    @ReactMethod
    public void registerSuperProperties(ReadableMap superProperties) {
        getSugoAPI().registerSuperPropertiesMap(superProperties.toHashMap());
    }

    @ReactMethod
    public void registerSuperPropertiesOnce(ReadableMap superProperties) {
        getSugoAPI().registerSuperPropertiesOnceMap(superProperties.toHashMap());
    }

    @ReactMethod
    public void unregisterSuperProperty(String superPropertyName) {
        getSugoAPI().unregisterSuperProperty(superPropertyName);
    }

//    @ReactMethod
//    public void getSuperProperties(JSCallback callback) {
//        JSONObject superProps = getSugoAPI().getSuperProperties();
//        Map<String, Object> props = new HashMap<>();
//
//        String key;
//        Object value;
//        Iterator<String> keys = superProps.keys();
//        while (keys.hasNext()) {
//            key = keys.next();
//            value = superProps.opt(key);
//            if (key != null && value != null) {
//                props.put(key, value);
//            }
//        }
//        callback.invoke(props);
//    }

    @ReactMethod
    public void clearSuperProperties() {
        getSugoAPI().clearSuperProperties();
    }

    @ReactMethod
    public void login(String userIdKey, String userIdValue) {
        getSugoAPI().login(userIdKey, userIdValue);
    }

    @ReactMethod
    public void logout() {
        getSugoAPI().logout();
    }

    @ReactMethod
    public void show(String message, int duration) {
        track("toast", null);
        Toast.makeText(getReactApplicationContext(), message, duration).show();
    }

    @ReactMethod
    public void initWebView(Integer tag) {
        final Integer finalTag = tag;
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = getReactApplicationContext().getCurrentActivity();
                View view = activity.findViewById(finalTag);
                if (view != null && view instanceof WebView) {
                    WebView webView = (WebView) view;
                    if (SugoAPI.getSugoWebNodeReporter(webView) == null) {
                        //webView.getSettings().setJavaScriptEnabled(true);
                        getSugoAPI().addWebViewJavascriptInterface(webView);
                    }

                }
            }
        });

    }

    @ReactMethod
    public void handleWebView(Integer tag, String path) {
        final Integer finalTag = tag;
        final String finalPath = path;
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = getReactApplicationContext().getCurrentActivity();
                View view = activity.findViewById(finalTag);
                if (view != null && view instanceof WebView) {
                    WebView webView = (WebView) view;
                    String script = SugoWebViewClient.getInjectScript(activity, finalPath);
                    webView.loadUrl("javascript:" + script);
                    SugoWebEventListener.addCurrentWebView(webView);
                }
            }
        });

    }

    private synchronized SugoAPI getSugoAPI() {
        if (sugoAPI == null) {
            sugoAPI = SugoAPI.getInstance(getReactApplicationContext());
        }
        return sugoAPI;
    }
}