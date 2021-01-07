package com.example.edgesum.util.video.viewholderprocessor;

import android.content.Context;

import com.example.edgesum.data.VideoViewModel;
import com.example.edgesum.page.main.VideoRecyclerViewAdapter;

public class NullVideoViewHolderProcess implements VideoViewHolderProcessor {
    @Override
    public void process(Context context, final VideoViewModel vm, VideoRecyclerViewAdapter.VideoViewHolder viewHolder, int position) {

    }
}
