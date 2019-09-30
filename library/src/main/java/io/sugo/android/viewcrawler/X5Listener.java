package io.sugo.android.viewcrawler;

import android.app.Activity;
import android.util.JsonWriter;
import android.view.View;

import org.json.JSONArray;

/**
 * create by chenyuyi on 2019/9/25
 */
public interface X5Listener {
    void snapshotSpecialView(JsonWriter j, View view);

    void recyclerx5View(Activity activity);

    void bindEvents(String token, JSONArray eventBindings);
}
