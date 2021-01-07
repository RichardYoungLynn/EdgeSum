package com.example.edgesum.util;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.edgesum.data.ExternalStorageVideosRepository;
import com.example.edgesum.data.ProcessingVideosRepository;
import com.example.edgesum.data.VideoViewModel;
import com.example.edgesum.data.VideoViewModelFactory;
import com.example.edgesum.data.VideosRepository;

public abstract class Injection {

    public static VideosRepository getExternalVideoRepository(Context context, String tag, String path) {
        return new ExternalStorageVideosRepository(context, tag, path);
    }

    public static VideosRepository getProcessingVideosRespository(Context context) {
        return new ProcessingVideosRepository(context);
    }

//    public static VideoViewModel getExternalVideosVideoViewModel(Fragment fragment, String tag, String path) {
//        return ViewModelProviders.of(fragment,
//                new VideoViewModelFactory(
//                        fragment.getActivity().getApplication(),
//                        null)
//        )
//                .get(VideoViewModel.class);
//    }

//    public static VideoViewModel getExternalVideosVideoViewModel(Activity fragment, String tag, String path) {
//        return ViewModelProviders.of(fragment,
//                new VideoViewModelFactory(
//                        fragment.getApplication(),
//                        null)
//        )
//                .get(VideoViewModel.class);
//    }

}
