package com.example.edgesum.page.main;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.edgesum.R;

import static com.example.edgesum.util.video.VidDownloading.getLastVideos;

public class DownloadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        getLastVideos();
    }
}
