package com.example.edgesum.util.nearby;

import com.example.edgesum.model.Video;

public class Message {
    final Video video;
    final Command command;

    Message(Video video, Command command) {
        this.video = video;
        this.command = command;
    }

    public enum Command {
        ERROR, // Error during transfer
        SUMMARISE, // Summarise the transferred file
        SUMMARISE_SEGMENT, // Summarise the transferred file as a video segment
        COMPLETE, // Completed file transfer
        RETURN, // Returning summarised file
        NO_ACTIVITY
    }

    static boolean isSummarise(Command command) {
        return (command.equals(Command.SUMMARISE) || command.equals(Command.SUMMARISE_SEGMENT));
    }
}
