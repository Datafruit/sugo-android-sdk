package me.owj.android_xwalkview_demo;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import me.owj.android_xwalkview_demo.fragments.MyFragment;

public class NativePageActivity extends AppCompatActivity {

    private FrameLayout mContainer;

    private Fragment mF1Fragment;
    private Fragment mF2Fragment;
    private Fragment mF3Fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_page);

        mF1Fragment = MyFragment.newInstance("F1 ");
        mF2Fragment = MyFragment.newInstance("F2 ");
        mF3Fragment = MyFragment.newInstance("F3 ");

        findViewById(R.id.wxj_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(NativePageActivity.this, WXJActivity.class));
            }
        });

        findViewById(R.id.f1_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,mF1Fragment).commit();
            }
        });

        findViewById(R.id.f2_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,mF2Fragment).commit();
            }
        });

        findViewById(R.id.f3_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,mF3Fragment).commit();
            }
        });

        mContainer = (FrameLayout) findViewById(R.id.fragment_container);

    }


}
