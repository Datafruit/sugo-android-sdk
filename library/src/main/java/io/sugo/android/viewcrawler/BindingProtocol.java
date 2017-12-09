package io.sugo.android.viewcrawler;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.sugo.android.mpmetrics.ResourceIds;
import io.sugo.android.util.ImageStore;
import io.sugo.android.util.JSONUtils;

class BindingProtocol {

    @SuppressWarnings("unused")
    private static final String LOGTAG = "SugoAPI.EProtocol";

    private final ResourceIds mResourceIds;
    private final ImageStore mImageStore;
    private final ViewVisitor.OnLayoutErrorListener mLayoutErrorListener;

    private static final Class<?>[] NO_PARAMS = new Class[0];
    private static final List<Pathfinder.PathElement> NEVER_MATCH_PATH = Collections.<Pathfinder.PathElement>emptyList();

    public BindingProtocol(ResourceIds resourceIds, ImageStore imageStore, ViewVisitor.OnLayoutErrorListener layoutErrorListener) {
        mResourceIds = resourceIds;
        mImageStore = imageStore;
        mLayoutErrorListener = layoutErrorListener;
    }

    public ViewVisitor readEventBinding(JSONObject source, ViewVisitor.OnEventListener listener) throws BadInstructionsException {
        try {
            final String eventName = source.getString("event_name");
            final String eventId = source.getString("event_id");
            final String eventType = source.getString("event_type");

            final JSONArray pathDesc = source.getJSONArray("path");
            final List<Pathfinder.PathElement> path = readPath(pathDesc, mResourceIds);

            if (path.size() == 0) {
                throw new InapplicableInstructionsException("event '" + eventName + "' will not be bound to any element in the UI.");
            }

            Map<String, List<Pathfinder.PathElement>> dimMap = null;
            if (source.has("attributes")) {
                JSONObject attributes = source.getJSONObject("attributes");
                dimMap = new HashMap<String, List<Pathfinder.PathElement>>();
                final Iterator<?> propIter = attributes.keys();
                while (propIter.hasNext()) {
                    final String key = (String) propIter.next();
                    final List<Pathfinder.PathElement> bindPaths = readPath(attributes.getJSONArray(key), mResourceIds);
                    dimMap.put(key, bindPaths);
                }
            }
            if ("click".equals(eventType)) {
                return new ViewVisitor.AddAccessibilityEventVisitor(
                        path,
                        AccessibilityEvent.TYPE_VIEW_CLICKED,
                        eventId,
                        eventName,
                        dimMap,
                        listener
                );
            } else if ("selected".equals(eventType)) {
                return new ViewVisitor.AddAccessibilityEventVisitor(
                        path,
                        AccessibilityEvent.TYPE_VIEW_SELECTED,
                        eventId,
                        eventName,
                        dimMap,
                        listener
                );
            } else if ("focus".equals(eventType)) {
                return new ViewVisitor.AddAccessibilityEventVisitor(
                        path,
                        AccessibilityEvent.TYPE_VIEW_FOCUSED,
                        eventId,
                        eventName,
                        dimMap,
                        listener
                );
            } else if ("text_changed".equals(eventType)) {
                return new ViewVisitor.AddTextChangeListener(path, eventId, eventName, dimMap, listener);
            } else if ("detected".equals(eventType)) {
                return new ViewVisitor.ViewDetectorVisitor(path, eventId, eventName, dimMap, listener);
            } else {
                throw new BadInstructionsException("Sugo can't track event type \"" + eventType + "\"");
            }
        } catch (final JSONException e) {
            throw new BadInstructionsException("Can't interpret instructions due to JSONException", e);
        }
    }

    public ViewSnapshot readSnapshotConfig(JSONObject source) throws BadInstructionsException {
        final List<PropertyDescription> properties = new ArrayList<PropertyDescription>();

        try {
            final JSONObject config = source.getJSONObject("config");
            final JSONArray classes = config.getJSONArray("classes");
            for (int classIx = 0; classIx < classes.length(); classIx++) {
                final JSONObject classDesc = classes.getJSONObject(classIx);
                final String targetClassName = classDesc.getString("name");
                final Class<?> targetClass = Class.forName(targetClassName);

                final JSONArray propertyDescs = classDesc.getJSONArray("properties");
                for (int i = 0; i < propertyDescs.length(); i++) {
                    final JSONObject propertyDesc = propertyDescs.getJSONObject(i);
                    final PropertyDescription desc = readPropertyDescription(targetClass, propertyDesc);
                    properties.add(desc);
                }
            }

            return new ViewSnapshot(properties, mResourceIds);
        } catch (JSONException e) {
            throw new BadInstructionsException("Can't read snapshot configuration", e);
        } catch (final ClassNotFoundException e) {
            throw new BadInstructionsException("Can't resolve types for snapshot configuration", e);
        }
    }

    // Package access FOR TESTING ONLY
    private List<Pathfinder.PathElement> readPath(JSONArray pathDesc, ResourceIds idNameToId) throws JSONException {
        final List<Pathfinder.PathElement> path = new ArrayList<Pathfinder.PathElement>();

        for (int i = 0; i < pathDesc.length(); i++) {
            final JSONObject targetView = pathDesc.getJSONObject(i);

            final String prefixCode = JSONUtils.optionalStringKey(targetView, "prefix");
            final String targetViewClass = JSONUtils.optionalStringKey(targetView, "view_class");
            final int targetIndex = targetView.optInt("index", -1);
            final String targetDescription = JSONUtils.optionalStringKey(targetView, "contentDescription");
            final int targetExplicitId = targetView.optInt("id", -1);
            final String targetIdName = JSONUtils.optionalStringKey(targetView, "mp_id_name");
            final String targetTag = JSONUtils.optionalStringKey(targetView, "tag");

            final int prefix;
            if ("shortest".equals(prefixCode)) {
                prefix = Pathfinder.PathElement.SHORTEST_PREFIX;
            } else if (null == prefixCode) {
                prefix = Pathfinder.PathElement.ZERO_LENGTH_PREFIX;
            } else {
                Log.w(LOGTAG, "Unrecognized prefix type \"" + prefixCode + "\". No views will be matched");
                return NEVER_MATCH_PATH;
            }

            final int targetId;

            final Integer targetIdOrNull = reconcileIds(targetExplicitId, targetIdName, idNameToId);
            if (null == targetIdOrNull) {
                return NEVER_MATCH_PATH;
            } else {
                targetId = targetIdOrNull.intValue();
            }

            path.add(new Pathfinder.PathElement(prefix, targetViewClass, targetIndex, targetId, targetDescription, targetTag));
        }

        return path;
    }

    // May return null (and log a warning) if arguments cannot be reconciled
    private Integer reconcileIds(int explicitId, String idName, ResourceIds idNameToId) {
        final int idFromName;
        if (null != idName) {
            if (idNameToId.knownIdName(idName)) {
                idFromName = idNameToId.idFromName(idName);
            } else {
                Log.w(LOGTAG,
                        "Path element contains an id name not known to the system. No views will be matched.\n" +
                                "Make sure that you're not stripping your packages R class out with proguard.\n" +
                                "id name was \"" + idName + "\""
                );
                return null;
            }
        } else {
            idFromName = -1;
        }

        if (-1 != idFromName && -1 != explicitId && idFromName != explicitId) {
            Log.e(LOGTAG, "Path contains both a named and an explicit id, and they don't match. No views will be matched.");
            return null;
        }

        if (-1 != idFromName) {
            return idFromName;
        }

        return explicitId;
    }

    private PropertyDescription readPropertyDescription(Class<?> targetClass, JSONObject propertyDesc)
            throws BadInstructionsException {
        try {
            final String propName = propertyDesc.getString("name");

            Caller accessor = null;
            if (propertyDesc.has("get")) {
                final JSONObject accessorConfig = propertyDesc.getJSONObject("get");
                final String accessorName = accessorConfig.getString("selector");
                final String accessorResultTypeName = accessorConfig.getJSONObject("result").getString("type");
                final Class<?> accessorResultType = Class.forName(accessorResultTypeName);
                accessor = new Caller(targetClass, accessorName, NO_PARAMS, accessorResultType);
            }

            final String mutatorName;
            if (propertyDesc.has("set")) {
                final JSONObject mutatorConfig = propertyDesc.getJSONObject("set");
                mutatorName = mutatorConfig.getString("selector");
            } else {
                mutatorName = null;
            }

            return new PropertyDescription(propName, targetClass, accessor, mutatorName);
        } catch (final NoSuchMethodException e) {
            throw new BadInstructionsException("Can't create property reader", e);
        } catch (final JSONException e) {
            throw new BadInstructionsException("Can't read property JSON", e);
        } catch (final ClassNotFoundException e) {
            throw new BadInstructionsException("Can't read property JSON, relevant arg/return class not found", e);
        }
    }

    private Object convertArgument(Object jsonArgument, String type, List<String> assetsLoaded)
            throws BadInstructionsException, CantGetEditAssetsException {
        // Object is a Boolean, JSONArray, JSONObject, Number, String, or JSONObject.NULL
        try {
            if ("java.lang.CharSequence".equals(type)) { // Because we're assignable
                return jsonArgument;
            } else if ("boolean".equals(type) || "java.lang.Boolean".equals(type)) {
                return jsonArgument;
            } else if ("int".equals(type) || "java.lang.Integer".equals(type)) {
                return ((Number) jsonArgument).intValue();
            } else if ("float".equals(type) || "java.lang.Float".equals(type)) {
                return ((Number) jsonArgument).floatValue();
            } else if ("android.graphics.drawable.Drawable".equals(type)) {
                // For historical reasons, we attempt to interpret generic Drawables as BitmapDrawables
                return readBitmapDrawable((JSONObject) jsonArgument, assetsLoaded);
            } else if ("android.graphics.drawable.BitmapDrawable".equals(type)) {
                return readBitmapDrawable((JSONObject) jsonArgument, assetsLoaded);
            } else if ("android.graphics.drawable.ColorDrawable".equals(type)) {
                int colorValue = ((Number) jsonArgument).intValue();
                return new ColorDrawable(colorValue);
            } else {
                throw new BadInstructionsException("Don't know how to interpret type " + type + " (arg was " + jsonArgument + ")");
            }
        } catch (final ClassCastException e) {
            throw new BadInstructionsException("Couldn't interpret <" + jsonArgument + "> as " + type);
        }
    }

    private Drawable readBitmapDrawable(JSONObject description, List<String> assetsLoaded)
            throws BadInstructionsException, CantGetEditAssetsException {
        try {
            if (description.isNull("url")) {
                throw new BadInstructionsException("Can't construct a BitmapDrawable with a null url");
            }

            final String url = description.getString("url");

            final boolean useBounds;
            final int left;
            final int right;
            final int top;
            final int bottom;
            if (description.isNull("dimensions")) {
                left = right = top = bottom = 0;
                useBounds = false;
            } else {
                final JSONObject dimensions = description.getJSONObject("dimensions");
                left = dimensions.getInt("left");
                right = dimensions.getInt("right");
                top = dimensions.getInt("top");
                bottom = dimensions.getInt("bottom");
                useBounds = true;
            }

            final Bitmap image;
            try {
                image = mImageStore.getImage(url);
                assetsLoaded.add(url);
            } catch (ImageStore.CantGetImageException e) {
                throw new CantGetEditAssetsException(e.getMessage(), e.getCause());
            }

            final Drawable ret = new BitmapDrawable(Resources.getSystem(), image);
            if (useBounds) {
                ret.setBounds(left, top, right, bottom);
            }

            return ret;
        } catch (JSONException e) {
            throw new BadInstructionsException("Couldn't read drawable description", e);
        }
    }

    public static class BadInstructionsException extends Exception {

        private static final long serialVersionUID = -4062004792184145311L;

        public BadInstructionsException(String message) {
            super(message);
        }

        public BadInstructionsException(String message, Throwable e) {
            super(message, e);
        }
    }

    public static class InapplicableInstructionsException extends BadInstructionsException {

        private static final long serialVersionUID = 3977056710817909104L;

        public InapplicableInstructionsException(String message) {
            super(message);
        }
    }

    public static class CantGetEditAssetsException extends Exception {

        public CantGetEditAssetsException(String message) {
            super(message);
        }

        public CantGetEditAssetsException(String message, Throwable cause) {
            super(message, cause);
        }
    }

} // BindingProtocol
