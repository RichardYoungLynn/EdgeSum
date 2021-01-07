package com.example.edgesum.data;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.edgesum.model.Video;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessingVideosRepository implements VideosRepository {

    Context context;

    List<Video> videos = new ArrayList<>();
    MutableLiveData<List<Video>> result = new MutableLiveData<>();

    public ProcessingVideosRepository(Context context) {
        this.context = context;
    }

    @Override
    public MutableLiveData<List<Video>> getVideos() {
        List<Video> videos = new ArrayList<>();

        result.setValue(videos);
        return result;
    }

    @Override
    public void insert(Video video) {
        videos.add(video);
        result.postValue(videos);
    }

    @Override
    public void delete(int position) {
        videos.remove(position);
        result.postValue(videos);
    }

    @Override
    public void delete(String path) {
        videos = videos.stream().filter(e -> !e.getData().equalsIgnoreCase(path)).collect(Collectors.toList());
        result.postValue(videos);
    }

    @Override
    public void update(Video video, int position) {
        videos.set(position, video);
        result.postValue(videos);
    }
}
