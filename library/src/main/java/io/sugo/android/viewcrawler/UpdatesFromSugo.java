package io.sugo.android.viewcrawler;

import android.net.Uri;

import org.json.JSONArray;

import io.sugo.android.mpmetrics.OnMixpanelTweaksUpdatedListener;
import io.sugo.android.mpmetrics.Tweaks;

/* This interface is for internal use in the Mixpanel library, and should not be
   implemented in client code. */
public interface UpdatesFromSugo {
    public void startUpdates();
    public void sendTestEvent(JSONArray events);
    public void setEventBindings(JSONArray bindings);
    public void setH5EventBindings(JSONArray bindings);
    public void setPageInfos(JSONArray pageInfos);
    void setDimensions(JSONArray dimensions);
    public void setVariants(JSONArray variants);
    public Tweaks getTweaks();
    public void addOnMixpanelTweaksUpdatedListener(OnMixpanelTweaksUpdatedListener listener);
    public void removeOnMixpanelTweaksUpdatedListener(OnMixpanelTweaksUpdatedListener listener);
    void setXWalkViewListener(XWalkViewListener XWalkViewListener);
    void sendConnectEditor(Uri data);

}
