package com.CasparvdBroek.bletomqtt;

/**
 * Listener interface for MQTT connection events.
 * Used to notify when MQTT connects or fails to connect.
 */
public interface MqttConnectionListener {
    void onConnectionSuccess();
    void onConnectionFailure(Exception e);
} 