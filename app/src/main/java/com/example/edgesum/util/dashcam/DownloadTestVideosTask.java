package com.example.edgesum.util.dashcam;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.edgesum.event.AddEvent;
import com.example.edgesum.event.Type;
import com.example.edgesum.model.Video;
import com.example.edgesum.util.file.FileManager;
import com.example.edgesum.util.nearby.TransferCallback;
import com.example.edgesum.util.video.summariser.SummariserIntentService;

import org.apache.commons.collections4.CollectionUtils;
import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class DownloadTestVideosTask implements Runnable {
    private static final String TAG = DownloadTestVideosTask.class.getSimpleName();
    private static final Set<String> startedDownloads = new HashSet<>();
    private static final ArrayList<String> completedDownloads = new ArrayList<>();
    private final WeakReference<Context> weakReference;
    private final Consumer<Video> downloadCallback;
    private final WeakReference<TransferCallback> transferCallback;
    private static final ArrayList<String> testVideos = new ArrayList<>(Arrays.asList(
            "20200312_150643.mp4",
            "20200312_150744.mp4",
            "20200312_150844.mp4",
            "20200312_150944.mp4",
            "20200312_151044.mp4",
            "20200312_193528.mp4",
            "20200312_193628.mp4",
            "20200312_193728.mp4",
            "20200312_193828.mp4",
            "20200312_193928.mp4",
            "20200312_194129.mp4",
            "20200312_194229.mp4",
            "20200312_194330.mp4",
            "20200312_194730.mp4",
            "20200312_194830.mp4",
            "20200312_194930.mp4",
            "20200312_195230.mp4",
            "20200312_195330.mp4",
            "20200312_195430.mp4"
    ));

    /* Active videos
      20200312_150744.mp4
      20200312_150844.mp4
      20200312_150944.mp4
      20200312_193628.mp4
      20200312_193728.mp4
      20200312_193828.mp4
      20200312_194129.mp4
      20200312_194229.mp4
      20200312_194830.mp4
      20200312_194930.mp4
      20200312_195230.mp4
      20200312_195330.mp4
      20200312_195430.mp4
    */

    public DownloadTestVideosTask(TransferCallback transferCallback, Context context) {
        this.weakReference = new WeakReference<>(context);
        this.transferCallback = new WeakReference<>(transferCallback);

        this.downloadCallback = (video) -> {
            if (video == null) {
                Log.e(TAG, "Video is null");
                return;
            }
            if (transferCallback == null) {
                Log.e(TAG, "transferCallback is null");
                return;
            }
            Log.v(TAG, String.format("Entering callback for download completion of %s", video.getName()));
            completedDownloads.add(video.getName());

            if (transferCallback.isConnected()) {
                EventBus.getDefault().post(new AddEvent(video, Type.RAW));
                transferCallback.addVideo(video);
                transferCallback.nextTransfer();
            } else {
                EventBus.getDefault().post(new AddEvent(video, Type.PROCESSING));

                final String output = String.format("%s/%s", FileManager.getSummarisedDirPath(), video.getName());
                Intent summariseIntent = new Intent(context, SummariserIntentService.class);
                summariseIntent.putExtra(SummariserIntentService.VIDEO_KEY, video);
                summariseIntent.putExtra(SummariserIntentService.OUTPUT_KEY, output);
                summariseIntent.putExtra(SummariserIntentService.TYPE_KEY, SummariserIntentService.LOCAL_TYPE);
                context.startService(summariseIntent);
            }
        };
    }

    @Override
    public void run() {
        Log.v(TAG, "Starting DownloadTestVideosTask");
        DashModel dash = DashModel.blackvue();
        //List<String> allVideos = dash.getFilenames();
        List<String> allVideos = testVideos;

        //noinspection ConstantConditions
        if (allVideos == null || allVideos.size() == 0) {
            Log.e(TAG, "Couldn't download videos");
            return;
        }
        List<String> newVideos = new ArrayList<>(CollectionUtils.disjunction(allVideos, startedDownloads));
        newVideos.sort(Comparator.comparing(String::toString));

        if (newVideos.size() != 0) {
            // Get oldest new video, allVideos should already be sorted
            String toDownload = newVideos.get(0);
            startedDownloads.add(toDownload);
            Context context = weakReference.get();
            DashDownloadManager downloadManager = DashDownloadManager.getInstance(downloadCallback);

            dash.downloadVideo(toDownload, downloadManager, context);
        } else {
            Log.d(TAG, "No new videos");

            if (completedDownloads.size() == testVideos.size()) {
                transferCallback.get().stopDashDownload();
            }
        }
    }
}
