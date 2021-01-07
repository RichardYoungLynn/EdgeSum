package com.example.edgesum.util.nearby;

public interface DeviceCallback {
    void connectEndpoint(Endpoint endpoint);
    void disconnectEndpoint(Endpoint endpoint);
    void removeEndpoint(Endpoint endpoint);
}
