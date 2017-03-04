package me.owj.android_xwalkview_demo.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import io.sugo.android.mpmetrics.SugoAPI;
import me.owj.android_xwalkview_demo.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyFragment extends Fragment {

    private static final String ARG_TITLE = "title";

    private String mTitle;
    private TextView mTitleTxt;
    private Button mClickShowBtn;

    public MyFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param title Parameter 1.
     * @return A new instance of fragment MyFragment.
     */
    public static MyFragment newInstance(String title) {
        MyFragment fragment = new MyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(ARG_TITLE);
        }
        Log.i(mTitle, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(mTitle, "onCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my, container, false);
        mTitleTxt = (TextView) view.findViewById(R.id.frag_title_txt);
        mClickShowBtn = (Button) view.findViewById(R.id.click_me_btn);
        mClickShowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SugoAPI.getInstance(getContext()).track("Click Fragment Btn");
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(mTitle, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(mTitle, "onResume");
        mTitleTxt.setText(mTitle);
        SugoAPI.getInstance(getContext()).traceFragmentResumed(this, mTitle);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(mTitle, "onPause");
        SugoAPI.getInstance(getContext()).traceFragmentPaused(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(mTitle, "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(mTitle, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(mTitle, "onDestroy");
    }

}


