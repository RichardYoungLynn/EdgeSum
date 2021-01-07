package com.example.edgesum.page.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edgesum.R;
import com.example.edgesum.util.nearby.NearbyFragment;

public class ConnectionFragment extends NearbyFragment {
    private static final String TAG = ConnectionFragment.class.getSimpleName();

    public ConnectionFragment() {
        // Required empty public constructor
    }

    static ConnectionFragment newInstance() {
        return new ConnectionFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_connection, container, false);

        RecyclerView recyclerView = rootView.findViewById(R.id.device_list);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(deviceAdapter);

        TextView locName = rootView.findViewById(R.id.local_name);
        locName.setText(localName);

        Switch discoverSwitch = rootView.findViewById(R.id.discover_switch);
        Switch advertiseSwitch = rootView.findViewById(R.id.advertise_switch);
        Switch autoSendSwitch = rootView.findViewById(R.id.auto_send_switch);

        discoverSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Log.v(TAG, "Discovery switch checked");
                startDiscovery();
            } else {
                Log.v(TAG, "Discovery switch unchecked");
                stopDiscovery();
            }
        });

        advertiseSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Log.v(TAG, "Advertisement switch checked");
                startAdvertising();
            } else {
                Log.v(TAG, "Advertisement switch unchecked");
                stopAdvertising();
            }
        });

        autoSendSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Log.v(TAG, "Auto-send switch checked");
                startDashDownload();
            } else {
                Log.v(TAG, "Auto-send switch unchecked");
                stopDashDownload();
            }
        });

        return rootView;
    }
}
