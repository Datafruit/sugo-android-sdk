package io.sugo.android.mpmetrics;

/**
 * This interface is for internal use in the Sugo library, and should not be included in
 * client code.
 */
public interface ResourceIds {
    public boolean knownIdName(String name);
    public int idFromName(String name);
    public String nameForId(int id);
}
