package com.example.edgesum.util.nearby;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.collection.SimpleArrayMap;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.edgesum.R;
import com.example.edgesum.event.AddEvent;
import com.example.edgesum.event.RemoveByNameEvent;
import com.example.edgesum.event.RemoveEvent;
import com.example.edgesum.event.Type;
import com.example.edgesum.model.Video;
import com.example.edgesum.util.dashcam.DownloadTestVideosTask;
import com.example.edgesum.util.file.FileManager;
import com.example.edgesum.util.nearby.Message.Command;
import com.example.edgesum.util.video.FfmpegTools;
import com.example.edgesum.util.video.VideoManager;
import com.example.edgesum.util.video.summariser.SummariserIntentService;
import com.example.edgesum.util.video.summariser.SummariserPrefs;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class NearbyFragment extends Fragment implements DeviceCallback, TransferCallback {
    private static final String TAG = NearbyFragment.class.getSimpleName();
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String SERVICE_ID = "com.example.edgesum";
    private static final String LOCAL_NAME_KEY = "LOCAL_NAME";
    private static final Algorithm DEFAULT_ALGORITHM = Algorithm.best;
    private final PayloadCallback payloadCallback = new ReceiveFilePayloadCallback();
    private final Queue<Message> transferQueue = new LinkedList<>();
    private final Queue<Endpoint> endpointQueue = new LinkedList<>();
    private final LinkedHashMap<String, LinkedHashMap<String, Video>> videoSegments = new LinkedHashMap<>();
    // Dashcam isn't able to handle concurrent downloads, leads to a very high rate of download errors.
    // Just use a single thread for downloading
    private final ScheduledExecutorService downloadTaskExecutor = Executors.newSingleThreadScheduledExecutor();

    // https://stackoverflow.com/questions/36351417/how-to-inflate-hashmapstring-listitems-into-the-recyclerview
    // https://stackoverflow.com/questions/50809619/on-the-adapter-class-how-to-get-key-and-value-from-hashmap
    // https://stackoverflow.com/questions/38142819/make-a-list-of-hashmap-type-in-recycler-view-adapter
    private final LinkedHashMap<String, Endpoint> discoveredEndpoints = new LinkedHashMap<>();
    private ConnectionsClient connectionsClient;
    protected DeviceListAdapter deviceAdapter;
    protected String localName = null;

    private int transferCount = 0;
    private OnFragmentInteractionListener listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        discoveredEndpoints.add(new Endpoint("testing1", "testing1", true));
//        discoveredEndpoints.add(new Endpoint("testing2", "testing2", false));
        deviceAdapter = new DeviceListAdapter(getContext(), discoveredEndpoints, this);

        Context context = getContext();
        if (context != null) {
            connectionsClient = Nearby.getConnectionsClient(context);
            setLocalName(context);
        }
    }

//    移除对 Build.serial 的直接访问（杨云朝修改）
    private String getUUID() {
        String serial = null;
        String m_szDevIDShort = "35" +
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10; //13 位
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                serial = android.os.Build.getSerial();
            } else {
                serial = Build.SERIAL;
            }
//API>=9 使用serial号
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception exception) {
//serial需要一个初始化
            serial = "serial"; // 随便一个初始化
        }
//使用硬件信息拼凑出来的15位号码
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

    private void setLocalName(Context context) {
        if (localName != null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("MissingPermission")
            String sn = getUUID();
            localName = String.format("%s [%s]", Build.MODEL, sn.substring(sn.length() - 4));
        } else {
            SharedPreferences sharedPrefs = context.getSharedPreferences(LOCAL_NAME_KEY, Context.MODE_PRIVATE);
            String uniqueId = sharedPrefs.getString(LOCAL_NAME_KEY, null);

            if (uniqueId == null) {
                uniqueId = RandomStringUtils.randomAlphanumeric(8);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(LOCAL_NAME_KEY, uniqueId);
                editor.apply();
            }
            localName = String.format("%s [%s]", Build.MODEL, uniqueId);
        }
    }

//    private void setLocalName(Context context) {
//        if (localName != null) {
//            return;
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            @SuppressLint("MissingPermission")
//            String sn = Build.getSerial();
//            localName = String.format("%s [%s]", Build.MODEL, sn.substring(sn.length() - 4));
//        } else {
//            SharedPreferences sharedPrefs = context.getSharedPreferences(LOCAL_NAME_KEY, Context.MODE_PRIVATE);
//            String uniqueId = sharedPrefs.getString(LOCAL_NAME_KEY, null);
//
//            if (uniqueId == null) {
//                uniqueId = RandomStringUtils.randomAlphanumeric(8);
//                SharedPreferences.Editor editor = sharedPrefs.edit();
//                editor.putString(LOCAL_NAME_KEY, uniqueId);
//                editor.apply();
//            }
//            localName = String.format("%s [%s]", Build.MODEL, uniqueId);
//        }
//    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
                    Log.d(TAG, String.format("Found endpoint %s: %s", endpointId, info.getEndpointName()));

                    if (!discoveredEndpoints.containsKey(endpointId)) {
                        discoveredEndpoints.put(endpointId, new Endpoint(endpointId, info.getEndpointName()));
                        deviceAdapter.notifyItemInserted(discoveredEndpoints.size() - 1);
                    }
                }

                @Override
                public void onEndpointLost(@NonNull String endpointId) {
                    // A previously discovered endpoint has gone away.
                    Log.d(TAG, String.format("Lost endpoint %s", discoveredEndpoints.get(endpointId)));

                    if (discoveredEndpoints.containsKey(endpointId)) {
                        discoveredEndpoints.remove(endpointId);
                        deviceAdapter.notifyDataSetChanged();
                    }
                }
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
                    Log.d(TAG, String.format("Initiated connection with %s: %s",
                            endpointId, connectionInfo.getEndpointName()));

                    if (!discoveredEndpoints.containsKey(endpointId)) {
                        discoveredEndpoints.put(endpointId, new Endpoint(endpointId, connectionInfo.getEndpointName()));
                        deviceAdapter.notifyItemInserted(discoveredEndpoints.size() - 1);
                    }

                    Context context = getContext();
                    if (context != null) {
                        new AlertDialog.Builder(context)
                                .setTitle("Accept connection to " + connectionInfo.getEndpointName())
                                .setMessage("Confirm the code matches on both devices: " + connectionInfo.getAuthenticationToken())
                                .setPositiveButton(android.R.string.ok,
                                        (DialogInterface dialog, int which) ->
                                                // The user confirmed, so we can accept the connection.
                                                connectionsClient.acceptConnection(endpointId, payloadCallback))
                                .setNegativeButton(android.R.string.cancel,
                                        (DialogInterface dialog, int which) ->
                                                // The user canceled, so we should reject the connection.
                                                connectionsClient.rejectConnection(endpointId))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            Endpoint endpoint = discoveredEndpoints.get(endpointId);
                            Log.i(TAG, String.format("Connected to %s", endpoint));

                            if (endpoint != null) {
                                endpoint.connected = true;
                                deviceAdapter.notifyDataSetChanged();
                            }
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            Log.i(TAG, String.format("Connection rejected by %s", discoveredEndpoints.get(endpointId)));
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            Log.e(TAG, "Connection error");
                            break;
                        default:
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be sent or received.
                    Endpoint endpoint = discoveredEndpoints.get(endpointId);
                    Log.d(TAG, String.format("Disconnected from %s", endpoint));

                    if (endpoint != null) {
                        endpoint.connected = false;
                        deviceAdapter.notifyDataSetChanged();
                    }
                }
            };

    protected void startAdvertising() {
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startAdvertising(localName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener((Void unused) ->
                        Log.d(TAG, "Started advertising"))
                .addOnFailureListener((Exception e) ->
                        Log.e(TAG, String.format("Advertisement failure: \n%s", e.getMessage())));
    }

    protected void startDiscovery() {
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener((Void unused) ->
                        Log.d(TAG, "Started discovering"))
                .addOnFailureListener((Exception e) ->
                        Log.e(TAG, String.format("Discovery failure: \n%s", e.getMessage())));
    }

    protected void stopAdvertising() {
        Log.d(TAG, "Stopped advertising");
        connectionsClient.stopAdvertising();
    }

    protected void stopDiscovery() {
        Log.d(TAG, "Stopped discovering");
        connectionsClient.stopDiscovery();
    }

    // https://stackoverflow.com/a/11944965/8031185
    protected void startDashDownload() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "No context");
            return;
        }
        int defaultDelay = 1;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int delay = pref.getInt(getString(R.string.download_delay_key), defaultDelay);

        printPreferences(true);
        Log.w(TAG, String.format("Download delay: %ds", delay));
        Log.w(TAG, "Started downloading from dashcam");

        downloadTaskExecutor.scheduleWithFixedDelay(new DownloadTestVideosTask(
                this, context), 0, delay, TimeUnit.SECONDS);
    }

    public void stopDashDownload() {
        Log.w(TAG, "Stopped downloading from dashcam");
        downloadTaskExecutor.shutdown();
    }

    private List<Endpoint> getConnectedEndpoints() {
        return discoveredEndpoints.values().stream().filter(e -> e.connected).collect(Collectors.toList());
    }

    private int getConnectedCount() {
        // Should never come close to INT_MAX endpoints, but count() returns a long, should be fine to cast down to int
        return (int) discoveredEndpoints.values().stream().filter(e -> e.connected).count();
    }

    private void queueVideo(Video video, Command command) {
        transferQueue.add(new Message(video, command));
    }

    @Override
    public void addVideo(Video video) {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "No context");
            return;
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean segmentationEnabled = pref.getBoolean(getString(R.string.enable_segment_key), false);
        boolean autoSegmentation = pref.getBoolean(getString(R.string.auto_segment_key), false);
        int segNum = autoSegmentation ?
                getConnectedCount() :
                pref.getInt(getString(R.string.manual_segment_key), -1);

        if (segmentationEnabled) {
            if (segNum > 2) {
                int segCount = splitAndQueue(video.getData(), segNum);
                if (segCount != segNum) {
                    Log.w(TAG, String.format("Number of segmented videos (%d) does not match intended value (%d)",
                            segCount, segNum));
                }
                return;
            } else {
                Log.i(TAG, String.format("Segmentation count too low (%d), just summarizing whole video instead",
                        segNum));
            }
        }
        queueVideo(video, Command.SUMMARISE);
    }

    @Override
    public void returnVideo(Video video) {
        List<Endpoint> endpoints = getConnectedEndpoints();
        Message message = new Message(video, Command.RETURN);

        // Workers should only have a single connection to the master endpoint
        if (endpoints.size() == 1) {
            sendFile(message, endpoints.get(0));
        } else {
            Log.e(TAG, "Non-worker attempting to return a video");
        }
    }

    @Override
    public void printPreferences(boolean autoDown) {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "No context");
            return;
        }
        StringJoiner prefMessage = new StringJoiner("\n  ");
        prefMessage.add("Preferences:");

        // Add segmentation prefs to SummariserPrefs?
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String algorithmKey = getString(R.string.scheduling_algorithm_key);
        Algorithm selectedAlgorithm = Algorithm.valueOf(pref.getString(algorithmKey, DEFAULT_ALGORITHM.name()));
        boolean fastScheduling = pref.getBoolean(getString(R.string.fast_scheduling_key), false);
        boolean segmentationEnabled = pref.getBoolean(getString(R.string.enable_segment_key), false);
        boolean autoSegmentation = pref.getBoolean(getString(R.string.auto_segment_key), false);
        int segNum = autoSegmentation ?
                getConnectedCount() :
                pref.getInt(getString(R.string.manual_segment_key), -1);
        SummariserPrefs sumPref = SummariserPrefs.extractPreferences(context);

        prefMessage.add(String.format("Auto download: %s", autoDown));
        prefMessage.add(String.format("Algorithm: %s", selectedAlgorithm.name()));
        prefMessage.add(String.format("Fast scheduling: %s", fastScheduling));
        prefMessage.add(String.format("Segmentation: %s", segmentationEnabled));
        prefMessage.add(String.format("Auto segmentation: %s", autoSegmentation));
        prefMessage.add(String.format("Segment number: %s", segNum));
        prefMessage.add(String.format(Locale.ENGLISH, "Noise tolerance: %.2f", sumPref.noise));
        prefMessage.add(String.format(Locale.ENGLISH, "Quality: %d", sumPref.quality));
        prefMessage.add(String.format("Speed: %s", sumPref.speed));
        prefMessage.add(String.format(Locale.ENGLISH, "Freeze duration: %.2f", sumPref.duration));

        Log.w(TAG, prefMessage.toString());
    }

    private int splitAndQueue(String videoPath, int segNum) {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "No context");
            return -1;
        }

        String baseVideoName = FilenameUtils.getBaseName(videoPath);
        List<Video> videos = FfmpegTools.splitAndReturn(context, videoPath, segNum);

        if (videos == null || videos.size() == 0) {
            Log.e(TAG, String.format("Could not split %s", baseVideoName));
            return -1;
        }
        for (Video segment : videos) {
            queueVideo(segment, Command.SUMMARISE_SEGMENT);
            queueVideoSegment(baseVideoName, segment);
        }
        return videos.size();
    }

    // Add segments when sending, remove segments when receiving. Empty list means all segments received.
    private void queueVideoSegment(String baseName, Video segment) {
        LinkedHashMap<String, Video> segmentMap = videoSegments.get(baseName);

        if (segmentMap == null) {
            segmentMap = new LinkedHashMap<>();
            videoSegments.put(baseName, segmentMap);
        }

        segmentMap.put(segment.getName(), segment);
    }

    private void transferToAllEndpoints() {
        List<Endpoint> connectedEndpoints = getConnectedEndpoints();

        if (connectedEndpoints.size() == 0) {
            Log.e(TAG, "Not connected to any devices");
            return;
        }

        for (Endpoint toEndpoint : connectedEndpoints) {
            if (transferQueue.isEmpty()) {
                Log.i(TAG, "Transfer queue is empty");
                break;
            }
            Message message = transferQueue.remove();

            if (message != null) {
                sendFile(message, toEndpoint);
            }
        }
    }

    /**
     * @return the endpoint which has completed the most summarisations
     */
    private Endpoint getBestEndpoint() {
        List<Endpoint> connectedEndpoints = getConnectedEndpoints();
        int maxComplete = Integer.MIN_VALUE;
        int minJobs = Integer.MAX_VALUE;
        Endpoint best = null;

        for (Endpoint endpoint : connectedEndpoints) {
            if (!endpoint.isActive() && endpoint.completeCount > maxComplete) {
                maxComplete = endpoint.completeCount;
                best = endpoint;
            }
        }

        if (best != null) {
            return best;
        }

        // No free workers, so just choose the one with the most completed jobs, use job queue length as tiebreaker
        for (Endpoint endpoint : connectedEndpoints) {
            if (endpoint.completeCount > maxComplete ||
                    (endpoint.completeCount == maxComplete && endpoint.getJobCount() < minJobs)) {
                maxComplete = endpoint.completeCount;
                minJobs = endpoint.getJobCount();
                best = endpoint;
            }
        }

        return best;
    }

    /**
     * @return an inactive endpoint with the most completed summarisations, or the endpoint with the shortest job queue
     */
    private Endpoint getFastestEndpoint() {
        List<Endpoint> connectedEndpoints = getConnectedEndpoints();
        int maxComplete = Integer.MIN_VALUE;
        int minJobs = Integer.MAX_VALUE;
        Endpoint fastest = null;

        for (Endpoint endpoint : connectedEndpoints) {
            if (!endpoint.isActive() && endpoint.completeCount > maxComplete) {
                maxComplete = endpoint.completeCount;
                fastest = endpoint;
            }
        }

        if (fastest != null) {
            return fastest;
        }

        // No free workers, so just choose the one with the shortest job queue, use completion count as tiebreaker
        for (Endpoint endpoint : connectedEndpoints) {
            if (endpoint.getJobCount() < minJobs ||
                    (endpoint.getJobCount() == minJobs && endpoint.completeCount > maxComplete)) {
                maxComplete = endpoint.completeCount;
                minJobs = endpoint.getJobCount();
                fastest = endpoint;
            }
        }

        return fastest;
    }

    /**
     * @return the endpoint with the smallest job queue
     */
    private Endpoint getLeastBusyEndpoint() {
        return getConnectedEndpoints().stream().min(Comparator.comparing(Endpoint::getJobCount)).orElse(null);
    }

    /**
     * send messages to endpoints in the order that endpoints have completed jobs
     */
    private void transferToEarliestEndpoint() {
        int connectedCount = getConnectedCount();

        if (transferCount < connectedCount && transferQueue.size() >= connectedCount) {
            transferToAllEndpoints();
        } else if (transferCount >= connectedCount) {
            if (transferQueue.isEmpty()) {
                Log.i(TAG, "Transfer queue is empty");
                return;
            }
            Message message = transferQueue.remove();

            if (endpointQueue.isEmpty()) {
                Log.i(TAG, "Endpoint queue is empty");
                sendFile(message, getLeastBusyEndpoint());
            } else {
                Endpoint toEndpoint = endpointQueue.remove();
                sendFile(message, toEndpoint);
            }
        } else {
            Log.d(TAG, "Less queued transfers than connected endpoints");
        }
    }

    /**
     * send next video to an inactive endpoint with the most completed summarisations, or summarise video locally if
     * all endpoints are busy
     */
    private void transferToBestOrLocal() {
        if (transferQueue.isEmpty()) {
            Log.i(TAG, "Transfer queue is empty");
            return;
        }
        Message message = transferQueue.remove();
        List<Endpoint> connectedEndpoints = getConnectedEndpoints();
        int maxComplete = Integer.MIN_VALUE;
        Endpoint best = null;

        for (Endpoint endpoint : connectedEndpoints) {
            if (!endpoint.isActive() && endpoint.completeCount > maxComplete) {
                maxComplete = endpoint.completeCount;
                best = endpoint;
            }
        }

        if (best != null) {
            sendFile(message, best);
        } else {
            summarise(message);
        }
    }

    @Override
    public void nextTransfer() {
        if (transferQueue.isEmpty()) {
            Log.i(TAG, "Transfer queue is empty");
            return;
        }

        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "No context");
            return;
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String algorithmKey = getString(R.string.scheduling_algorithm_key);
        Algorithm selectedAlgorithm = Algorithm.valueOf(pref.getString(algorithmKey, DEFAULT_ALGORITHM.name()));
        Log.v(TAG, String.format("nextTransfer with selected algorithm: %s", selectedAlgorithm.name()));

        switch (selectedAlgorithm) {
            case best:
                sendFile(transferQueue.remove(), getBestEndpoint());
                break;
            case fastest:
                sendFile(transferQueue.remove(), getFastestEndpoint());
                break;
            case least_busy:
                sendFile(transferQueue.remove(), getLeastBusyEndpoint());
                break;
            case ordered:
                transferToEarliestEndpoint();
                break;
            case best_or_local:
                transferToBestOrLocal();
                break;
            case simple_return:
                // nextTransfer should only be called during the initial transfer with simple_return,
                //  all subsequent transfers will be handled by nextTransferOrQuickReturn
                // Probably shouldn't be used with live video downloading, summarisation speed may be faster than
                //  download speed
                int connectedCount = getConnectedCount();
                Log.v(TAG, String.format("connectedCount: %d, transferCount: %d, transferQueue size: %d",
                        connectedCount, transferCount, transferQueue.size()));

                if (transferCount < connectedCount && transferQueue.size() >= connectedCount) {
                    transferToAllEndpoints();
                } else {
                    Log.d(TAG, "In nextTransfer with simple_return; transferQueue is too small to start");
                }
                break;
            default:
        }
    }

    private void nextTransferOrQuickReturn(Context context, String toEndpointId) {
        boolean quickReturn = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(getString(R.string.scheduling_algorithm_key), "")
                .equals(getString(R.string.simple_return_algorithm_key));

        if (quickReturn) {
            Log.v(TAG, String.format("Quick return to %s", discoveredEndpoints.get(toEndpointId)));
            nextTransferTo(toEndpointId);
        } else {
            nextTransfer();
        }
    }

    private void nextTransferTo(String toEndpointId) {
        if (getConnectedEndpoints().size() == 0) {
            Log.e(TAG, "Not connected to any devices");
            return;
        }

        if (transferQueue.isEmpty()) {
            Log.i(TAG, "Transfer queue is empty");
            return;
        }

        Message message = transferQueue.remove();
        Endpoint toEndpoint = discoveredEndpoints.get(toEndpointId);
        sendFile(message, toEndpoint);
    }

    private void sendFile(Message message, Endpoint toEndpoint) {
        if (message == null || toEndpoint == null) {
            Log.e(TAG, "No message or endpoint selected");
            return;
        }

        transferCount++;
        File fileToSend = new File(message.video.getData());
        Uri uri = Uri.fromFile(fileToSend);
        Payload filePayload = null;
        Context context = getContext();

        if (context == null) {
            Log.e(TAG, "No context");
            return;
        }

        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                filePayload = Payload.fromFile(pfd);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, String.format("sendFile ParcelFileDescriptor error: \n%s", e.getMessage()));
        }

        if (filePayload == null) {
            Log.e(TAG, String.format("Could not create file payload for %s", message.video));
            return;
        }
        Log.w(TAG, String.format("Sending %s to %s", message.video.getName(), toEndpoint.name));

        // Construct a message mapping the ID of the file payload to the desired filename and command.
        // Also include summarisation preferences for summarisation commands
        String bytesMessage;
        if (Message.isSummarise(message.command)) {
            SummariserPrefs prefs = SummariserPrefs.extractPreferences(context);
            bytesMessage = String.format("%s:%s:%s:%s_%s_%s_%s",
                    message.command, filePayload.getId(), uri.getLastPathSegment(),
                    prefs.noise, prefs.duration, prefs.quality, prefs.speed
            );
        } else {
            bytesMessage = String.format("%s:%s:%s", message.command, filePayload.getId(), uri.getLastPathSegment());
        }

        // Send the filename message as a bytes payload.
        // Master will send to all workers, workers will just send to master
        Payload filenameBytesPayload = Payload.fromBytes(bytesMessage.getBytes(UTF_8));
        connectionsClient.sendPayload(toEndpoint.id, filenameBytesPayload);

        // Finally, send the file payload.
        connectionsClient.sendPayload(toEndpoint.id, filePayload);
        toEndpoint.addJob(uri.getLastPathSegment());
    }

    @Override
    public void sendCommandMessageToAll(Command command, String filename) {
        String commandMessage = String.format("%s:%s", command, filename);
        Payload filenameBytesPayload = Payload.fromBytes(commandMessage.getBytes(UTF_8));

        // Only sent from worker to master, might be better to make bidirectional
        List<String> connectedEndpointIds = discoveredEndpoints.values().stream()
                .filter(e -> e.connected)
                .map(e -> e.id)
                .collect(Collectors.toList());
        connectionsClient.sendPayload(connectedEndpointIds, filenameBytesPayload);
    }

    @Override
    public void sendCommandMessage(Command command, String filename, String toEndpointId) {
        String commandMessage = String.format("%s:%s", command, filename);
        Payload filenameBytesPayload = Payload.fromBytes(commandMessage.getBytes(UTF_8));
        connectionsClient.sendPayload(toEndpointId, filenameBytesPayload);
    }

    @Override
    public boolean isConnected() {
        return discoveredEndpoints.values().stream().anyMatch(e -> e.connected);
    }

    @Override
    public void connectEndpoint(Endpoint endpoint) {
        Log.d(TAG, String.format("Selected '%s'", endpoint));
        if (!endpoint.connected) {
            connectionsClient.requestConnection(localName, endpoint.id, connectionLifecycleCallback)
                    .addOnSuccessListener(
                            // We successfully requested a connection. Now both sides
                            // must accept before the connection is established.
                            (Void unused) -> Log.d(TAG, String.format("Requested connection with %s", endpoint)))
                    .addOnFailureListener(
                            // Nearby Connections failed to request the connection.
                            (Exception e) -> Log.e(TAG, String.format("Endpoint failure: \n%s", e.getMessage())));
        } else {
            Log.d(TAG, String.format("'%s' is already connected", endpoint));
        }
    }

    @Override
    public void disconnectEndpoint(Endpoint endpoint) {
        Log.d(TAG, String.format("Disconnected from '%s'", endpoint));

        connectionsClient.disconnectFromEndpoint(endpoint.id);
        endpoint.connected = false;
        deviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void removeEndpoint(Endpoint endpoint) {
        Log.d(TAG, String.format("Removed %s", endpoint));

        discoveredEndpoints.remove(endpoint.id);
        deviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String name);
    }

    @Override
    public void handleSegment(String videoName) {
        if (videoSegments.size() == 0) { // Not master device
            Log.v(TAG, "Worker device attempted to handle video segments");
            return;
        }

        String baseVideoName = FfmpegTools.getBaseName(videoName);
        LinkedHashMap<String, Video> vidMap = videoSegments.get(baseVideoName);
        if (vidMap == null) {
            Log.d(TAG, "Couldn't retrieve video map");
            return;
        }
        if (vidMap.size() == 0) { // Video segment map is already empty,
            Log.v(TAG, String.format("Removing video segment map for %s", baseVideoName));
            videoSegments.remove(baseVideoName);
            return;
        }

        Video video = vidMap.remove(videoName);
        if (video == null) {
            Log.d(TAG, "Couldn't retrieve video");
            return;
        }

        if (vidMap.size() == 0) {
            Log.d(TAG, String.format("Received all summarised video segments of %s", baseVideoName));
            String parentName = String.format("%s.%s", baseVideoName, FilenameUtils.getExtension(video.getName()));
            String outPath = FfmpegTools.mergeVideos(parentName);

            if (outPath == null) {
                Log.e(TAG, "Couldn't merge videos");
                return;
            }

            if (!outPath.equals(FfmpegTools.NO_VIDEO)) {
                Context context = getContext();
                if (context == null) {
                    Log.e(TAG, "No context");
                    return;
                }

                video.insertMediaValues(context, outPath);
                Video mergedVideo = VideoManager.getVideoFromPath(context, outPath);
                EventBus.getDefault().post(new AddEvent(mergedVideo, Type.SUMMARISED));
            }

            EventBus.getDefault().post(new RemoveByNameEvent(parentName, Type.RAW));
            EventBus.getDefault().post(new RemoveByNameEvent(parentName, Type.PROCESSING));
            Log.v(TAG, String.format("Removing video segment map for %s", baseVideoName));
            videoSegments.remove(baseVideoName);
        } else {
            Log.v(TAG, String.format("Received a segment of %s", baseVideoName));
        }
    }

    void summarise(Message message) {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "No context");
            return;
        }

        File videoFile = new File(message.video.getData());
        String filename = videoFile.getName();
        String outPath = message.command.equals(Command.SUMMARISE_SEGMENT) ?
                String.format("%s/%s", FileManager.getSegmentSumSubDirPath(filename), filename)
                : String.format("%s/%s", FileManager.getSummarisedDirPath(), filename);
        SummariserPrefs prefs = SummariserPrefs.extractPreferences(context);

        summarise(context, videoFile, prefs, outPath, false);
    }

    void summarise(Context context, File videoFile, SummariserPrefs prefs, String outPath, boolean sendVideo) {
        Log.d(TAG, String.format("Summarising %s", videoFile.getName()));

        Video video = VideoManager.getVideoFromPath(context, videoFile.getAbsolutePath());
        EventBus.getDefault().post(new AddEvent(video, Type.PROCESSING));
        EventBus.getDefault().post(new RemoveEvent(video, Type.RAW));

        Intent intent = new Intent(context, SummariserIntentService.class);
        intent.putExtra(SummariserIntentService.VIDEO_KEY, video);
        intent.putExtra(SummariserIntentService.OUTPUT_KEY, outPath);
        intent.putExtra(SummariserIntentService.TYPE_KEY, SummariserIntentService.NETWORK_TYPE);
        intent.putExtra(SummariserIntentService.SEND_VIDEO_KEY, sendVideo);
        intent.putExtra(SummariserPrefs.NOISE_KEY, prefs.noise);
        intent.putExtra(SummariserPrefs.DURATION_KEY, prefs.duration);
        intent.putExtra(SummariserPrefs.QUALITY_KEY, prefs.quality);
        intent.putExtra(SummariserPrefs.ENCODING_SPEED_KEY, prefs.speed.name());
        context.startService(intent);
    }

    class ReceiveFilePayloadCallback extends PayloadCallback {
        private final SimpleArrayMap<Long, Payload> incomingFilePayloads = new SimpleArrayMap<>();
        private final SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();
        private final SimpleArrayMap<Long, String> filePayloadFilenames = new SimpleArrayMap<>();
        private final SimpleArrayMap<Long, Command> filePayloadCommands = new SimpleArrayMap<>();
        private final SimpleArrayMap<Long, SummariserPrefs> filePayloadPrefs = new SimpleArrayMap<>();
        private final SimpleArrayMap<Long, Instant> startTimes = new SimpleArrayMap<>();

        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            Log.d(TAG, String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));

            if (payload.getType() == Payload.Type.BYTES) {
                String message = new String(Objects.requireNonNull(payload.asBytes()), UTF_8);
                String[] parts = message.split(":");
                long payloadId;
                String videoName;
                String videoPath;
                Video video;

                Endpoint fromEndpoint = discoveredEndpoints.get(endpointId);
                if (fromEndpoint == null) {
                    Log.e(TAG, String.format("Failed to retrieve endpoint %s", endpointId));
                    return;
                }

                Context context = getContext();
                if (context == null) {
                    Log.e(TAG, "No context");
                    return;
                }

                switch (Command.valueOf(parts[0])) {
                    case ERROR:
                        //
                        break;
                    case SUMMARISE:
                    case SUMMARISE_SEGMENT:
                        Log.v(TAG, String.format("Started downloading %s from %s", message, fromEndpoint));
                        payloadId = addPayloadFilename(parts);
                        startTimes.put(payloadId, Instant.now());

                        processFilePayload(payloadId, endpointId);
                        break;
                    case RETURN:
                        videoName = parts[2];
                        Log.v(TAG, String.format("Started downloading %s from %s", videoName, fromEndpoint));
                        payloadId = addPayloadFilename(parts);
                        startTimes.put(payloadId, Instant.now());

                        fromEndpoint.completeCount++;
                        fromEndpoint.removeJob(videoName);
                        endpointQueue.add(fromEndpoint);

                        processFilePayload(payloadId, endpointId);
                        nextTransferOrQuickReturn(context, endpointId);
                        break;
                    case COMPLETE:
                        videoName = parts[1];
                        Log.d(TAG, String.format("%s has finished downloading %s", fromEndpoint, videoName));

                        if (!isSegmentedVideo(videoName)) {
                            videoPath = String.format("%s/%s", FileManager.getRawFootageDirPath(), videoName);
                            video = VideoManager.getVideoFromPath(context, videoPath);

                            EventBus.getDefault().post(new AddEvent(video, Type.PROCESSING));
                            EventBus.getDefault().post(new RemoveEvent(video, Type.RAW));
                        }

                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                        boolean fastScheduling = pref.getBoolean(getString(R.string.fast_scheduling_key), false);
                        if (fastScheduling) {
                            nextTransferOrQuickReturn(context, endpointId);
                        }

                        break;
                    case NO_ACTIVITY:
                        videoName = parts[1];
                        Log.d(TAG, String.format("%s from %s contained no activity", videoName, fromEndpoint));

                        fromEndpoint.completeCount++;
                        fromEndpoint.removeJob(videoName);
                        endpointQueue.add(fromEndpoint);

                        if (!isSegmentedVideo(videoName)) {
                            videoPath = String.format("%s/%s", FileManager.getRawFootageDirPath(), videoName);
                            video = VideoManager.getVideoFromPath(context, videoPath);
                            EventBus.getDefault().post(new RemoveEvent(video, Type.PROCESSING));
                        } else {
                            handleSegment(videoName);
                        }

                        nextTransferOrQuickReturn(context, endpointId);
                        break;
                }
            } else if (payload.getType() == Payload.Type.FILE) {
                // Add this to our tracking map, so that we can retrieve the payload later.
                incomingFilePayloads.put(payload.getId(), payload);
            }
        }

        /**
         * Extracts the payloadId and filename from the message and stores it in the
         * filePayloadFilenames map. The format is command:payloadId:filename:preferences.
         */
        private long addPayloadFilename(String[] message) {
            Command command = Command.valueOf(message[0]);
            long payloadId = Long.parseLong(message[1]);
            String filename = message[2];
            filePayloadFilenames.put(payloadId, filename);
            filePayloadCommands.put(payloadId, command);

            if (Message.isSummarise(command)) {
                filePayloadPrefs.put(payloadId, new SummariserPrefs(message[3]));
            }

            return payloadId;
        }

        private boolean isSegmentedVideo(String videoName) {
            String baseVideoName = FfmpegTools.getBaseName(videoName);
            return videoSegments.containsKey(baseVideoName);
        }

        private void processFilePayload(long payloadId, String fromEndpointId) {
            // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
            // payload is completely received. The file payload is considered complete only when both have
            // been received.
            Payload filePayload = completedFilePayloads.get(payloadId);
            String filename = filePayloadFilenames.get(payloadId);
            Command command = filePayloadCommands.get(payloadId);

            if (filePayload != null && filename != null && command != null) {
                long duration = Duration.between(startTimes.remove(payloadId), Instant.now()).toMillis();
                String time = DurationFormatUtils.formatDuration(duration, "ss.SSS");
                Log.w(TAG, String.format("Completed downloading %s from %s in %ss",
                        filename, discoveredEndpoints.get(fromEndpointId), time));

                completedFilePayloads.remove(payloadId);
                filePayloadFilenames.remove(payloadId);
                filePayloadCommands.remove(payloadId);

                if (Message.isSummarise(command)) {
                    sendCommandMessage(Command.COMPLETE, filename, fromEndpointId);
                }
                // Get the received file (which will be in the Downloads folder)
                Payload.File payload = filePayload.asFile();
                File payloadFile = payload != null ? payload.asJavaFile() : null;

                if (payloadFile == null) {
                    Log.e(TAG, String.format("Could not create file payload for %s", filename));
                    return;
                }
                // Rename the file.
                File videoFile = new File(payloadFile.getParentFile(), filename);
                if (!payloadFile.renameTo(videoFile)) {
                    Log.e(TAG, String.format("Could not rename received file as %s", filename));
                    return;
                }

                if (Message.isSummarise(command)) {
                    SummariserPrefs prefs = filePayloadPrefs.remove(payloadId);
                    if (prefs == null) {
                        Log.e(TAG, "Failed to retrieve summarisation preferences");
                        return;
                    }

                    String outPath = (command.equals(Command.SUMMARISE_SEGMENT)) ?
                            String.format("%s/%s", FileManager.getSegmentSumSubDirPath(filename), videoFile.getName()) :
                            String.format("%s/%s", FileManager.getSummarisedDirPath(), videoFile.getName());
                    summarise(getContext(), videoFile, prefs, outPath, true);

                } else if (command.equals(Command.RETURN)) {
                    boolean isSeg = isSegmentedVideo(filename);
                    String videoDestPath = (isSeg) ?
                            String.format("%s/%s", FileManager.getSegmentSumSubDirPath(filename), filename) :
                            String.format("%s/%s", FileManager.getSummarisedDirPath(), filename);

                    File videoDest = new File(videoDestPath);
                    try {
                        FileManager.copy(videoFile, videoDest);
                    } catch (IOException e) {
                        Log.e(TAG, String.format("processFilePayload copy error: \n%s", e.getMessage()));
                    }

                    if (isSeg) {
                        handleSegment(filename);
                        return;
                    }

                    Context context = getContext();
                    if (context == null) {
                        Log.e(TAG, "No context");
                        return;
                    }

                    Video video = VideoManager.getVideoFromPath(context, videoDestPath);
                    EventBus.getDefault().post(new AddEvent(video, Type.SUMMARISED));
                    EventBus.getDefault().post(new RemoveByNameEvent(filename, Type.PROCESSING));
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
            // int progress = (int) (100.0 * (update.getBytesTransferred() / (double) update.getTotalBytes()));
            // Log.v(TAG, String.format("Transfer to %s: %d%%", endpointId, progress));

            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                Log.v(TAG, String.format("Transfer to %s complete", discoveredEndpoints.get(endpointId)));

                long payloadId = update.getPayloadId();
                Payload payload = incomingFilePayloads.remove(payloadId);
                completedFilePayloads.put(payloadId, payload);

                if (payload != null && payload.getType() == Payload.Type.FILE) {
                    processFilePayload(payloadId, endpointId);
                }
            }
        }
    }
}
