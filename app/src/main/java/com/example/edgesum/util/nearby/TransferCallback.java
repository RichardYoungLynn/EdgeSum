package com.example.edgesum.util.nearby;

import com.example.edgesum.model.Video;

public interface TransferCallback {
    void addVideo(Video video);

    void returnVideo(Video video);

    void nextTransfer();

    void sendCommandMessageToAll(Message.Command command, String filename);

    void sendCommandMessage(Message.Command command, String filename, String toEndpoint);

    void stopDashDownload();

    boolean isConnected();

    void printPreferences(boolean autoDown);

    void handleSegment(String videoName);
}
