package com.example.edgesum.util.video.videoeventhandler;

import com.example.edgesum.event.AddEvent;
import com.example.edgesum.event.RemoveEvent;

import org.greenrobot.eventbus.Subscribe;

public class NullVideoEventHandler implements VideoEventHandler {

    @Subscribe
    @Override
    public void onAdd(AddEvent event) {

    }

    @Override
    public void onRemove(RemoveEvent event) {

    }
}
