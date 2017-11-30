package io.sugo.android.viewcrawler;

import android.app.Activity;
import android.util.JsonWriter;
import android.view.View;

import org.json.JSONArray;

/**
 *
 * @author Administrator
 * @date 2017/2/8
 */

public interface XWalkViewListener {

    void snapshotSpecialView(JsonWriter j, View view);

    void recyclerXWalkView(Activity activity);

    void bindEvents(String token, JSONArray eventBindings);
}
