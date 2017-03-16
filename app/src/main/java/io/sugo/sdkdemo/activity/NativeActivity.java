package io.sugo.sdkdemo.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.sugo.sdkdemo.R;

public class NativeActivity extends AppCompatActivity {

    @BindView(R.id.back_img)
    ImageView mBackImg;
    @BindView(R.id.a_btn)
    Button mABtn;
    @BindView(R.id.b_btn)
    Button mBBtn;
    @BindView(R.id.a_edit)
    EditText mAEdit;
    @BindView(R.id.b_edit)
    EditText mBEdit;
    @BindView(R.id.seek_bar)
    SeekBar mSeekBar;
    @BindView(R.id.compat_seek_bar)
    AppCompatSeekBar mCompatSeekBar;
    @BindView(R.id.a_swt)
    SwitchCompat mASwt;
    @BindView(R.id.b_swt)
    SwitchCompat mBSwt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native);
        ButterKnife.bind(this);


    }

    @OnClick({R.id.back_img, R.id.a_btn, R.id.b_btn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_img:
                onBackPressed();
                break;
            case R.id.a_btn:
                break;
            case R.id.b_btn:
                break;
        }
    }
}
