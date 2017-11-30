package io.sugo.android.viewcrawler;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Paths in the view hierarchy, and the machinery for finding views using them.
 * <p>
 * An individual pathfinder is NOT THREAD SAFE, and should only be used by one thread at a time.
 */
class Pathfinder {

    @SuppressWarnings("unused")
    private static final String LOGTAG = "SugoAPI.PathFinder";

    private final IntStack mIndexStack;

    public Pathfinder() {
        mIndexStack = new IntStack();
    }

    /**
     * a path element E matches a view V if each non "prefix" or "index"
     * attribute of E is equal to (or characteristic of) V.
     * <p>
     * So
     * <p>
     * E.viewClassName == 'com.sugo.Awesome' => V instanceof com.sugo.Awesome
     * E.id == 123 => V.getId() == 123
     * <p>
     * The index attribute, counting from root to leaf, and first child to last child, selects a particular
     * matching view amongst all possible matches. Indexing starts at zero, like an array
     * index. So E.index == 2 means "Select the third possible match for this element"
     * <p>
     * The prefix attribute refers to the position of the matched views in the hierarchy,
     * relative to the current position of the path being searched. The "current position" of
     * a path element is determined by the path that preceeded that element:
     * <p>
     * - The current position of the empty path is the root view
     * <p>
     * - The current position of a non-empty path is the children of any element that matched the last
     * element of that path.
     * <p>
     * Prefix values can be:
     * <p>
     * ZERO_LENGTH_PREFIX- the next match must occur at the current position (so at the root
     * view if this is the first element of a path, or at the matching children of the views
     * already matched by the preceeding portion of the path.) If a path element with ZERO_LENGTH_PREFIX
     * has no index, then *all* matching elements of the path will be matched, otherwise indeces
     * will count from first child to last child.
     * <p>
     * SHORTEST_PREFIX- the next match must occur at some descendant of the current position.
     * SHORTEST_PREFIX elements are indexed depth-first, first child to last child. For performance
     * reasons, at most one element will ever be matched to a SHORTEST_PREFIX element, so
     * elements with no index will be treated as having index == 0
     */
    public static class PathElement {
        public PathElement(int usePrefix, String vClass, int ix, int vId, String cDesc, String vTag) {
            prefix = usePrefix;
            viewClassName = vClass;
            index = ix;
            viewId = vId;
            contentDescription = cDesc;
            tag = vTag;
        }

        @Override
        public String toString() {
            try {
                final JSONObject ret = new JSONObject();
                if (prefix == SHORTEST_PREFIX) {
                    ret.put("prefix", "shortest");
                }
                if (null != viewClassName) {
                    ret.put("view_class", viewClassName);
                }
                if (index > -1) {
                    ret.put("index", index);
                }
                if (viewId > -1) {
                    ret.put("id", viewId);
                }
                if (null != contentDescription) {
                    ret.put("contentDescription", contentDescription);
                }
                if (null != tag) {
                    ret.put("tag", tag);
                }
                return ret.toString();
            } catch (final JSONException e) {
                throw new RuntimeException("Can't serialize PathElement to String", e);
            }
        }

        public final int prefix;
        public final String viewClassName;
        public final int index;
        public final int viewId;
        public final String contentDescription;
        public final String tag;

        public static final int ZERO_LENGTH_PREFIX = 0;
        public static final int SHORTEST_PREFIX = 1;
    }

    public interface Accumulator {
        public void accumulate(View v);
    }

    public void findTargetsInRoot(View givenRootView, List<PathElement> path, Accumulator accumulator) {
        if (path.isEmpty()) {
            return;
        }

        if (mIndexStack.full()) {
            Log.w(LOGTAG, "There appears to be a concurrency issue in the pathfinding code. Path will not be matched.");
            return; // No memory to perform the find.
        }

        final PathElement rootPathElement = path.get(0);
        final List<PathElement> childPath = path.subList(1, path.size());

        final int indexKey = mIndexStack.alloc();
        final View rootView = findPrefixedMatch(rootPathElement, givenRootView, indexKey);
        mIndexStack.free();

        if (null != rootView) {
            findTargetsInMatchedView(rootView, childPath, accumulator);
        }
    }

    /**
     * 查找 path 的每一段
     *
     * @param alreadyMatched
     * @param remainingPath
     * @param accumulator
     */
    private void findTargetsInMatchedView(View alreadyMatched, List<PathElement> remainingPath, Accumulator accumulator) {
        // When this is run, alreadyMatched has already been matched to a path prefix.
        // path is a possibly empty "remaining path" suffix left over after the match

        if (remainingPath.isEmpty()) {
            // Nothing left to match- we're found!
            accumulator.accumulate(alreadyMatched);
            return;
        }

        if (!(alreadyMatched instanceof ViewGroup)) {
            // Matching a non-empty path suffix is impossible, because we have no children
            return;
        }

        if (mIndexStack.full()) {
            Log.v(LOGTAG, "Path is too deep, will not match");
            // Can't match anyhow, stack is too deep
            return;
        }

        final ViewGroup parent = (ViewGroup) alreadyMatched;
        final PathElement matchElement = remainingPath.get(0);
        final List<PathElement> nextPath = remainingPath.subList(1, remainingPath.size());

        final int childCount = parent.getChildCount();
        final int indexKey = mIndexStack.alloc();
        for (int i = 0; i < childCount; i++) {
            final View givenChild = parent.getChildAt(i);
            final View child = findPrefixedMatch(matchElement, givenChild, indexKey);
            if (null != child) {
                findTargetsInMatchedView(child, nextPath, accumulator);
            }
            // 如果当前元素的 index 已经超过了已有的布局的 index ，则退出循环
            if (matchElement.index >= 0 && mIndexStack.read(indexKey) > matchElement.index) {
                break;
            }
        }
        mIndexStack.free();
    }

    // Finds the first matching view of the path element in the given subject's view hierarchy.
    // If the path is indexed, it needs a start index, and will consume some indexes

    /**
     * 判断 subject view 是否匹配当前 path
     * 在匹配 id tag contentDescription 的情况下，判断 index 是否相同
     * index 一般在那些没有命名 id / tag / contentDescription 的情况下才会有不同，否则都等于 0
     *
     * @param findElement
     * @param subject
     * @param indexKey
     * @return
     */
    private View findPrefixedMatch(PathElement findElement, View subject, int indexKey) {
        final int currentIndex = mIndexStack.read(indexKey);
        if (matches(findElement, subject)) {
            if (findElement.index == -1 || findElement.index == currentIndex) {
                return subject;
            }
            // 当前没有匹配上，则匹配下一个 index (外层循环是传一个兄弟 view 进来)
            mIndexStack.increment(indexKey);
        }

        // 如果是 shortest 规则的元素，则一直递归查找到对应的元素，一般是指 content_view，以优化遍历性能
        if (findElement.prefix == PathElement.SHORTEST_PREFIX && subject instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) subject;
            final int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = group.getChildAt(i);
                final View result = findPrefixedMatch(findElement, child, indexKey);
                if (null != result) {
                    return result;
                }
            }
        }

        return null;
    }

    private boolean matches(PathElement matchElement, View subject) {
        // 判断 subject 是否是该元素的类型或父类型
        if (null != matchElement.viewClassName &&
                !hasClassName(subject, matchElement.viewClassName)) {
            return false;
        }

        // 判断 subject 的 id 是否匹配
        final int subjectId = subject.getId();
        if (-1 != matchElement.viewId && subjectId != matchElement.viewId) {
            return false;
        }

        // 判断 subject 的 contentDescription 是否匹配
        final CharSequence subjectContentDescription = subject.getContentDescription();
        if (null != matchElement.contentDescription &&
                !matchElement.contentDescription.equals(subjectContentDescription == null ? null : subjectContentDescription + "")) {
            return false;
        }

        // 判断 subject 的 tag 是否匹配
        final String matchTag = matchElement.tag;
        if (null != matchElement.tag) {
            final Object subjectTag = subject.getTag();
            if (null == subjectTag || !matchTag.equals(subject.getTag().toString())) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasClassName(Object subject, String className) {
        Class<?> klass = subject.getClass();
        while (true) {
            if (klass.getCanonicalName().equals(className)) {
                return true;
            }

            if (klass == Object.class) {
                return false;
            }

            klass = klass.getSuperclass();
        }
    }

    /**
     * Bargain-bin pool of integers, for use in avoiding allocations during path crawl
     */
    private static class IntStack {
        public IntStack() {
            mStack = new int[MAX_INDEX_STACK_SIZE];
            mStackSize = 0;
        }

        public boolean full() {
            return mStack.length == mStackSize;
        }

        /**
         * Pushes a new value, and returns the index you can use to increment and read that value later.
         */
        public int alloc() {
            final int index = mStackSize;
            mStackSize++;
            mStack[index] = 0;
            return index;
        }

        /**
         * Gets the value associated with index. index should be the result of a previous call to alloc()
         */
        public int read(int index) {
            return mStack[index];
        }

        public void increment(int index) {
            mStack[index]++;
        }

        /**
         * Should be matched to each call to alloc. Once free has been called, the key associated with the
         * matching alloc should be considered invalid.
         */
        public void free() {
            mStackSize--;
            if (mStackSize < 0) {
                throw new ArrayIndexOutOfBoundsException(mStackSize);
            }
        }

        private final int[] mStack;
        private int mStackSize;

        private static final int MAX_INDEX_STACK_SIZE = 256;
    }

}
