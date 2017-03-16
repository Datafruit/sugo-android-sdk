package io.sugo.sdkdemo.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.sugo.android.mpmetrics.SGConfig;
import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.sdkdemo.R;

public class MainActivity extends AppCompatActivity {

    private static final int SCAN_REQUEST_CODE = 1020;

    @BindView(R.id.shuoming_img)
    ImageView mShuomingImg;
    @BindView(R.id.shuoming_txt)
    TextView mShuomingTxt;
    @BindView(R.id.scan_img)
    ImageView mScanImg;
    @BindView(R.id.to_native_img)
    ImageView mToNativeImg;
    @BindView(R.id.to_native_txt)
    TextView mToNativeTxt;
    @BindView(R.id.to_web_img)
    ImageView mToWebImg;
    @BindView(R.id.to_web_txt)
    TextView mToWebTxt;
    @BindView(R.id.activity_main)
    LinearLayout mActivityMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        SugoAPI.startSugo(this, SGConfig.getInstance(this)
                .setToken("123")
                .setProjectId("321")
                .logConfig());

    }

    @OnClick({R.id.shuoming_img, R.id.shuoming_txt, R.id.scan_img,
            R.id.to_native_img, R.id.to_native_txt,
            R.id.to_web_img, R.id.to_web_txt})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.shuoming_img:
            case R.id.shuoming_txt:

                break;
            case R.id.scan_img:
                openQRCodeScan();
                break;
            case R.id.to_native_img:
            case R.id.to_native_txt:
                startActivity(new Intent(this, NativeActivity.class));
                break;
            case R.id.to_web_img:
            case R.id.to_web_txt:
                startActivity(new Intent(this, WebActivity.class));
                break;
        }
    }

    private void openQRCodeScan() {
        new IntentIntegrator(this).setOrientationLocked(true).initiateScan(); // `this` is the current Activity
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String content = result.getContents();
            if (!TextUtils.isEmpty(content)) {
                SugoAPI.getInstance(MainActivity.this).connectEditor(Uri.parse(content));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


}
