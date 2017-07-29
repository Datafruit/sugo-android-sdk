package io.sugo.sdkdemo.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.sdkdemo.R;

public class SendTestDataActivity extends AppCompatActivity {

    @BindView(R.id.send_count_txt)
    TextView mSendCountTxt;
    @BindView(R.id.send_freq_txt)
    TextView mSendFreqTxt;
    @BindView(R.id.freq_edit)
    EditText mFreqEdit;
    @BindView(R.id.confirm_btn)
    Button mConfirmBtn;

    private SugoAPI mSugoAPI;
    private Handler mHandler;

    public static int mSendCount = 0;
    public static int mSendFreq = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_test_data);
        ButterKnife.bind(this);

        mSugoAPI = SugoAPI.getInstance(this);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 666) {
                    mSendCountTxt.setText(String.format("已发送数据： %s 条", mSendCount));
                    mHandler.sendEmptyMessageDelayed(666, 1000);
                    for (int i = 0; i < mSendFreq; i++) {
                        mSugoAPI.track("AutoEvent");
                    }
                    mSugoAPI.flush();
                    mSendCount += mSendFreq;
                }
            }
        };
    }

    @OnClick(R.id.confirm_btn)
    public void onConfirmClicked() {
        try {
            String num = mFreqEdit.getText().toString();
            mSendFreqTxt.requestFocus();
            int number = Integer.parseInt(num);
            mSendFreq = number;

            mSendFreqTxt.setText(String.format("数据发送频率：每秒 %s 条", mSendFreq));
            mHandler.sendEmptyMessageDelayed(666, 1000);
            Toast.makeText(getApplicationContext(), "设置成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "数据填写有误", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }
}
