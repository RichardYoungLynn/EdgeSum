package com.example.edgesum.util.video.summariser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

public class SummariserPrefs {
    public static final String NOISE_KEY = "noise";
    public static final String DURATION_KEY = "duration";
    public static final String QUALITY_KEY = "quality";
    public static final String ENCODING_SPEED_KEY = "speed";

    public final double noise;
    public final double duration;
    public final int quality;
    public final Speed speed;

    private SummariserPrefs(double noise, double duration, int quality, Speed speed) {
        this.noise = noise;
        this.duration = duration;
        this.quality = quality;
        this.speed = speed;
    }

    public SummariserPrefs(String prefString) {
        String[] prefs = prefString.split("_");

        this.noise = Double.parseDouble(prefs[0]);
        this.duration = Double.parseDouble(prefs[1]);
        this.quality = Integer.parseInt(prefs[2]);
        this.speed = Speed.valueOf(prefs[3]);
    }

    public static SummariserPrefs extractPreferences(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        double noise = pref.getInt(NOISE_KEY, (int) Summariser.DEFAULT_NOISE);
        double duration = pref.getInt(DURATION_KEY, (int) Summariser.DEFAULT_DURATION * 10) / 10.0;
        int quality = pref.getInt(QUALITY_KEY, Summariser.DEFAULT_QUALITY);
        Speed speed = Speed.valueOf(pref.getString(ENCODING_SPEED_KEY, Summariser.DEFAULT_SPEED.name()));

        return new SummariserPrefs(noise, duration, quality, speed);
    }

    static SummariserPrefs extractExtras(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras == null || !hasExtras(intent)) {
            return extractPreferences(context);
        }

        double noise = extras.getDouble(NOISE_KEY);
        double duration = extras.getDouble(DURATION_KEY);
        int quality = extras.getInt(QUALITY_KEY);
        Speed speed = Speed.valueOf(extras.getString(ENCODING_SPEED_KEY));

        return new SummariserPrefs(noise, duration, quality, speed);
    }

    private static boolean hasExtras(Intent intent) {
        return intent.hasExtra(NOISE_KEY) &&
                intent.hasExtra(DURATION_KEY) &&
                intent.hasExtra(QUALITY_KEY) &&
                intent.hasExtra(ENCODING_SPEED_KEY);
    }
}
