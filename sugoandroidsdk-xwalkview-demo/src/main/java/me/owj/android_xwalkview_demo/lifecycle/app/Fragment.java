package me.owj.android_xwalkview_demo.lifecycle.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.owj.android_xwalkview_demo.lifecycle.LifecycleDispatcher;

/**
 * Project: AndroidLifeCycleCallback
 * Created by LiaoKai(soarcn) on 14-3-18.
 */
public class Fragment extends android.support.v4.app.Fragment {

    @Override
    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);
        LifecycleDispatcher.get().onFragmentAttach(this, activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LifecycleDispatcher.get().onFragmentDetach(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LifecycleDispatcher.get().onFragmentCreated(this, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        LifecycleDispatcher.get().onFragmentStarted(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        LifecycleDispatcher.get().onFragmentResumed(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        LifecycleDispatcher.get().onFragmentPaused(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        LifecycleDispatcher.get().onFragmentStopped(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LifecycleDispatcher.get().onFragmentDestroyed(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LifecycleDispatcher.get().onFragmentSaveInstanceState(this, outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LifecycleDispatcher.get().onFragmentActivityCreated(this, savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        LifecycleDispatcher.get().onFragmentCreateView(this, inflater, container, savedInstanceState);
        return view;
    }

    @Override
    public void onViewCreated(View view,
                              Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LifecycleDispatcher.get().onFragmentViewCreated(this, view, savedInstanceState);
    }
}
