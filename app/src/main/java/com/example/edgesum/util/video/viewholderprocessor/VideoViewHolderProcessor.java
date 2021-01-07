package com.example.edgesum.util.video.viewholderprocessor;

import android.content.Context;

import com.example.edgesum.data.VideoViewModel;
import com.example.edgesum.page.main.VideoRecyclerViewAdapter;

public interface VideoViewHolderProcessor {


    void process(Context context, VideoViewModel vm, VideoRecyclerViewAdapter.VideoViewHolder viewHolder, int position);

}
