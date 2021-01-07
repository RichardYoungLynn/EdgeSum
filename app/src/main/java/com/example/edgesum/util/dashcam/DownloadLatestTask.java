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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class DownloadLatestTask implements Runnable {
    private static final String TAG = DownloadLatestTask.class.getSimpleName();
    private final WeakReference<Context> weakReference;
    private final Consumer<Video> downloadCallback;
    private static final Set<String> downloadedVideos = new HashSet<>();

    public DownloadLatestTask(TransferCallback transferCallback, Context context) {
        this.weakReference = new WeakReference<>(context);

        this.downloadCallback = (video) -> {
            synchronized (this) {
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
                this.notify();
            }
        };
    }

    @Override
    public void run() {
        Log.v(TAG, "Starting DownloadLatestTask");
        DashModel dash = DashModel.blackvue();
        List<String> allVideos = dash.getFilenames();

        if (allVideos == null || allVideos.size() == 0) {
            Log.e(TAG, "Couldn't download videos");
            return;
        }
        List<String> newVideos = new ArrayList<>(CollectionUtils.disjunction(allVideos, downloadedVideos));
        newVideos.sort(Comparator.comparing(String::toString));

        if (newVideos.size() != 0) {
            // Get oldest new video
            String toDownload = newVideos.get(0);
            downloadedVideos.add(toDownload);
            Context context = weakReference.get();
            DashDownloadManager downloadManager = DashDownloadManager.getInstance(downloadCallback);

            dash.downloadVideo(toDownload, downloadManager, context);
        } else {
            Log.d(TAG, "No new videos");
        }

        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
