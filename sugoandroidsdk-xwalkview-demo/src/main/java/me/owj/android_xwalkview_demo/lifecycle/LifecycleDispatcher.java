package me.owj.android_xwalkview_demo.lifecycle;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;


public class LifecycleDispatcher implements ActivityLifecycleCallbacksCompat, FragmentLifecycleCallbacks {

    private static final LifecycleDispatcher INSTANCE = new LifecycleDispatcher();

    public static LifecycleDispatcher get() {
        return INSTANCE;
    }

    private LifecycleDispatcher() {
    }


    private static final boolean PRE_ICS = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    /*
     * Register.
     */

    /**
     * Registers a callback to be called following the life cycle of the application's {@link Activity activities}.
     *
     * @param application The application with which to register the callback.
     * @param callback    The callback to register.
     */
    public static void registerActivityLifecycleCallbacks(Application application, ActivityLifecycleCallbacksCompat callback) {
        if (PRE_ICS) {
            preIcsRegisterActivityLifecycleCallbacks(callback);
        } else {
            postIcsRegisterActivityLifecycleCallbacks(application, callback);
        }
    }

    private static void preIcsRegisterActivityLifecycleCallbacks(ActivityLifecycleCallbacksCompat callback) {
        LifecycleDispatcher.get().registerActivityLifecycleCallbacks(callback);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static void postIcsRegisterActivityLifecycleCallbacks(Application application, ActivityLifecycleCallbacksCompat callback) {
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksWrapper(callback));
    }


    /**
     * Unregisters a previously registered callback.
     *
     * @param application The application with which to unregister the callback.
     * @param callback    The callback to unregister.
     */
    public static void unregisterActivityLifecycleCallbacks(Application application, ActivityLifecycleCallbacksCompat callback) {
        if (PRE_ICS) {
            preIcsUnregisterActivityLifecycleCallbacks(callback);
        } else {
            postIcsUnregisterActivityLifecycleCallbacks(application, callback);
        }
    }

    private static void preIcsUnregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacksCompat callback) {
        LifecycleDispatcher.get().unregisterActivityLifecycleCallbacks(callback);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static void postIcsUnregisterActivityLifecycleCallbacks(Application application, ActivityLifecycleCallbacksCompat callback) {
        application.unregisterActivityLifecycleCallbacks(new ActivityLifecycleCallbacksWrapper(callback));
    }

    /**
     * Registers a callback to be called following the life cycle of Fragments
     *
     * @param application The application with which to register the callback.
     * @param callback    The callback to register.
     */
    public static void registerFragmentLifecycleCallbacks(Application application, FragmentLifecycleCallbacks callback) {
        LifecycleDispatcher.get().registerFragmentLifecycleCallbacks(callback);
    }

    /**
     * Unregisters a previously registered callback.
     *
     * @param application The application with which to unregister the callback.
     * @param callback    The callback to unregister.
     */
    public static void unregisterFragmentLifecycleCallbacks(Application application, FragmentLifecycleCallbacks callback) {
        LifecycleDispatcher.get().unregisterFragmentLifecycleCallbacks(callback);
    }

    private List<ActivityLifecycleCallbacksCompat> mActivityLifecycleCallbacks = new ArrayList<>();

    private List<FragmentLifecycleCallbacks> mFragmentLifecycleCallbacks = new ArrayList<>();

    private void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacksCompat callback) {
        synchronized (mActivityLifecycleCallbacks) {
            mActivityLifecycleCallbacks.add(callback);
        }
    }

    private void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacksCompat callback) {
        synchronized (mActivityLifecycleCallbacks) {
            mActivityLifecycleCallbacks.remove(callback);
        }
    }

    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks callback) {
        synchronized (mFragmentLifecycleCallbacks) {
            mFragmentLifecycleCallbacks.add(callback);
        }
    }

    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks callback) {
        synchronized (mFragmentLifecycleCallbacks) {
            mFragmentLifecycleCallbacks.remove(callback);
        }
    }

    private Object[] collectActivityLifecycleCallbacks() {
        Object[] callbacks = null;
        synchronized (mActivityLifecycleCallbacks) {
            if (mActivityLifecycleCallbacks.size() > 0) {
                callbacks = mActivityLifecycleCallbacks.toArray();
            }
        }
        return callbacks;
    }

    private Object[] collectFragmentLifecycleCallbacks() {
        Object[] callbacks = null;
        synchronized (mFragmentLifecycleCallbacks) {
            if (mFragmentLifecycleCallbacks.size() > 0) {
                callbacks = mFragmentLifecycleCallbacks.toArray();
            }
        }
        return callbacks;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (!PRE_ICS)
            return;
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object callback : callbacks) {
                ((ActivityLifecycleCallbacksCompat) callback).onActivityCreated(activity, savedInstanceState);
            }
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (!PRE_ICS)
            return;
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object callback : callbacks) {
                ((ActivityLifecycleCallbacksCompat) callback).onActivityStarted(activity);
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (!PRE_ICS)
            return;
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object callback : callbacks) {
                ((ActivityLifecycleCallbacksCompat) callback).onActivityResumed(activity);
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!PRE_ICS)
            return;
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object callback : callbacks) {
                ((ActivityLifecycleCallbacksCompat) callback).onActivityPaused(activity);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (!PRE_ICS)
            return;
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object callback : callbacks) {
                ((ActivityLifecycleCallbacksCompat) callback).onActivityStopped(activity);
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        if (!PRE_ICS)
            return;
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object callback : callbacks) {
                ((ActivityLifecycleCallbacksCompat) callback).onActivitySaveInstanceState(activity, outState);
            }
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (!PRE_ICS)
            return;
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object callback : callbacks) {
                ((ActivityLifecycleCallbacksCompat) callback).onActivityDestroyed(activity);
            }
        }
    }

    public void onFragmentActivityCreated(Fragment fragment,
                                          Bundle savedInstanceState) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentActivityCreated(fragment, savedInstanceState);
            }
        }
    }

    public void onFragmentAttach(Fragment fragment,
                                 Activity activity) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentAttach(fragment, activity);
            }
        }
    }

    public void onFragmentDetach(Fragment fragment) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentDetach(fragment);
            }
        }
    }

    public void onFragmentCreated(Fragment fragment,
                                  Bundle savedInstanceState) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentCreated(fragment, savedInstanceState);
            }
        }
    }

    public void onFragmentStarted(Fragment fragment) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentStarted(fragment);
            }
        }
    }

    public void onFragmentResumed(Fragment fragment) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentResumed(fragment);
            }
        }
    }

    public void onFragmentPaused(Fragment fragment) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentPaused(fragment);
            }
        }
    }

    public void onFragmentStopped(Fragment fragment) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentStopped(fragment);
            }
        }
    }

    public void onFragmentSaveInstanceState(Fragment fragment,
                                            Bundle outState) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentSaveInstanceState(fragment, outState);
            }
        }
    }

    public void onFragmentDestroyed(Fragment fragment) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentDestroyed(fragment);
            }
        }
    }

    public void onFragmentCreateView(Fragment fragment,
                                     LayoutInflater inflater,
                                     ViewGroup container,
                                     Bundle savedInstanceState) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentCreateView(fragment, inflater, container, savedInstanceState);
            }
        }
    }

    public void onFragmentViewCreated(Fragment fragment,
                                      View view,
                                      Bundle savedInstanceState) {
        Object[] callbacks = collectFragmentLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((FragmentLifecycleCallbacks) callbacks[i]).onFragmentViewCreated(fragment, view, savedInstanceState);
            }
        }
    }
}
