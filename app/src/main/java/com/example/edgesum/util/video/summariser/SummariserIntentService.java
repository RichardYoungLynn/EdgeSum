package com.example.edgesum.util.video.summariser;

import android.app.IntentService;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.edgesum.event.AddEvent;
import com.example.edgesum.event.RemoveEvent;
import com.example.edgesum.event.Type;
import com.example.edgesum.model.Video;
import com.example.edgesum.util.file.FileManager;
import com.example.edgesum.util.nearby.Message;
import com.example.edgesum.util.nearby.TransferCallback;
import com.example.edgesum.util.video.VideoManager;

import org.greenrobot.eventbus.EventBus;

public class SummariserIntentService extends IntentService {
    private static final String TAG = SummariserIntentService.class.getSimpleName();

    public static final String VIDEO_KEY = "video";
    public static final String OUTPUT_KEY = "outputPath";
    public static final String TYPE_KEY = "type";
    public static final String LOCAL_TYPE = "local";
    public static final String NETWORK_TYPE = "network";
    public static final String SEND_VIDEO_KEY = "sendVideo";

    public static TransferCallback transferCallback;

    public SummariserIntentService() {
        super("SummariserIntentService");
    }

    public SummariserIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.v(TAG, "onHandleIntent");

        if (intent == null) {
            Log.e(TAG, "Null intent");
            return;
        }
        Video video = intent.getParcelableExtra(VIDEO_KEY);
        String output = intent.getStringExtra(OUTPUT_KEY);
        String type = intent.getStringExtra(TYPE_KEY);
        boolean sendVideo = intent.getBooleanExtra(SEND_VIDEO_KEY, true);

        if (video == null) {
            Log.e(TAG, "Video not specified");
            return;
        }
        if (output == null) {
            Log.e(TAG, "Output not specified");
            return;
        }
        if (type == null) {
            type = LOCAL_TYPE;
        }
        Log.d(TAG, video.toString());
        Log.d(TAG, output);

        SummariserPrefs prefs = SummariserPrefs.extractExtras(this, intent);
        Summariser summariser = Summariser.createSummariser(video.getData(),
                prefs.noise, prefs.duration, prefs.quality, prefs.speed, output);
        boolean isVideo = summariser.summarise();

        if (isVideo) {
            Video sumVid = VideoManager.getVideoFromPath(getApplicationContext(), output);

            if (sumVid.getData().contains(FileManager.getSummarisedDirPath())) {
                EventBus.getDefault().post(new AddEvent(sumVid, Type.SUMMARISED));
            }
            if (sendVideo && type.equals(NETWORK_TYPE)) {
                transferCallback.returnVideo(sumVid);
            }
        } else if (sendVideo && type.equals(NETWORK_TYPE)) {
            transferCallback.sendCommandMessageToAll(Message.Command.NO_ACTIVITY, video.getName());
        }
        if (!sendVideo && type.equals(NETWORK_TYPE)) {
            transferCallback.handleSegment(video.getName());
        }
        EventBus.getDefault().post(new RemoveEvent(video, Type.PROCESSING));

        MediaScannerConnection.scanFile(getApplicationContext(),
                new String[]{FileManager.getSummarisedDirPath()}, null, (path, uri) -> {
                    Log.d(TAG, String.format("Scanned %s\n  -> uri=%s", path, uri));
                    // Delete raw video
//                    File rawFootageVideoPath = new File(video.getData());
//                    rawFootageVideoPath.delete();
                });
    }
}
