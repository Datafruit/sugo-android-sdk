package io.sugo.sdkdemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.sugo.android.mpmetrics.SugoAPI;
import io.sugo.sdkdemo.R;

public class NativeActivity extends AppCompatActivity {

    @BindView(R.id.back_img)
    ImageView mBackImg;
    @BindView(R.id.txt_list)
    RecyclerView mTxtList;
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

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mTxtList.setLayoutManager(linearLayoutManager);
        mTxtList.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                TextView textView = new TextView(NativeActivity.this);
                textView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                textView.setClickable(true);
                textView.setBackgroundResource(R.drawable.bg_txt_click);
                textView.setPadding(24, 24, 24, 24);
                return new RecyclerView.ViewHolder(textView) {
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ((TextView) holder.itemView).setText("文字 " + position);
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });

    }

    @OnClick({R.id.back_img, R.id.a_btn, R.id.b_btn, R.id.to_fragments_btn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_img:
                onBackPressed();
                break;
            case R.id.a_btn:
                String userId = mAEdit.getText().toString();
                if (TextUtils.isEmpty(userId)) {
                    userId = "userId123";
                }
                SugoAPI.getInstance(getApplicationContext()).login("test_user_id", userId);
                break;
            case R.id.b_btn:
                SugoAPI.getInstance(getApplicationContext()).logout();
                break;
            case R.id.to_fragments_btn:
                startActivity(new Intent(NativeActivity.this, FragmentTestActivity.class));
                break;
            default:
                break;
        }
    }
}
