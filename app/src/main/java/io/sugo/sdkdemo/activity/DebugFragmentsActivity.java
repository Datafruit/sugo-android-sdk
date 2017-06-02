package io.sugo.sdkdemo.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.sugo.sdkdemo.R;
import io.sugo.sdkdemo.fragment.OneTextFragment;

/**
 * Created by Ouwenjie on 2017/6/1.
 */

public class DebugFragmentsActivity extends AppCompatActivity {

    @BindView(R.id.fragment_container)
    FrameLayout mFragmentContainer;
    @BindView(R.id.switch_fragment_btn)
    Button mSwitchFragmentBtn;

    OneTextFragment mOneTextFragment0;
    OneTextFragment mOneTextFragment1;
    OneTextFragment mOneTextFragment2;
    OneTextFragment mOneTextFragment3;
    OneTextFragment mOneTextFragment4;
    @BindView(R.id.fragment_container1)
    FrameLayout mFragmentContainer1;
    @BindView(R.id.fragment_container2)
    FrameLayout mFragmentContainer2;
    @BindView(R.id.fragment_container3)
    FrameLayout mFragmentContainer3;
    @BindView(R.id.fragment_container4)
    FrameLayout mFragmentContainer4;
    @BindView(R.id.show_all_fragment_btn)
    Button mShowAllFragmentBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_debug_fragments);
        ButterKnife.bind(this);

        mOneTextFragment0 = OneTextFragment.newInstance("00000");
        mOneTextFragment1 = OneTextFragment.newInstance("11111");
        mOneTextFragment2 = OneTextFragment.newInstance("22222");
        mOneTextFragment3 = OneTextFragment.newInstance("33333");
        mOneTextFragment4 = OneTextFragment.newInstance("44444");

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    int curIndex = 0;

    @OnClick(R.id.switch_fragment_btn)
    public void onSwitchClicked() {
        curIndex++;
        if (curIndex > 3) {
            curIndex = 0;
        }
        switch (curIndex) {
            case 0:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mOneTextFragment1).commit();
                break;
            case 1:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mOneTextFragment2).commit();
                break;
            case 2:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mOneTextFragment3).commit();
                break;
            case 3:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mOneTextFragment4).commit();
                break;
            default:
                break;
        }
    }

    boolean isShow = false;

    @OnClick(R.id.show_all_fragment_btn)
    public void onShowAllClicked() {
        if (isShow) {
            getSupportFragmentManager().beginTransaction().hide(mOneTextFragment1).commit();
            getSupportFragmentManager().beginTransaction().hide(mOneTextFragment2).commit();
            getSupportFragmentManager().beginTransaction().hide(mOneTextFragment3).commit();
            getSupportFragmentManager().beginTransaction().hide(mOneTextFragment4).commit();
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container1, mOneTextFragment1).commitAllowingStateLoss();
                            }
                        });
                        Thread.sleep(3000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container2, mOneTextFragment2).commitAllowingStateLoss();
                            }
                        });
                        Thread.sleep(3000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container3, mOneTextFragment3).commitAllowingStateLoss();
                            }
                        });
                        Thread.sleep(3000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container4, mOneTextFragment4).commitAllowingStateLoss();
                            }
                        });
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        isShow = !isShow;
    }

}
