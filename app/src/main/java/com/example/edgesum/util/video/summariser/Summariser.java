package com.example.edgesum.util.video.summariser;

import android.util.Log;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.Level;
import com.example.edgesum.util.file.FileManager;
import com.example.edgesum.util.video.FfmpegTools;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.regex.Pattern;

class Summariser {
    private static final String TAG = Summariser.class.getSimpleName();
    static final float DEFAULT_NOISE = 60;
    static final float DEFAULT_DURATION = 2;
    static final int DEFAULT_QUALITY = 23;
    static final Speed DEFAULT_SPEED = Speed.medium;

    private final String inPath;
    private final double noise;
    private final double duration;
    private final int quality;
    private final Speed speed;
    private final String outPath;
    private final String freezeFilePath;

    static Summariser createSummariser(String inPath, double noise, double duration, int quality,
                                       Speed speed, String outPath) {
        return new Summariser(inPath, noise, duration, quality, speed, outPath);
    }

    private Summariser(String inPath, double noise, double duration, int quality, Speed speed, String outPath) {
        this.inPath = inPath;
        this.noise = noise;
        this.quality = quality;
        this.speed = speed;
        this.duration = duration;
        this.outPath = outPath;
        this.freezeFilePath = String.format("%s/%s.txt",
                FileManager.getRawFootageDirPath(), FilenameUtils.getBaseName(inPath));
    }

    /**
     * @return true if a summary video is created, false if no video is created
     */
    boolean summarise() {
        Instant start = Instant.now();
        Config.setLogLevel(Level.AV_LOG_WARNING);
        ArrayList<Double[]> activeTimes = getActiveTimes();

        if (activeTimes == null) {
            // Testing purposes: Video file is completely active, so just copy it
            try {
                Log.w(TAG, "Whole video is active");
                FileManager.copy(new File(inPath), new File(getOutPath()));
                return printResult(start, true, -1);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        int activeCount = activeTimes.size();

        if (activeCount == 0) {
            // Video file is completely inactive, so ignore it, don't copy it
            Log.w(TAG, "No activity detected");
            return printResult(start, false, 0);
        }

        // One or more active scenes found, extract them and combine them into a summarised video
        ArrayList<String> ffmpegArgs = getSummarisationArguments(activeTimes);
        Log.w(TAG, String.format("%d active sections found", activeCount));

        FfmpegTools.executeFfmpeg(ffmpegArgs);
        return printResult(start, true, activeCount);
    }

    private boolean printResult(Instant start, boolean isOutVid, int activeCount) {
        StringJoiner result = new StringJoiner("\n  ");

        result.add("Summarisation completed");
        result.add(String.format("filename: %s.%s",
                FilenameUtils.getBaseName(inPath), FilenameUtils.getExtension(inPath)));
        result.add(String.format(Locale.ENGLISH, "active sections: %d", activeCount));

        result.add(String.format("time: %ss",
                DurationFormatUtils.formatDuration(Duration.between(start, Instant.now()).toMillis(), "ss.SSS")));
        result.add(String.format(Locale.ENGLISH, "original duration: %.2f", FfmpegTools.getDuration(inPath)));

        double outDur = isOutVid ? FfmpegTools.getDuration(getOutPath()) : -1.0;
        result.add(String.format(Locale.ENGLISH, "summarised duration: %.2f", outDur));

        result.add(String.format(Locale.ENGLISH, "noise tolerance: %.2f", noise));
        result.add(String.format(Locale.ENGLISH, "quality: %d", quality));
        result.add(String.format("speed: %s", speed));
        result.add(String.format(Locale.ENGLISH, "freeze duration: %.2f", duration));

        Log.w(TAG, result.toString());
        return isOutVid;
    }

    // https://superuser.com/a/1230097/911563
    private ArrayList<String> getSummarisationArguments(ArrayList<Double[]> activeTimes) {
        ArrayList<String> ffmpegArgs = new ArrayList<>(Arrays.asList(
                "-y", // Skip prompts
                "-i", inPath,
                "-filter_complex"
        ));
        StringBuilder filter = new StringBuilder();

        for (int i = 0; i < activeTimes.size(); i++) {
            filter.append(String.format(Locale.ENGLISH,
                    "[0:v]trim=%1$f:%2$f,setpts=PTS-STARTPTS[v%3$d];" +
                            "[0:a]atrim=%1$f:%2$f,asetpts=PTS-STARTPTS[a%3$d];",
                    activeTimes.get(i)[0], activeTimes.get(i)[1], i));
        }
        for (int i = 0; i < activeTimes.size(); i++) {
            filter.append(String.format(Locale.ENGLISH, "[v%1$d][a%1$d]", i));
        }
        filter.append(String.format(Locale.ENGLISH, "concat=n=%d:v=1:a=1[outv][outa]", activeTimes.size()));

        ffmpegArgs.addAll(new ArrayList<>(Arrays.asList(
                filter.toString(),
                "-map", "[outv]",
                "-map", "[outa]",
                "-crf", Integer.toString(quality), // Set quality
                "-preset", speed.name(), // Set speed
                getOutPath()
        )));

        return ffmpegArgs;
    }

    private ArrayList<Double[]> getActiveTimes() {
        detectFreeze();
        File freeze = new File(freezeFilePath);

        if (!freeze.exists() || freeze.length() == 0) {  // No inactive sections
            Log.d(TAG, "No freeze file");
            return null;
        }
        Scanner sc = null;

        try {
            sc = new Scanner(freeze);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (sc == null) {
            Log.e(TAG, "Could not open freeze file");
            return null;
        }

        ArrayList<Double[]> activeTimes = new ArrayList<>();
        Pattern gotoEquals = Pattern.compile(".*=");

        sc.nextLine(); // Skip first line
        double start_time = sc.skip(gotoEquals).nextDouble();

        if (start_time != 0) { // Video starts active
            activeTimes.add(new Double[]{0.0, start_time});
        }
        while (sc.hasNextLine()) {
            String start_prefix = sc.findInLine("freeze_end");

            if (start_prefix != null) {
                Double[] times = new Double[2];
                sc.skip(gotoEquals);
                times[0] = sc.nextDouble();

                sc.nextLine(); // Go to next line

                if (sc.hasNextLine()) {
                    sc.nextLine(); // Skip line
                    times[1] = sc.skip(gotoEquals).nextDouble();
                    sc.nextLine();
                } else { // Active until end
                    times[1] = FfmpegTools.getDuration(inPath);
                }

                if (times[0] < times[1]) { // Make sure start time is before end time
                    activeTimes.add(times);
                }
            } else {
                sc.nextLine();
            }
        }
        return activeTimes;
    }

    private void detectFreeze() {
        FfmpegTools.executeFfmpeg(new ArrayList<>(Arrays.asList(
                "-y", // Skip prompts
                "-i", inPath,
                "-vf", String.format(Locale.ENGLISH,
                        "freezedetect=n=-%fdB:d=%f,metadata=mode=print:file=%s", noise, duration, freezeFilePath),
                "-f", "null", "-"
        )));
    }

    private String getOutPath() {
        if (outPath == null) {
            return String.format("%s-sum.%s",
                    FilenameUtils.getBaseName(inPath),
                    FilenameUtils.getExtension(inPath));
        } else {
            return outPath;
        }
    }
}
