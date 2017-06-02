package io.sugo.sdkdemo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import io.sugo.sdkdemo.R;


/**
 * A ViewPager which can not swip left or right.
 *
 * @author hehu
 * @version 1.0 2016/3/19
 */
public class NoSwipeViewpager extends ViewPager {
    boolean isSwipeEnable = false;

    public NoSwipeViewpager(Context context) {
        super(context);
    }

    public NoSwipeViewpager(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = getResources().obtainAttributes(attrs, R.styleable.NoSwipeViewpager);
        isSwipeEnable = a.getBoolean(R.styleable.NoSwipeViewpager_isSwipeEnable, false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (isSwipeEnable)
            //let parent object ViewPager do touch work.
            return super.onTouchEvent(ev);
        else
            //Have not tell super to do anything,only return true or false.
            return true;
    }

    //屏蔽滑动动画
    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item, false);
    }

    /**
     * 调用此方法可保留滑动动画
     *
     * @param item
     * @param smoothScroll
     */
    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(item, smoothScroll);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
}
