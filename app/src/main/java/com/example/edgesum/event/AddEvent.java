package com.example.edgesum.event;

import com.example.edgesum.model.Video;

public class AddEvent extends VideoEvent {

    public AddEvent(Video video, Type type) {
        super(video, type);
    }
}
