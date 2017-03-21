package io.sugo.sdkdemo.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.sugo.sdkdemo.R;

public class PrivateFuncActivity extends AppCompatActivity {

    @BindView(R.id.h_sv_btn)
    Button mHSVBtn;
    @BindView(R.id.horizontal_scroll_view)
    HorizontalScrollView mHorizontalScrollView;

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

}
