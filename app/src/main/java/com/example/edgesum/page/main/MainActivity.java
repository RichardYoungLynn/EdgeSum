package com.example.edgesum.page.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.eminayar.panter.DialogType;
import com.eminayar.panter.PanterDialog;
import com.eminayar.panter.enums.Animation;
import com.eminayar.panter.interfaces.OnSingleCallbackConfirmListener;
import com.example.edgesum.R;
import com.example.edgesum.data.VideosRepository;
import com.example.edgesum.model.Video;
import com.example.edgesum.page.authentication.AuthenticationActivity;
import com.example.edgesum.page.objectdetection.ObjectDetectionActivity;
import com.example.edgesum.page.setting.SettingsActivity;
import com.example.edgesum.util.Injection;
import com.example.edgesum.util.dashcam.DashName;
import com.example.edgesum.util.file.FileManager;
import com.example.edgesum.util.nearby.NearbyFragment;
import com.example.edgesum.util.dashcam.DownloadAllTask;
import com.example.edgesum.util.video.summariser.SummariserIntentService;
import com.example.edgesum.util.video.videoeventhandler.ProcessingVideosEventHandler;
import com.example.edgesum.util.video.videoeventhandler.RawFootageEventHandler;
import com.example.edgesum.util.video.videoeventhandler.SummarisedVideosEventHandler;
import com.example.edgesum.util.video.viewholderprocessor.ProcessingVideosViewHolderProcessor;
import com.example.edgesum.util.video.viewholderprocessor.RawFootageViewHolderProcessor;
import com.example.edgesum.util.video.viewholderprocessor.SummarisedVideosViewHolderProcessor;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements VideoFragment.OnListFragmentInteractionListener,
        NearbyFragment.OnFragmentInteractionListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private VideoFragment rawFootageFragment;
    private VideoFragment processingFragment;
    private VideoFragment summarisedVideoFragment;
    private ConnectionFragment connectionFragment;

    private final FragmentManager supportFragmentManager = getSupportFragmentManager();
    private Fragment activeFragment;

    private final BottomNavigationView.OnNavigationItemSelectedListener bottomNavigationOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_raw_footage:
                    Log.v(TAG, "Navigation raw button clicked");
                    showNewFragmentAndHideOldFragment(rawFootageFragment);
                    return true;
                case R.id.navigation_processing:
                    Log.v(TAG, "Navigation processing button clicked");
                    showNewFragmentAndHideOldFragment(processingFragment);
                    return true;
                case R.id.navigation_summarised_videos:
                    Log.v(TAG, "Navigation summarised button clicked");
                    showNewFragmentAndHideOldFragment(summarisedVideoFragment);
                    return true;
            }
            return false;
        }
    };

    private void showNewFragmentAndHideOldFragment(Fragment newFragment) {
        supportFragmentManager.beginTransaction().hide(activeFragment).show(newFragment).commit();
        setActiveFragment(newFragment);
    }

    private void setActiveFragment(Fragment newFragment) {
        activeFragment = newFragment;
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkPermissions();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanVideoDirectories();
        startAwsS3TransferService();

        // Set the toolbar as the app bar for this Activity.
        setToolBarAsTheAppBar();
        setUpBottomNavigation();
        setUpFragments();
        FileManager.initialiseDirectories();
    }

    private void scanVideoDirectories() {
        MediaScannerConnection.OnScanCompletedListener scanCompletedListener = (path, uri) ->
                Log.d(TAG, String.format("Scanned %s\n  -> uri=%s", path, uri));

        MediaScannerConnection.scanFile(this, new String[]{FileManager.getRawFootageDirPath()},
                null, scanCompletedListener);
        MediaScannerConnection.scanFile(this, new String[]{FileManager.getSummarisedDirPath()},
                null, scanCompletedListener);
    }

    private void startAwsS3TransferService() {
        getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));
    }

    private void setToolBarAsTheAppBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    private void setUpFragments() {
        VideosRepository rawFootageRepository = Injection.getExternalVideoRepository(this, "",
                FileManager.getRawFootageDirPath());
        VideosRepository processingVideosRepository = Injection.getProcessingVideosRespository(this);
        VideosRepository summarisedVideosRepository = Injection.getExternalVideoRepository(this, "",
                FileManager.getSummarisedDirPath());

        connectionFragment = ConnectionFragment.newInstance();
        SummariserIntentService.transferCallback = connectionFragment;
        rawFootageFragment = VideoFragment.newInstance(1, new RawFootageViewHolderProcessor(connectionFragment),
                ActionButton.ADD, new RawFootageEventHandler(rawFootageRepository), connectionFragment);
        processingFragment = VideoFragment.newInstance(1, new ProcessingVideosViewHolderProcessor(),
                ActionButton.REMOVE, new ProcessingVideosEventHandler(processingVideosRepository), connectionFragment);
        summarisedVideoFragment = VideoFragment.newInstance(1, new SummarisedVideosViewHolderProcessor(),
                ActionButton.UPLOAD, new SummarisedVideosEventHandler(summarisedVideosRepository), connectionFragment);

        supportFragmentManager.beginTransaction().add(R.id.main_container, connectionFragment, "4").hide(connectionFragment).commit();
        supportFragmentManager.beginTransaction().add(R.id.main_container, summarisedVideoFragment, "3").hide(summarisedVideoFragment).commit();
        supportFragmentManager.beginTransaction().add(R.id.main_container, processingFragment, "2").hide(processingFragment).commit();
        supportFragmentManager.beginTransaction().add(R.id.main_container, rawFootageFragment, "1").commit();

        rawFootageFragment.setRepository(rawFootageRepository);
        processingFragment.setRepository(processingVideosRepository);
        summarisedVideoFragment.setRepository(summarisedVideosRepository);

        setActiveFragment(rawFootageFragment);
    }

    private void setUpBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(bottomNavigationOnNavigationItemSelectedListener);
    }

    private void cleanVideoDirectories() {
        Context context = getApplicationContext();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        if (pref.getBoolean(getString(R.string.remove_raw_key), false)) {
            rawFootageFragment.cleanRepository(this);
        }
        processingFragment.cleanRepository(this);
        summarisedVideoFragment.cleanRepository(this);
        FileManager.cleanVideoDirectories(context);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // This method gets called when a option in the App bar gets selected.

        // Define logic on how to handle each option item selected.
        switch (item.getItemId()) {
            case R.id.action_connect:
                Log.v(TAG, "Connect button clicked");
                showNewFragmentAndHideOldFragment(connectionFragment);
                return true;
            case R.id.action_download:
                Log.v(TAG, "Download button clicked");
                Toast.makeText(this, "Starting download", Toast.LENGTH_SHORT).show();
                DownloadAllTask downloadAllTask = new DownloadAllTask(this);
                downloadAllTask.execute(DashName.BLACKVUE);
                return true;
            case R.id.action_clean:
                Log.v(TAG, "Clean button clicked");
                Toast.makeText(this, "Cleaning video directories", Toast.LENGTH_SHORT).show();
                cleanVideoDirectories();
                return true;
            case R.id.action_settings:
                Log.v(TAG, "Setting button clicked");
                goToSettingsActivity();
                return true;
            case R.id.action_logout:
                Log.v(TAG, "Logout button clicked");
                signOut();
                Intent i = new Intent(MainActivity.this, AuthenticationActivity.class);
                startActivity(i);
                finish();
                return true;
            case R.id.action_object_detection:
                Log.v(TAG, "Object detection button clicked");
                goToObjectDetectionActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        AWSMobileClient.getInstance().signOut();
    }

    private void goToSettingsActivity() {
        Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void goToObjectDetectionActivity() {
        new PanterDialog(this)
                .setHeaderBackground(R.drawable.pattern_bg_blue)
                .setDialogType(DialogType.SINGLECHOICE)
                .withAnimation(Animation.SLIDE)
                .items(R.array.object_detection_models, new OnSingleCallbackConfirmListener() {
                    @Override
                    public void onSingleCallbackConfirmed(PanterDialog dialog, int pos, String text) {
//                        Toast.makeText(MainActivity.this, "position : " + String.valueOf(pos) +
//                                        " value = " + text,
//                                Toast.LENGTH_LONG).show();
                        ObjectDetectionActivity.USE_MODEL = ObjectDetectionActivity.YOLOV5;
                        Intent objectDetectionIntent = new Intent(getApplicationContext(), ObjectDetectionActivity.class);
                        startActivity(objectDetectionIntent);
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onListFragmentInteraction(Video item) {

    }

    private void checkPermissions() {
        final int REQUEST_PERMISSIONS = 1;
        String[] PERMISSIONS = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
        };

        if (hasPermissions()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }

    private boolean hasPermissions(String... permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onFragmentInteraction(String name) {

    }
}
