package com.example.edgesum.page.viewuploadedvideos.ui.viewuploadedvideos;

import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.edgesum.R;


public class ViewUploadedVideosFragment extends Fragment {

    private ViewUploadedVideosViewModel mViewModel;

    public static ViewUploadedVideosFragment newInstance() {
        return new ViewUploadedVideosFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_uploaded_videos_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ViewUploadedVideosViewModel.class);
        // TODO: Use the ViewModel
    }

}
