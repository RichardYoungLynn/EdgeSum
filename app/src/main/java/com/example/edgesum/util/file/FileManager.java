package com.example.edgesum.util.file;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.example.edgesum.R;
import com.example.edgesum.util.devicestorage.DeviceExternalStorage;
import com.example.edgesum.util.video.FfmpegTools;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class FileManager {
    private static final String TAG = FileManager.class.getSimpleName();

    public static final String VIDEO_EXTENSION = "mp4";
    public static final String RAW_DIR_NAME = "raw";
    private static final String SUMMARISED_DIR_NAME = "summarised";
    private static final String NEARBY_DIR_NAME = "nearby";
    private static final String SEGMENT_DIR_NAME = "segment";
    private static final String SEGMENT_SUM_DIR_NAME = String.format("%s-sum", SEGMENT_DIR_NAME);

    private static final File MOVIE_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
    private static final File DOWN_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private static final File RAW_DIR = new File(MOVIE_DIR, RAW_DIR_NAME);
    private static final File SUMMARISED_DIR = new File(MOVIE_DIR, SUMMARISED_DIR_NAME);
    private static final File NEARBY_DIR = new File(DOWN_DIR, NEARBY_DIR_NAME);
    private static final File SEGMENT_DIR = new File(MOVIE_DIR, SEGMENT_DIR_NAME);
    private static final File SEGMENT_SUM_DIR = new File(MOVIE_DIR, SEGMENT_SUM_DIR_NAME);

    private static final List<File> DIRS = Arrays.asList(
            RAW_DIR, SUMMARISED_DIR, NEARBY_DIR, SEGMENT_DIR, SEGMENT_SUM_DIR);

    public static String getRawFootageDirPath() {
        return RAW_DIR.getAbsolutePath();
    }

    public static String getSummarisedDirPath() {
        return SUMMARISED_DIR.getAbsolutePath();
    }

    public static String getNearbyDirPath() {
        return NEARBY_DIR.getAbsolutePath();
    }

    public static String getSegmentDirPath() {
        return SEGMENT_DIR.getAbsolutePath();
    }

    public static String getSegmentDirPath(String subDir) {
        return makeDirectory(SEGMENT_DIR, subDir).getAbsolutePath();
    }

    public static String getSegmentSumDirPath() {
        return SEGMENT_SUM_DIR.getAbsolutePath();
    }

    public static String getSegmentSumDirPath(String subDir) {
        return makeDirectory(SEGMENT_SUM_DIR, subDir).getAbsolutePath();
    }

    public static String getSegmentSumSubDirPath(String videoName) {
        String baseVideoName = FfmpegTools.getBaseName(videoName);
        return makeDirectory(SEGMENT_SUM_DIR, baseVideoName).getAbsolutePath();
    }

    public static void initialiseDirectories() {
        for (File dir : DIRS) {
            makeDirectory(dir);
        }
    }

    private static File makeDirectory(File dirPath) {
        if (DeviceExternalStorage.externalStorageIsWritable()) {
            Log.v(TAG, "External storage is readable");
            try {
                if (!dirPath.exists()) {
                    if (dirPath.mkdirs()) {
                        Log.v(TAG, String.format("Created new directory: %s", dirPath));
                        return dirPath;
                    } else {
                        Log.e(TAG, String.format("Failed to create new directory: %s", dirPath));
                    }
                } else {
                    Log.v(TAG, String.format("Directory already exists: %s", dirPath));
                    return dirPath;
                }
            } catch (SecurityException e) {
                Log.e(TAG, "makeDirectory error: \n%s");
            }
        } else {
            Log.e(TAG, "External storage is not readable");
        }
        return null;
    }

    private static File makeDirectory(File dir, String subDirName) {
        return makeDirectory(new File(dir, subDirName));
    }

    public static void cleanVideoDirectories(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        List<File> dirs = pref.getBoolean(context.getString(R.string.remove_raw_key), false) ?
                DIRS :
                Arrays.asList(SUMMARISED_DIR, NEARBY_DIR, SEGMENT_DIR, SEGMENT_SUM_DIR);

        for (File dir : dirs) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                Log.e(TAG, String.format("Failed to delete %s", dir.getAbsolutePath()));
                Log.e(TAG, "cleanVideoDirectories error: \n%s");
            }
        }
    }

    // https://stackoverflow.com/a/9293885/8031185
    public static void copy(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source)) {
            try (OutputStream out = new FileOutputStream(dest)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public static boolean isMp4(String filename) {
        int extensionStartIndex = filename.lastIndexOf('.') + 1;
        return filename.regionMatches(true, extensionStartIndex, VIDEO_EXTENSION, 0, VIDEO_EXTENSION.length());
    }

    public static String getFilenameFromPath(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }
}
