package com.example.android.intentplayground;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.android.intentplayground.BaseActivity.Mode;
import com.example.android.intentplayground.IntentBuilderView.OnLaunchCallback;

public class LaunchFragment extends Fragment {
    private IntentBuilderView mIntentBuilderView;
    private BaseActivityViewModel mViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mIntentBuilderView = new IntentBuilderView(getContext(), Mode.LAUNCH);
        setOnLaunchCallBack();
        return mIntentBuilderView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mViewModel = (new ViewModelProvider(getActivity(),
                new ViewModelProvider.NewInstanceFactory())).get(BaseActivityViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.actOnFab(BaseActivityViewModel.FabAction.Hide);
    }

    private void setOnLaunchCallBack() {
        FragmentActivity activity = this.getActivity();
        if (activity instanceof OnLaunchCallback) {
            mIntentBuilderView.setOnLaunchCallback((OnLaunchCallback) activity);
        }
    }
}
