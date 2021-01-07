package com.example.edgesum.util.dashcam;

import android.content.Context;
import android.util.Log;

import com.example.edgesum.model.Video;
import com.example.edgesum.util.file.FileManager;
import com.example.edgesum.util.video.VideoManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

public class DashDownloadManager {
    private static final String TAG = DashDownloadManager.class.getSimpleName();
    private static DashDownloadManager manager = null;
    private static Consumer<Video> downloadCallback;

    private DashDownloadManager(Consumer<Video> callback) {
        downloadCallback = callback;
    }

    static DashDownloadManager getInstance(Consumer<Video> callback) {
        if (manager == null) {
            manager = new DashDownloadManager(callback);
        }
        return manager;
    }

    void startDownload(String url, Context context) {
        String filename = FileManager.getFilenameFromPath(url);
        String filePath = String.format("%s/%s", FileManager.getRawFootageDirPath(), filename);
        Log.v(TAG, String.format("Started downloading: %s", filename));
        Instant start = Instant.now();

        try {
            FileUtils.copyURLToFile(new URL(url), new File(filePath));
        } catch (IOException e) {
            Log.e(TAG, String.format("Download error: \n%s", e.getMessage()));
            return;
        }
        long duration = Duration.between(start, Instant.now()).toMillis();
        String time = DurationFormatUtils.formatDuration(duration, "ss.SSS");

        Video video = VideoManager.getVideoFromPath(context, filePath);
        Log.w(TAG, String.format("Successfully downloaded %s in %ss", filename, time));
        downloadCallback.accept(video);
    }
}
