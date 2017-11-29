package io.sugo.android.mpmetrics;

/**
 */
public interface OnSugoUpdatesReceivedListener {
    /**
     * Called when the Sugo library has updates, for example, Surveys or Notifications.
     * This method will not be called once per update, but rather any time a batch of updates
     * becomes available. The related updates can be checked with
     */
    public void onSugoUpdatesReceived();
}
