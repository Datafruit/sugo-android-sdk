package io.sugo.android.reactnative;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.facebook.react.ReactRootView;
import com.facebook.react.views.view.ReactViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.sugo.android.metrics.SugoAPI;
import io.sugo.android.viewcrawler.ViewCrawler;

public class RNLifecycleCallbacks implements Application.ActivityLifecycleCallbacks{
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        final Activity finalActivity = activity;
        View rootView = activity.getWindow().getDecorView();
        ReactRootView reactRootView = getReactRootView(rootView);

        reactRootView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener(){

            @Override
            public void onChildViewAdded(View parent, View child) {
                ViewCrawler viewCrawler = (ViewCrawler) SugoAPI.getInstance(finalActivity.getApplicationContext()).getmUpdatesFromSugo();
                viewCrawler.getmBindingState().add(finalActivity);
                if(child instanceof ReactViewGroup){
                    ReactViewGroup reactViewGroup = (ReactViewGroup) child;
                    int childCount = reactViewGroup.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View view = reactViewGroup.getChildAt(i);
                        view.setOnTouchListener(new View.OnTouchListener(){
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN){
                                    v.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                                }
                                return true;
                            }
                        });
                    }
                }

            }

            @Override
            public void onChildViewRemoved(View parent, View child) {

            }
        });

    }

    private ReactRootView getReactRootView(View view){
        if (view instanceof ReactRootView)
            return (ReactRootView) view;

        if (view instanceof ViewGroup) {
            ViewGroup vp = (ViewGroup) view;
            for (int i = 0; i < vp.getChildCount(); i++) {
                View viewchild = vp.getChildAt(i);
                ReactRootView found = getReactRootView(viewchild);
                if (found != null)
                    return found;
            }
        }
        return null;
    }


    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
