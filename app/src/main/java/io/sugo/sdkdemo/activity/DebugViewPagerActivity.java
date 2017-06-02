package io.sugo.sdkdemo.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import io.sugo.sdkdemo.R;
import io.sugo.sdkdemo.fragment.OneTextFragment;
import io.sugo.sdkdemo.view.NoSwipeViewpager;

public class DebugViewPagerActivity extends AppCompatActivity {

    private Button mChangePageBtn;

    List<Fragment> mFragments;
    NoSwipeViewpager mNoSwipeViewpager;
    private DebugViewPagerAdapter mAdapter;

    int curPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_view_pager);
        mNoSwipeViewpager = (NoSwipeViewpager) findViewById(R.id.no_swipe_view_pager);
        mChangePageBtn = (Button) findViewById(R.id.change_page_btn);

        mChangePageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                curPage = curPage + 1;
                if (curPage > 3) {
                    curPage = 0;
                }
                mNoSwipeViewpager.setCurrentItem(curPage);
            }
        });

        mFragments = new ArrayList<>();
        mFragments.add(OneTextFragment.newInstance("page 1"));
        mFragments.add(OneTextFragment.newInstance("page 2"));
        mFragments.add(OneTextFragment.newInstance("page 3"));
        mFragments.add(OneTextFragment.newInstance("page 4"));
        mAdapter = new DebugViewPagerAdapter(getSupportFragmentManager());
        mNoSwipeViewpager.setAdapter(mAdapter);
        mNoSwipeViewpager.setCurrentItem(curPage);
    }

    private class DebugViewPagerAdapter extends FragmentPagerAdapter {

        public DebugViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }
    }

}
