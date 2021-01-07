package com.example.edgesum.util.dashcam;

import android.content.Context;
import android.util.Log;

import com.example.edgesum.model.Video;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

class DashModel {
    private static final String TAG = DashModel.class.getSimpleName();
    static final String drideBaseUrl = "http://192.168.1.254/DCIM/MOVIE/";
    static final String blackvueBaseUrl = "http://10.99.77.1/";
    static final String blackvueVideoUrl = blackvueBaseUrl + "Record/";

    private final DashName dashName;
    private final String baseUrl;
    private final String videoDirUrl;
    private final Supplier<List<String>> getFilenameFunc;

    private DashModel(DashName dashName, String baseUrl, String videoDirUrl, Supplier<List<String>> getFilenameFunc) {
        this.dashName = dashName;
        this.baseUrl = baseUrl;
        this.videoDirUrl = videoDirUrl;
        this.getFilenameFunc = getFilenameFunc;
    }

    static DashModel dride() {
        return new DashModel(DashName.DRIDE, drideBaseUrl, drideBaseUrl, DashTools::getDrideFilenames);
    }

    static DashModel blackvue() {
        return new DashModel(DashName.BLACKVUE, blackvueBaseUrl, blackvueVideoUrl, DashTools::getBlackvueFilenames);
    }

    List<String> downloadAll(Consumer<Video> downloadCallback, Context context) {
        List<String> allFiles = getFilenames();
        int last_n = 2;

        if (allFiles == null) {
            Log.e(TAG, "Dashcam file list is null");
            return null;
        }
        if (allFiles.size() < last_n) {
            Log.e(TAG, "Dashcam file list is smaller than expected");
            return null;
        }
        List<String> lastFiles = allFiles.subList(Math.max(allFiles.size(), 0) - last_n, allFiles.size());

        for (String filename : lastFiles) {
            downloadVideo(filename, DashDownloadManager.getInstance(downloadCallback), context);
        }
        return lastFiles;
    }

    void downloadVideo(String filename, DashDownloadManager downloadManager, Context context) {
        downloadManager.startDownload(String.format("%s%s", videoDirUrl, filename), context);
    }

    List<String> getFilenames() {
        return getFilenameFunc.get();
    }
}
