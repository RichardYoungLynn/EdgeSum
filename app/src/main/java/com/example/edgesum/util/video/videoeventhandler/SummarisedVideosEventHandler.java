package com.example.edgesum.util.video.videoeventhandler;

import android.util.Log;

import com.example.edgesum.data.VideosRepository;
import com.example.edgesum.event.AddEvent;
import com.example.edgesum.event.RemoveByPathEvent;
import com.example.edgesum.event.RemoveEvent;
import com.example.edgesum.event.Type;

import org.greenrobot.eventbus.Subscribe;

public class SummarisedVideosEventHandler implements VideoEventHandler {

    private String TAG = SummarisedVideosEventHandler.class.getSimpleName();

    public VideosRepository repository;

    public SummarisedVideosEventHandler(VideosRepository repository) {
        this.repository = repository;
    }

    @Subscribe
    @Override
    public void onAdd(AddEvent event) {
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
            return;
        }

        if (event.type == Type.SUMMARISED) {
            Log.v(TAG, "onAdd");
            try {
                repository.insert(event.video);
            } catch (Exception e) {
                Log.e(TAG, String.format("onAdd error: \n%s", e.getMessage()));
            }
        }
    }

    @Subscribe
    public void onRemoveByPath(RemoveByPathEvent event) {
        if (event == null) {
            Log.e(TAG, "Null event");
            return;
        }
        if (repository == null) {
            Log.e(TAG, "Null repository");
            return;
        }
        if (event.path == null) {
            Log.e(TAG, "Null video");
            return;
        }

        if (event.type == Type.SUMMARISED) {
            Log.v(TAG, "onRemoveByPath");
            try {
                repository.delete(event.path);
            } catch (Exception e) {
                Log.e(TAG, String.format("onRemoveByPath error: \n%s", e.getMessage()));
            }
        }
    }

    @Subscribe
    @Override
    public void onRemove(RemoveEvent event) {

    }
}
