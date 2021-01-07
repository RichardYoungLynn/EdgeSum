// https://github.com/android/connectivity-samples/blob/bbbd6f5db3e0ffdc0b92b172053b021456e4df8c/NearbyConnectionsWalkieTalkie/app/src/main/java/com/google/location/nearby/apps/walkietalkie/ConnectionsActivity.java#L555

package com.example.edgesum.util.nearby;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

class Endpoint {
    final String id;
    final String name;
    boolean connected;
    Integer completeCount = 0;
    private final List<String> jobList = new ArrayList<>();

    Endpoint(String id, String name, boolean connected) {
        this.id = id;
        this.name = name;
        this.connected = connected;
    }

    Endpoint(String id, String name) {
        this.id = id;
        this.name = name;
        this.connected = false;
    }

    public boolean isActive() {
        return !jobList.isEmpty();
    }

    void addJob(String videoName) {
        jobList.add(videoName);
    }

    void removeJob(String videoName) {
        jobList.remove(videoName);
    }

    int getJobCount() {
        return jobList.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Endpoint) {
            Endpoint other = (Endpoint) obj;
            return id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Endpoint{id=%s, name=%s}", id, name);
    }
}
