package com.example.edgesum.page.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.edgesum.R;
import com.example.edgesum.page.viewuploadedvideos.ViewUploadedVideosActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final String TAG = SettingsActivity.class.getSimpleName();
        private static final String VIEW_SUMMARISED_VIDEO = "view_summarised_video";
        Context context;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference videoSummarisedVideoPreference = findPreference(VIEW_SUMMARISED_VIDEO);
            FragmentActivity activity = getActivity();

            if (activity != null) {
                context = activity.getApplicationContext();
            }
            if (videoSummarisedVideoPreference != null) {
                videoSummarisedVideoPreference.setOnPreferenceClickListener(preference -> {
                    Log.d(TAG, "View summarised videos");
                    Intent i = new Intent(getContext(), ViewUploadedVideosActivity.class);
                    startActivity(i);
                    return false;
                });
            }
        }
    }
}