package io.sugo.sdkdemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.sugo.sdkdemo.R;

public class PrivateFuncActivity extends AppCompatActivity {

    @BindView(R.id.h_sv_btn)
    Button mHSVBtn;
    @BindView(R.id.horizontal_scroll_view)
    HorizontalScrollView mHorizontalScrollView;
    @BindView(R.id.back_img)
    ImageView mBackImg;
    @BindView(R.id.to_view_pager_btn)
    Button mToViewPagerBtn;
    @BindView(R.id.to_fragments_btn)
    Button mToFragmentsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_func);
        ButterKnife.bind(this);

    }


    @OnClick(R.id.h_sv_btn)
    public void onClickHSV() {
        if (mHorizontalScrollView.isShown()) {
            mHorizontalScrollView.setVisibility(View.GONE);
        } else {
            mHorizontalScrollView.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.back_img)
    public void onMBackImgClicked() {
        onBackPressed();
    }

    @OnClick(R.id.to_view_pager_btn)
    public void onMToViewPagerBtnClicked() {
        startActivity(new Intent(this, DebugViewPagerActivity.class));
    }


    @OnClick(R.id.to_fragments_btn)
    public void onViewClicked() {
        startActivity(new Intent(this, DebugFragmentsActivity.class));
    }
}
