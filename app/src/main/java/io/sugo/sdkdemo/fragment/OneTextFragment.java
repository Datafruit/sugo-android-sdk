package io.sugo.sdkdemo.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.sugo.android.metrics.SugoAPI;
import io.sugo.sdkdemo.R;

/**
 * A fragment with a Google +1 button.
 * Use the {@link OneTextFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * @author going
 */
public class OneTextFragment extends Fragment {

    private static final String ARG_PARAM = "param";

    @BindView(R.id.test_txt)
    TextView mTestTxt;

    private Unbinder unbinder;
    private String mParam;

    public OneTextFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment OneTextFragment.
     */
    public static OneTextFragment newInstance(String param) {
        OneTextFragment fragment = new OneTextFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM, param);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam = getArguments().getString(ARG_PARAM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_plus_one, container, false);

        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTestTxt.setText(mParam);
        SugoAPI.getInstance(getContext()).traceFragmentResumed(this, mParam);
    }

    @Override
    public void onPause() {
        super.onPause();
        SugoAPI.getInstance(getContext()).traceFragmentPaused(this, mParam);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            SugoAPI.getInstance(getContext()).traceFragmentPaused(this, mParam);
        } else {
            SugoAPI.getInstance(getContext()).traceFragmentResumed(this, mParam);
        }
    }

    @OnClick(R.id.test_txt)
    public void onViewClicked() {
    }
}
