package io.sugo.android.viewcrawler;

import android.net.Uri;

import org.json.JSONArray;

/* This interface is for internal use in the Sugo library, and should not be
   implemented in client code. */
public interface UpdatesFromSugo {
    void startUpdates();
    void sendTestEvent(JSONArray events);
    void setEventBindings(JSONArray bindings);
    void setH5EventBindings(JSONArray bindings);
    void setPageInfos(JSONArray pageInfos);
    void setDimensions(JSONArray dimensions);
    void setXWalkViewListener(XWalkViewListener XWalkViewListener);
    boolean sendConnectEditor(Uri data);

}
