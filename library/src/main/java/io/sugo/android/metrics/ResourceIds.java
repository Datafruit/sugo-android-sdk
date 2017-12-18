package io.sugo.android.metrics;

/**
 * This interface is for internal use in the Sugo library, and should not be included in
 * client code.
 */
public interface ResourceIds {
    boolean knownIdName(String name);
    int idFromName(String name);
    String nameForId(int id);
}
