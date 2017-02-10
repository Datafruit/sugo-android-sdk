package io.sugo.android.viewcrawler;

import org.json.JSONArray;

import io.sugo.android.mpmetrics.OnMixpanelTweaksUpdatedListener;
import io.sugo.android.mpmetrics.Tweaks;

/* This interface is for internal use in the Mixpanel library, and should not be
   implemented in client code. */
public interface UpdatesFromMixpanel {
    public void startUpdates();
    public void sendTestEvent(JSONArray events);
    public void setEventBindings(JSONArray bindings);
    public void setH5EventBindings(JSONArray bindings);
    public void setPageInfos(JSONArray pageInfos);
    public void setVariants(JSONArray variants);
    public Tweaks getTweaks();
    public void addOnMixpanelTweaksUpdatedListener(OnMixpanelTweaksUpdatedListener listener);
    public void removeOnMixpanelTweaksUpdatedListener(OnMixpanelTweaksUpdatedListener listener);
    void sendConnectEditor();
}
