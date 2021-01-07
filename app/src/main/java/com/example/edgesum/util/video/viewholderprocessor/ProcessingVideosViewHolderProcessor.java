package com.example.edgesum.util.video.viewholderprocessor;

import android.content.Context;
import android.graphics.Color;

import com.example.edgesum.data.VideoViewModel;
import com.example.edgesum.page.main.VideoRecyclerViewAdapter;

public class ProcessingVideosViewHolderProcessor implements VideoViewHolderProcessor {
    @Override
    public void process(Context context, final VideoViewModel vm,  VideoRecyclerViewAdapter.VideoViewHolder viewHolder, int position) {
        disableFirstRowButton(viewHolder, position);

    }

    private void disableFirstRowButton(VideoRecyclerViewAdapter.VideoViewHolder viewHolder, int position) {
        if (position == 0) {
            disable(viewHolder);
        }
    }

    private void disable(VideoRecyclerViewAdapter.VideoViewHolder viewHolder) {
        viewHolder.view.setBackgroundColor(Color.parseColor("#e7eecc"));
        viewHolder.actionButton.setEnabled(false);
    }
}
