package com.example.edgesum.util.video.viewholderprocessor;

import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;

import com.example.edgesum.data.VideoViewModel;
import com.example.edgesum.model.Video;
import com.example.edgesum.page.main.VideoRecyclerViewAdapter;
import com.example.edgesum.util.video.clouduploader.S3Uploader;

public class SummarisedVideosViewHolderProcessor implements VideoViewHolderProcessor {
    @Override
    public void process(Context context, VideoViewModel vm, VideoRecyclerViewAdapter.VideoViewHolder holder, int pos) {
        holder.actionButton.setOnClickListener(view -> {
            final Video video = holder.video;
            final String path = video.getData();

            S3Uploader s3 = new S3Uploader();
            s3.upload(context, path);

            Toast.makeText(context, String.format("Added %s to upload queue", video.getName()), Toast.LENGTH_SHORT).show();
        });

        if (!holder.video.isVisible()) {
            holder.view.setBackgroundColor(Color.parseColor("#e7eecc"));
            holder.actionButton.setEnabled(false);
        }
    }
}
