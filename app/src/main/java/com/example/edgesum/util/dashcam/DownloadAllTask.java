package com.example.edgesum.util.dashcam;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.edgesum.event.AddEvent;
import com.example.edgesum.event.Type;
import com.example.edgesum.model.Video;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Consumer;

public class DownloadAllTask extends AsyncTask<DashName, Void, List<String>> {
    private static final String TAG = DownloadAllTask.class.getSimpleName();
    private final WeakReference<Context> weakReference;
    private final Consumer<Video> downloadCallback;

    public DownloadAllTask(Context context) {
        this.weakReference = new WeakReference<>(context);
        this.downloadCallback = (video) -> EventBus.getDefault().post(new AddEvent(video, Type.RAW));
    }

    @Override
    protected List<String> doInBackground(DashName... dashNames) {
        DashName name = dashNames[0];
        DashModel dash;

        switch (name) {
            case DRIDE:
                dash = DashModel.dride();
                break;
            case BLACKVUE:
                dash = DashModel.blackvue();
                break;
            default:
                Log.e(TAG, "Dashcam model not specified");
                return null;
        }
        return dash.downloadAll(downloadCallback, weakReference.get());
    }
}
