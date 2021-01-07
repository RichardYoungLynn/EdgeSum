package com.example.edgesum.event;

import com.example.edgesum.model.Video;

public class RemoveEvent extends VideoEvent {

    public RemoveEvent(Video video, Type type) {
        super(video, type);
    }
}
