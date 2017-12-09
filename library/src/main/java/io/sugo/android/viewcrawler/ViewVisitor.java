package io.sugo.android.viewcrawler;

import android.annotation.TargetApi;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

import io.sugo.android.mpmetrics.SGConfig;

@TargetApi(SGConfig.UI_FEATURES_MIN_API)
abstract class ViewVisitor implements Pathfinder.Accumulator {

    private static final String LOGTAG = "SugoAPI.ViewVisitor";

    private final Pathfinder mPathfinder;
    private View mRootView;
    private final List<Pathfinder.PathElement> mPath;
    private boolean mBinded = false;

    protected ViewVisitor(List<Pathfinder.PathElement> path) {
        mPath = path;
        mPathfinder = new Pathfinder();
    }

    /**
     * Removes listeners and frees resources associated with the visitor.
     * Once cleanup is called, the ViewVisitor should not be used again.
     */
    public abstract void cleanup();

    protected abstract String name();

    /**
     * Scans the View hierarchy below rootView, applying it's operation to each matching child view.
     */
    public void visit(View rootView) {
        mRootView = rootView;
        mPathfinder.findTargetsInRoot(rootView, mPath, this);
    }

    protected List<Pathfinder.PathElement> getPath() {
        return mPath;
    }

    protected Pathfinder getPathfinder() {
        return mPathfinder;
    }

    protected View getRootView() {
        return mRootView;
    }

    public boolean isBinded() {
        return mBinded;
    }

    public void setBinded(boolean isBinded) {
        this.mBinded = isBinded;
    }

    /**
     * onEvent will be fired when whatever the ViewVisitor installed fires
     * (For example, if the ViewVisitor installs watches for clicks, then onEvent will be called
     * on click)
     */
    public interface OnEventListener {
        void onEvent(View host, String eventId, String eventName, JSONObject properties, boolean debounce);
    }

    public interface OnLayoutErrorListener {
        void onLayoutError(LayoutErrorMessage e);
    }

    public static class LayoutErrorMessage {

        private final String mErrorType;
        private final String mName;

        public LayoutErrorMessage(String errorType, String name) {
            mErrorType = errorType;
            mName = name;
        }

        public String getErrorType() {
            return mErrorType;
        }

        public String getName() {
            return mName;
        }

    }

    private static class CycleDetector {

        /**
         * This function detects circular dependencies for all the views under the parent
         * of the updated view. The basic idea is to consider the views as a directed
         * graph and perform a DFS on all the nodes in the graph. If the current node is
         * in the DFS stack already, there must be a circle in the graph. To speed up the
         * search, all the parsed nodes will be removed from the graph.
         */
        public boolean hasCycle(TreeMap<View, List<View>> dependencyGraph) {
            final List<View> dfsStack = new ArrayList<View>();
            while (!dependencyGraph.isEmpty()) {
                View currentNode = dependencyGraph.firstKey();
                if (!detectSubgraphCycle(dependencyGraph, currentNode, dfsStack)) {
                    return false;
                }
            }

            return true;
        }

        private boolean detectSubgraphCycle(TreeMap<View, List<View>> dependencyGraph,
                                            View currentNode, List<View> dfsStack) {
            if (dfsStack.contains(currentNode)) {
                return false;
            }

            if (dependencyGraph.containsKey(currentNode)) {
                final List<View> dependencies = dependencyGraph.remove(currentNode);
                dfsStack.add(currentNode);

                int size = dependencies.size();
                for (int i = 0; i < size; i++) {
                    if (!detectSubgraphCycle(dependencyGraph, dependencies.get(i), dfsStack)) {
                        return false;
                    }
                }

                dfsStack.remove(currentNode);
            }

            return true;
        }
    }

    public static class LayoutUpdateVisitor extends ViewVisitor {

        private final WeakHashMap<View, int[]> mOriginalValues;
        private final List<LayoutRule> mArgs;
        private final String mName;
        private static final Set<Integer> mHorizontalRules = new HashSet<Integer>(Arrays.asList(
                RelativeLayout.LEFT_OF, RelativeLayout.RIGHT_OF,
                RelativeLayout.ALIGN_LEFT, RelativeLayout.ALIGN_RIGHT
        ));
        private static final Set<Integer> mVerticalRules = new HashSet<Integer>(Arrays.asList(
                RelativeLayout.ABOVE, RelativeLayout.BELOW,
                RelativeLayout.ALIGN_BASELINE, RelativeLayout.ALIGN_TOP,
                RelativeLayout.ALIGN_BOTTOM
        ));
        private boolean mAlive;
        private final OnLayoutErrorListener mOnLayoutErrorListener;
        private final CycleDetector mCycleDetector;

        public LayoutUpdateVisitor(List<Pathfinder.PathElement> path, List<LayoutRule> args,
                                   String name, OnLayoutErrorListener onLayoutErrorListener) {
            super(path);
            mOriginalValues = new WeakHashMap<View, int[]>();
            mArgs = args;
            mName = name;
            mAlive = true;
            mOnLayoutErrorListener = onLayoutErrorListener;
            mCycleDetector = new CycleDetector();
        }

        @Override
        public void cleanup() {
            // TODO find a way to optimize this.. remove this visitor and trigger a re-layout??
            for (Map.Entry<View, int[]> original : mOriginalValues.entrySet()) {
                final View changedView = original.getKey();
                final int[] originalValue = original.getValue();
                final RelativeLayout.LayoutParams originalParams = (RelativeLayout.LayoutParams) changedView.getLayoutParams();
                for (int i = 0; i < originalValue.length; i++) {
                    originalParams.addRule(i, originalValue[i]);
                }
                changedView.setLayoutParams(originalParams);
            }
            mAlive = false;
        }

        @Override
        public void visit(View rootView) {
            // this check is necessary - if the layout change is invalid, accumulate will send an error message
            // to the Web UI; before Web UI removes such change, this visit may get called by Android again and
            // thus send another error message to Web UI which leads to lots of weird problems
            if (mAlive) {
                getPathfinder().findTargetsInRoot(rootView, getPath(), this);
            }
        }

        // layout changes are performed on the children of found according to the LayoutRule
        @Override
        public void accumulate(View found) {
            ViewGroup parent = (ViewGroup) found;
            SparseArray<View> idToChild = new SparseArray<View>();

            int count = parent.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = parent.getChildAt(i);
                int childId = child.getId();
                if (childId > 0) {
                    idToChild.put(childId, child);
                }
            }

            int size = mArgs.size();
            for (int i = 0; i < size; i++) {
                LayoutRule layoutRule = mArgs.get(i);
                final View currentChild = idToChild.get(layoutRule.viewId);
                if (null == currentChild) {
                    continue;
                }

                RelativeLayout.LayoutParams currentParams = (RelativeLayout.LayoutParams) currentChild.getLayoutParams();
                final int[] currentRules = currentParams.getRules().clone();

                if (currentRules[layoutRule.verb] == layoutRule.anchor) {
                    continue;
                }

                if (mOriginalValues.containsKey(currentChild)) {
                    ; // Cache exactly one set of rules per child view
                } else {
                    mOriginalValues.put(currentChild, currentRules);
                }

                currentParams.addRule(layoutRule.verb, layoutRule.anchor);

                final Set<Integer> rules;
                if (mHorizontalRules.contains(layoutRule.verb)) {
                    rules = mHorizontalRules;
                } else if (mVerticalRules.contains(layoutRule.verb)) {
                    rules = mVerticalRules;
                } else {
                    rules = null;
                }

                if (rules != null && !verifyLayout(rules, idToChild)) {
                    cleanup();
                    mOnLayoutErrorListener.onLayoutError(new LayoutErrorMessage("circular_dependency", mName));
                    return;
                }

                currentChild.setLayoutParams(currentParams);
            }
        }

        private boolean verifyLayout(Set<Integer> rules, SparseArray<View> idToChild) {
            // We don't really care about the order, as long as it's always the same.
            final TreeMap<View, List<View>> dependencyGraph = new TreeMap<View, List<View>>(new Comparator<View>() {
                @Override
                public int compare(final View lhs, final View rhs) {
                    if (lhs == rhs) {
                        return 0;
                    } else if (null == lhs) {
                        return -1;
                    } else if (null == rhs) {
                        return 1;
                    } else {
                        return rhs.hashCode() - lhs.hashCode();
                    }
                }
            });
            int size = idToChild.size();
            for (int i = 0; i < size; i++) {
                final View child = idToChild.valueAt(i);
                final RelativeLayout.LayoutParams childLayoutParams = (RelativeLayout.LayoutParams) child.getLayoutParams();
                int[] layoutRules = childLayoutParams.getRules();

                final List<View> dependencies = new ArrayList<View>();
                for (int rule : rules) {
                    int dependencyId = layoutRules[rule];
                    if (dependencyId > 0 && dependencyId != child.getId()) {
                        dependencies.add(idToChild.get(dependencyId));
                    }
                }

                dependencyGraph.put(child, dependencies);
            }

            return mCycleDetector.hasCycle(dependencyGraph);
        }

        @Override
        protected String name() {
            return "Layout Update";
        }

    }

    public static class LayoutRule {
        public LayoutRule(int vi, int v, int a) {
            viewId = vi;
            verb = v;
            anchor = a;
        }

        public final int viewId;
        public final int verb;
        public final int anchor;
    }


    private static abstract class BaseEventTriggeringVisitor extends ViewVisitor {

        private final OnEventListener onEventListener;
        protected final String mEvenId;
        protected final String mEventName;
        private final Map<String, List<Pathfinder.PathElement>> mDimMap;
        private final boolean mDebounce;
        private String mEventTypeString;

        public BaseEventTriggeringVisitor(List<Pathfinder.PathElement> path,
                                          String eventId,
                                          String eventName,
                                          Map<String, List<Pathfinder.PathElement>> dimMap,
                                          OnEventListener listener,
                                          boolean debounce) {
            super(path);
            onEventListener = listener;
            mEvenId = eventId;
            mEventName = eventName;
            mDimMap = dimMap;
            mDebounce = debounce;
        }

        protected String getEventName() {
            return mEventName;
        }

        protected String getEventTypeString() {
            return mEventTypeString;
        }

        protected void setEventTypeString(String eventTypeString) {
            mEventTypeString = eventTypeString;
        }

        protected void fireEvent(View found) {
            final JSONObject properties = new JSONObject();

            // 判断是否有关联的控件，去获取控件的值
            if (mDimMap != null && mDimMap.size() > 0) {
                for (final String dimName : mDimMap.keySet()) {
                    getPathfinder().findTargetsInRoot(getRootView(), mDimMap.get(dimName), new Pathfinder.Accumulator() {
                        @Override
                        public void accumulate(View v) {
                            String text = DynamicEventTracker.textPropertyFromView(v);
                            try {
                                properties.put(dimName, text);
                            } catch (JSONException e) {
                                Log.e(LOGTAG, "", e);
                            }
                        }
                    });
                }
            }
            try {
                properties.put(SGConfig.FIELD_EVENT_TYPE, getEventTypeString());
            } catch (JSONException ignored) {
            }
            onEventListener.onEvent(found, mEvenId, mEventName, properties, mDebounce);
        }

    }

    /**
     * Adds an accessibility event, which will fire onEvent, to every matching view.
     */
    public static class AddAccessibilityEventVisitor extends BaseEventTriggeringVisitor {

        private final int mEventType;
        private final WeakHashMap<View, TrackingAccessibilityDelegate> mWatching;

        public AddAccessibilityEventVisitor(List<Pathfinder.PathElement> path,
                                            int accessibilityEventType,
                                            String eventId,
                                            String eventName,
                                            Map<String, List<Pathfinder.PathElement>> dimMap,
                                            OnEventListener listener) {
            super(path, eventId, eventName, dimMap, listener, false);
            mEventType = accessibilityEventType;
            if (mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                setEventTypeString("click");
            } else if (mEventType == AccessibilityEvent.TYPE_VIEW_SELECTED) {
                setEventTypeString("selected");
            } else if (mEventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                setEventTypeString("focus");
            }
            mWatching = new WeakHashMap<View, TrackingAccessibilityDelegate>();
        }

        @Override
        public void cleanup() {
            for (final Map.Entry<View, TrackingAccessibilityDelegate> entry : mWatching.entrySet()) {
                final View v = entry.getKey();
                final TrackingAccessibilityDelegate toCleanup = entry.getValue();
                final View.AccessibilityDelegate currentViewDelegate = getOldDelegate(v);
                if (currentViewDelegate == toCleanup) {
                    v.setAccessibilityDelegate(toCleanup.getRealDelegate());
                } else if (currentViewDelegate instanceof TrackingAccessibilityDelegate) {
                    final TrackingAccessibilityDelegate newChain = (TrackingAccessibilityDelegate) currentViewDelegate;
                    newChain.removeFromDelegateChain(toCleanup);
                } else {
                    // Assume we've been replaced, zeroed out, or for some other reason we're already gone.
                    // (This isn't too weird, for example, it's expected when views get recycled)
                }
            }
            mWatching.clear();
            setBinded(false);
        }

        @Override
        public void accumulate(View found) {
            // 热图渲染
            if (SugoHeatMap.isShowHeatMap()) {
                int startColor = SugoHeatMap.getEventHeat(mEvenId);
                int endColor = SugoHeatMap.sColdColor;
                GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
                        new int[]{startColor, startColor, endColor});
                gradientDrawable.setShape(GradientDrawable.OVAL);
                gradientDrawable.setGradientRadius(Math.max(found.getWidth() / 2, found.getHeight() / 2));
                gradientDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);

                found.setForeground(gradientDrawable);
            }

            final View.AccessibilityDelegate realDelegate = getOldDelegate(found);

            // 判断之前的代理是不是 sugo 的代理，如果是，则判断此次事件与之前代理的事件是不是同一个，如果是，则此次绑定取消
            if (realDelegate != null && realDelegate instanceof TrackingAccessibilityDelegate) {
                final TrackingAccessibilityDelegate currentTracker = (TrackingAccessibilityDelegate) realDelegate;
                if (currentTracker.willFireEvent(getEventName())) {
                    return; // Don't double track
                }
            }

            // We aren't already in the tracking call chain of the view
            // 将本SDK的代理设置进去，则表示 SDK 加入了该 View 事件变化的调用链，
            // 事件发生时，将调用 newDelegate 的 sendAccessibilityEvent，从而实现监听事件并发送的逻辑
            // 执行完将调用原本的 realDelegate，将事件继续传递下去原本的传递链
            final TrackingAccessibilityDelegate newDelegate = new TrackingAccessibilityDelegate(realDelegate);
            found.setAccessibilityDelegate(newDelegate);
            mWatching.put(found, newDelegate);
            setBinded(true);
        }

        @Override
        protected String name() {
            return getEventName() + " event when (" + mEventType + ")";
        }

        private View.AccessibilityDelegate getOldDelegate(View v) {
            View.AccessibilityDelegate ret = null;
            try {
                Class<?> klass = v.getClass();
                // 通过反射调用的方法，所以，这个方法不能被混淆，否则将找不到该方法而引发 BUG
                Method m = klass.getMethod("getAccessibilityDelegate");
                ret = (View.AccessibilityDelegate) m.invoke(v);
            } catch (NoSuchMethodException e) {
                // In this case, we just overwrite the original.
            } catch (IllegalAccessException e) {
                // In this case, we just overwrite the original.
            } catch (InvocationTargetException e) {
                Log.w(LOGTAG, "getAccessibilityDelegate threw an exception when called.", e);
            }

            return ret;
        }

        /**
         * 内部类 AddAccessibilityEventVisitor 的内部类
         */
        private class TrackingAccessibilityDelegate extends View.AccessibilityDelegate {

            private View.AccessibilityDelegate mRealDelegate;

            TrackingAccessibilityDelegate(View.AccessibilityDelegate realDelegate) {
                mRealDelegate = realDelegate;
            }

            View.AccessibilityDelegate getRealDelegate() {
                return mRealDelegate;
            }

            /**
             * 如果是相同的事件名，那么这次绑定就会被取消
             *
             * @param eventName
             * @return
             */
            boolean willFireEvent(final String eventName) {
                if (getEventName().equals(eventName)) {
                    return true;
                } else if (mRealDelegate instanceof TrackingAccessibilityDelegate) {
                    // 如果获取的代理，仍是 sugo 的代理，则递归直到默认实现
                    return ((TrackingAccessibilityDelegate) mRealDelegate).willFireEvent(eventName);
                } else {
                    return false;
                }
            }

            /**
             * 移除掉这一个代理
             */
            void removeFromDelegateChain(final TrackingAccessibilityDelegate other) {
                if (mRealDelegate == other) {
                    mRealDelegate = other.getRealDelegate();
                } else if (mRealDelegate instanceof TrackingAccessibilityDelegate) {
                    final TrackingAccessibilityDelegate child = (TrackingAccessibilityDelegate) mRealDelegate;
                    child.removeFromDelegateChain(other);
                } else {
                    // We can't see any further down the chain, just return.
                }
            }

            @Override
            public void sendAccessibilityEvent(View host, int eventType) {
                if (eventType == mEventType) {
                    fireEvent(host);
                }

                if (null != mRealDelegate) {
                    mRealDelegate.sendAccessibilityEvent(host, eventType);
                }
            }

        }

    }

    /**
     * Installs a TextWatcher in each matching view. Does nothing if matching views are not TextViews.
     */
    public static class AddTextChangeListener extends BaseEventTriggeringVisitor {
        public AddTextChangeListener(List<Pathfinder.PathElement> path, String eventId, String eventName, Map<String, List<Pathfinder.PathElement>> dimMap, OnEventListener listener) {
            super(path, eventId, eventName, dimMap, listener, true);
            mWatching = new HashMap<TextView, TextWatcher>();
            setEventTypeString("text_changed");
        }

        @Override
        public void cleanup() {
            for (final Map.Entry<TextView, TextWatcher> entry : mWatching.entrySet()) {
                final TextView v = entry.getKey();
                final TextWatcher watcher = entry.getValue();
                v.removeTextChangedListener(watcher);
            }

            mWatching.clear();
            setBinded(false);
        }

        @Override
        public void accumulate(View found) {
            if (found instanceof TextView) {
                final TextView foundTextView = (TextView) found;
                final TextWatcher watcher = new TrackingTextWatcher(foundTextView);
                final TextWatcher oldWatcher = mWatching.get(foundTextView);
                if (null != oldWatcher) {
                    foundTextView.removeTextChangedListener(oldWatcher);
                }
                foundTextView.addTextChangedListener(watcher);
                mWatching.put(foundTextView, watcher);
                setBinded(true);
            }
        }

        @Override
        protected String name() {
            return getEventName() + " on Text Change";
        }

        private class TrackingTextWatcher implements TextWatcher {
            public TrackingTextWatcher(View boundTo) {
                mBoundTo = boundTo;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ; // Nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ; // Nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                fireEvent(mBoundTo);
            }

            private final View mBoundTo;
        }

        private final Map<TextView, TextWatcher> mWatching;
    }

    /**
     * Monitors the view tree for the appearance of matching views where there were not
     * matching views before. Fires only once per traversal.
     */
    public static class ViewDetectorVisitor extends BaseEventTriggeringVisitor {

        private boolean mSeen;

        public ViewDetectorVisitor(List<Pathfinder.PathElement> path, String eventId, String eventName, Map<String, List<Pathfinder.PathElement>> dimMap, OnEventListener listener) {
            super(path, eventId, eventName, dimMap, listener, false);
            mSeen = false;
            setEventTypeString("detected");
        }

        @Override
        public void cleanup() {
            // Do nothing, we don't have anything to leak :)
            setBinded(false);
        }

        @Override
        public void accumulate(View found) {
            if (found != null && !mSeen) {
                fireEvent(found);
                setBinded(true);
            }

            mSeen = (found != null);
        }

        @Override
        protected String name() {
            return getEventName() + " when Detected";
        }

    }

}
