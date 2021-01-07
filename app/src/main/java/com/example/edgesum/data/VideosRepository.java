package com.example.edgesum.data;

import androidx.lifecycle.LiveData;

import com.example.edgesum.model.Video;

import java.util.List;

/**
 * Contract for the data store of videos.
 */
public interface VideosRepository {

    LiveData<List<Video>> getVideos();

    void insert(Video video);

    void delete(int position);

    void delete(String path);

    void update(Video video, int position);
}
