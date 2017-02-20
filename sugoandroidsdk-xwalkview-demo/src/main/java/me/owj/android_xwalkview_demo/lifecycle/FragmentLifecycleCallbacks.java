package me.owj.android_xwalkview_demo.lifecycle;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
* Project: AndroidLifeCycleCallback
* Created by LiaoKai(soarcn) on 14-3-17.
*/
public interface FragmentLifecycleCallbacks {
    void onFragmentCreated(Fragment fragment,
                           Bundle savedInstanceState);

    void onFragmentStarted(Fragment fragment);

    void onFragmentResumed(Fragment fragment);

    void onFragmentPaused(Fragment fragment);

    void onFragmentStopped(Fragment fragment);

    void onFragmentSaveInstanceState(Fragment fragment,
                                     Bundle outState);

    void onFragmentDestroyed(Fragment fragment);

    void onFragmentAttach(Fragment fragment,
                          Activity activity);

    void onFragmentDetach(Fragment fragment);

    void onFragmentActivityCreated(Fragment fragment,
                                   Bundle savedInstanceState);

    void onFragmentCreateView(Fragment fragment,
                              LayoutInflater inflater,
                              ViewGroup container,
                              Bundle savedInstanceState);

    void onFragmentViewCreated(Fragment fragment,
                               View view,
                               Bundle savedInstanceState);
}
