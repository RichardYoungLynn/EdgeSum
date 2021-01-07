package com.example.edgesum.util.dashcam;

import android.util.Log;

import com.example.edgesum.util.file.FileManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DashTools {
    private static final String TAG = DashTools.class.getSimpleName();

    static List<String> getDrideFilenames() {
        Document doc = null;

        try {
            doc = Jsoup.connect(DashModel.drideBaseUrl).get();
        } catch (SocketTimeoutException | ConnectException e) {
            Log.e(TAG, "Could not connect to dashcam");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> allFiles = new ArrayList<>();

        if (doc == null) {
            Log.e(TAG, "Couldn't parse dashcam web-page");
            return null;
        }

        for (Element file : doc.select("td:eq(0) > a")) {
            if (FileManager.isMp4(file.text())) {
                allFiles.add(file.text());
            }
        }
        allFiles.sort(Comparator.comparing(String::toString));
        return allFiles;
    }

    static List<String> getBlackvueFilenames() {
        Document doc = null;

        try {
            doc = Jsoup.connect(DashModel.blackvueBaseUrl + "blackvue_vod.cgi").get();
        } catch (SocketTimeoutException | ConnectException e) {
            Log.e(TAG, "Could not connect to dashcam");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> allFiles = new ArrayList<>();

        if (doc == null) {
            Log.e(TAG, "Couldn't parse dashcam web-page");
            return null;
        }

        String raw = doc.select("body").text();
        Pattern pat = Pattern.compile(Pattern.quote("Record/") + "(.*?)" + Pattern.quote(",s:"));
        Matcher match = pat.matcher(raw);

        while (match.find()) {
            allFiles.add(match.group(1));
        }

        allFiles.sort(Comparator.comparing(String::toString));
        return allFiles;
    }
}
