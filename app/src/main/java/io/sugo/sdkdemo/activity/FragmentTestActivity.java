package io.sugo.sdkdemo.activity;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.sugo.sdkdemo.R;

/**
 * @author Administrator
 */
public class FragmentTestActivity extends AppCompatActivity {

    @BindView(R.id.big_btn)
    Button bigBtn;

    @BindView(R.id.big2_btn)
    Button big2Btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_test);
        ButterKnife.bind(this);
    }


    @OnClick(R.id.big_btn)
    public void bigBtn() {
        Rect rect = new Rect();
        boolean visibility = bigBtn.getGlobalVisibleRect(rect);
        Log.i(this.getClass().getSimpleName(), visibility + "");

        Rect rect2 = new Rect();
        boolean visibility2 = big2Btn.getGlobalVisibleRect(rect2);
        Log.i(this.getClass().getSimpleName(), visibility2 + "");

        findViewById(R.id.second_layout).setVisibility(View.VISIBLE);
    }


    @OnClick(R.id.big2_btn)
    public void big2Btn() {

        Rect rect = new Rect();
        boolean visibility = bigBtn.getGlobalVisibleRect(rect);
        Log.i(this.getClass().getSimpleName(), visibility + "");

        Rect rect2 = new Rect();
        boolean visibility2 = big2Btn.getGlobalVisibleRect(rect2);
        Log.i(this.getClass().getSimpleName(), visibility2 + "");


        findViewById(R.id.second_layout).setVisibility(View.GONE);
    }


}
