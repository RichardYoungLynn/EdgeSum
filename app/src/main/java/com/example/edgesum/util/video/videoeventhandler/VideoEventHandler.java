package com.example.edgesum.util.video.videoeventhandler;

import com.example.edgesum.event.AddEvent;
import com.example.edgesum.event.RemoveEvent;

import org.greenrobot.eventbus.Subscribe;

public interface VideoEventHandler {

    void onAdd(AddEvent event);

    void onRemove(RemoveEvent event);
}
