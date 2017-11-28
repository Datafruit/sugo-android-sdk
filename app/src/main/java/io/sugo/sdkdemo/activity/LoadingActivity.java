package io.sugo.sdkdemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.sugo.android.mpmetrics.SGConfig;
import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.sdkdemo.R;

/**
 * @author Administrator
 */
public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        SugoAPI.startSugo(this, SGConfig.getInstance(this).logConfig());

    }


    @Override
    protected void onResume() {
        super.onResume();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}
