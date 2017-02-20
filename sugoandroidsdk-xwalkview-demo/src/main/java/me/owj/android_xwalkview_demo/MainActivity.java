package me.owj.android_xwalkview_demo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.android.xwalk.SugoXWalkViewSupport;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 10086;

    SugoAPI mSugoAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSugoAPI = SugoAPI.getInstance(this);
        SugoXWalkViewSupport.enableXWalkView(mSugoAPI, true);

        findViewById(R.id.one_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NativePageActivity.class));
            }
        });

        findViewById(R.id.two_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, WebActivity.class));
            }
        });

        findViewById(R.id.three_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, XWalkViewActivity.class));
            }
        });

        getDeviceId();

        findViewById(R.id.scan_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityResult  :", "requestCode : " + requestCode + "");
        if (requestCode == REQUEST_CODE) {
            //处理扫描结果（在界面上显示）
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    Log.d("解析结果:", result);
                    if (mSugoAPI != null) {
                        Uri uri = Uri.parse(result);
                        mSugoAPI.connectEditor(uri);
                    }
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Log.d("解析结果:", "解析二维码失败");
                }
            }
        }
    }

    public String getDeviceId() {
        String deviceId = null;
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            deviceId = tm.getDeviceId();
            Log.e("getDeviceId:", deviceId);
            if (TextUtils.isEmpty(deviceId)) {
                WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                String mac = wifi.getConnectionInfo().getMacAddress();
                deviceId = mac;
            }
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deviceId;
    }

}
