package com.example.edgesum.util.video.clouduploader;

import android.content.Context;

public interface CloudUploader {
    void upload(Context context, String videoPath);

    void uploadVideos(Context context);
}
