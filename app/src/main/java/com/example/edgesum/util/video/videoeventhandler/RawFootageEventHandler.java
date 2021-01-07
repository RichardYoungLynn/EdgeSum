package com.example.edgesum.util.video.videoeventhandler;

import android.util.Log;

import com.example.edgesum.data.VideosRepository;
import com.example.edgesum.event.AddEvent;
import com.example.edgesum.event.RemoveByNameEvent;
import com.example.edgesum.event.RemoveEvent;
import com.example.edgesum.event.Type;
import com.example.edgesum.event.VideoEvent;
import com.example.edgesum.util.file.FileManager;

import org.greenrobot.eventbus.Subscribe;

public class RawFootageEventHandler implements VideoEventHandler {

    private String TAG = RawFootageEventHandler.class.getSimpleName();

    public VideosRepository repository;

    public RawFootageEventHandler(VideosRepository repository) {

        this.repository = repository;
    }

    @Subscribe
    @Override
    public void onAdd(AddEvent event) {
        nullCheck(event);

        if (event.type == Type.RAW) {
            Log.v(TAG, "onAdd");
            try {
                repository.insert(event.video);
            } catch (Exception e) {
                Log.e(TAG, String.format("onAdd error: \n%s", e.getMessage()));
            }
        }
    }

    @Subscribe
    @Override
    public void onRemove(RemoveEvent event) {
        nullCheck(event);

        if (event.type == Type.RAW) {
            Log.v(TAG, "onRemove");
            try {
                repository.delete(event.video.getData());
            } catch (Exception e) {
                Log.e(TAG, String.format("onRemove error: \n%s", e.getMessage()));
            }
        }
    }

    @Subscribe
    public void removeByName(RemoveByNameEvent event) {
        if (event == null) {
            Log.e(TAG, "Null event");
            return;
        }
        if (repository == null) {
            Log.e(TAG, "Null repository");
            return;
        }
        if (event.name == null) {
            Log.e(TAG, "Null name");
        }

        if (event.type == Type.RAW) {
            Log.v(TAG, "removeByName");
            try {
                String path = String.format("%s/%s", FileManager.getRawFootageDirPath(), event.name);
                repository.delete(path);
            } catch (Exception e) {
                Log.e(TAG, String.format("removeByName error: \n%s", e.getMessage()));
            }
        }
    }

    public void nullCheck(VideoEvent event) {
        if (event == null) {
            Log.e(TAG, "Null event");
            return;
        }
        if (repository == null) {
            Log.e(TAG, "Null repository");
            return;
        }
        if (event.video == null) {
            Log.e(TAG, "Null video");
        }
    }
}
