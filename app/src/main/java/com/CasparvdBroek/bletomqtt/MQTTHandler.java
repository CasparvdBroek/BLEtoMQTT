package com.CasparvdBroek.bletomqtt;

import android.os.Handler;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class MQTTHandler{
    private static MQTTHandler instance = null;
    private static boolean isCleaningUp = false;
    private BLEtoMqttService context;
    private final Handler handler = new Handler();
    private MqttConnectOptions conOpts = new MqttConnectOptions();
    static MqttClient v3Client;
    private static MqttConnectionListener connectionListener;
    
    private String broker = "tcp://192.168.192.40:1883";
    private String user = "ghost";
    private String pswd = "1234567890";
    private static String willtopic = "LWT_topic";
    private String clientId = java.util.UUID.randomUUID().toString();
    
    public static MQTTHandler getInstance(BLEtoMqttService context, JSONObject jsonObj, MqttConnectionListener listener) {
        android.util.Log.d("MQTTHandler", "=== GET INSTANCE CALLED ===");
        android.util.Log.d("MQTTHandler", "instance null: " + (instance == null));
        android.util.Log.d("MQTTHandler", "v3Client null: " + (v3Client == null));
        connectionListener = listener;
        if (instance == null) {
            android.util.Log.d("MQTTHandler", "Creating new MQTTHandler instance");
            instance = new MQTTHandler(context, jsonObj, listener);
        } else {
            android.util.Log.d("MQTTHandler", "Returning existing MQTTHandler instance");
        }
        android.util.Log.d("MQTTHandler", "=== GET INSTANCE COMPLETED ===");
        return instance;
    }

    private MQTTHandler(BLEtoMqttService context, JSONObject jsonObj, MqttConnectionListener listener) {
        android.util.Log.d("MQTTHandler", "=== CONSTRUCTOR CALLED ===");
        this.context = context;
        try {
            try {
                if(jsonObj.has("clientID")) {
                    clientId = jsonObj.getString("clientID");
                }
                broker = jsonObj.getString("IP");
                user = jsonObj.getString("user");
                pswd = jsonObj.getString("pswd");
                if(jsonObj.has("LWT_topic")) {
                    willtopic = jsonObj.getString("LWT_topic");
                }
            }catch(JSONException e){
                Toast.makeText(context,"JSON, "+ e.getMessage(), Toast.LENGTH_LONG).show();
            }
            android.util.Log.d("MQTTHandler", "Creating new MqttClient with broker: " + broker + ", clientId: " + clientId);
            v3Client = new MqttClient(broker, clientId, null);
            android.util.Log.d("MQTTHandler", "MqttClient created successfully");
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(user);
            options.setPassword(pswd.toCharArray());
            options.setConnectionTimeout(30); // Increased from 0 to 30 seconds
            options.setCleanSession(false);
            options.setWill(willtopic,"OFFLINE".getBytes(),1,true);
            options.setAutomaticReconnect(true);
            options.setKeepAliveInterval(60); // Reduced from 300 to 60 seconds
            options.setMaxInflight(1000); // Allow more in-flight messages
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            // Set client timeout
            v3Client.setTimeToWait(10000); // 10 seconds timeout for operations
            v3Client.setCallback(new MqttCallbackExtended(){
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (context == null || isCleaningUp) {
                        android.util.Log.w("MQTTHandler", "Context is null or cleaning up, skipping connectComplete");
                        return;
                    }
                    android.util.Log.i("MQTTHandler", "MQTT connected successfully. Reconnect: " + reconnect);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (context != null && !isCleaningUp) {
                                Publish(willtopic, "ONLINE", true);
                                if (listener != null) listener.onConnectionSuccess();
                            }
                        }
                    }, 1000);
                }
                @Override
                public void connectionLost(Throwable cause) {
                    android.util.Log.w("MQTTHandler", "MQTT connection lost: " + (cause != null ? cause.getMessage() : "Unknown reason"));
                }
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if (context == null || isCleaningUp) {
                        android.util.Log.w("MQTTHandler", "Context is null or cleaning up, skipping messageArrived");
                        return;
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (context != null && !isCleaningUp) {
                                context.BLE_Publish(topic,message.toString());
                            }
                        }
                    });
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    android.util.Log.d("MQTTHandler", "Message delivery completed");
                }
            });
            android.util.Log.d("MQTTHandler", "Connecting to MQTT broker...");
            v3Client.connect(options);
            android.util.Log.d("MQTTHandler", "=== CONSTRUCTOR COMPLETED ===");
        } catch (MqttException e) {
            android.util.Log.e("MQTTHandler", "MQTT connection failed: " + e.getMessage());
            if (listener != null) listener.onConnectionFailure(e);
            Toast.makeText(context,"MQTT, "+ e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void Publish(String topic, String msg, boolean retain){
        if (v3Client == null || isCleaningUp) {
            android.util.Log.w("MQTTHandler", "MQTT client is null or cleaning up, skipping Publish");
            return;
        }
        
        MqttMessage message = new MqttMessage( msg.getBytes());
        message.setQos(1);
        message.setRetained(retain);

        try {
            if (v3Client.isConnected()) {
                // Set a shorter timeout for publish operations
                v3Client.setTimeToWait(5000); // 5 seconds for publish
                v3Client.publish(topic, message);
                android.util.Log.d("MQTTHandler", "Message published successfully to topic: " + topic);
            } else {
                android.util.Log.w("MQTTHandler", "MQTT client not connected, cannot publish to topic: " + topic);
            }
        } catch (MqttException e){
            android.util.Log.e("MQTTHandler", "Error publishing message to topic " + topic + ": " + e.getMessage() + " (Reason code: " + e.getReasonCode() + ")");
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
                android.util.Log.e("MQTTHandler", "Publish timeout detected - server may be overloaded or network issue");
            }
        }
    };
    public static void Subscribe(String topicName, int qos) throws MqttException {
        if (v3Client == null || isCleaningUp) {
            android.util.Log.w("MQTTHandler", "MQTT client is null or cleaning up, skipping Subscribe");
            return;
        }

        try {
            if (v3Client.isConnected()) {
                v3Client.setTimeToWait(5000); // 5 seconds for subscribe
                v3Client.subscribe(topicName, qos);
                android.util.Log.d("MQTTHandler", "Subscribed successfully to topic: " + topicName);
            } else {
                android.util.Log.w("MQTTHandler", "MQTT client not connected, cannot subscribe to topic: " + topicName);
            }
        } catch (MqttException e) {
            android.util.Log.e("MQTTHandler", "Error subscribing to topic " + topicName + ": " + e.getMessage() + " (Reason code: " + e.getReasonCode() + ")");
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
                android.util.Log.e("MQTTHandler", "Subscribe timeout detected - server may be overloaded or network issue");
            }
            throw e; // Re-throw to maintain original method signature
        }

    }
    
    /**
     * Check if MQTT client is available and connected
     */
    public static boolean isAvailable() {
        return v3Client != null && v3Client.isConnected() && !isCleaningUp;
    }
    
    /**
     * Check connection status and attempt reconnect if needed
     */
    public static boolean checkAndReconnect() {
        if (v3Client == null || isCleaningUp) {
            return false;
        }
        
        if (!v3Client.isConnected()) {
            android.util.Log.w("MQTTHandler", "MQTT client disconnected, attempting to reconnect...");
            try {
                // The automatic reconnect should handle this, but we can log it
                android.util.Log.i("MQTTHandler", "Automatic reconnect should be in progress");
                return false; // Not immediately available
            } catch (Exception e) {
                android.util.Log.e("MQTTHandler", "Error during reconnect attempt: " + e.getMessage());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Cleanup method to disconnect MQTT client
     * Call this when the service is stopping
     */
    public static void cleanup() {
        android.util.Log.d("MQTTHandler", "=== MQTT CLEANUP STARTED ===");
        android.util.Log.d("MQTTHandler", "isCleaningUp: " + isCleaningUp);
        android.util.Log.d("MQTTHandler", "v3Client null: " + (v3Client == null));
        android.util.Log.d("MQTTHandler", "instance null: " + (instance == null));
        
        if (isCleaningUp) {
            android.util.Log.i("MQTTHandler", "Cleanup already in progress, skipping");
            return;
        }
        
        if (v3Client != null && v3Client.isConnected()) {
            android.util.Log.d("MQTTHandler", "MQTT client is connected, performing cleanup");
            isCleaningUp = true;
            try {
                // Publish offline message before disconnecting
                if (willtopic != null) {
                    android.util.Log.d("MQTTHandler", "Publishing OFFLINE message to: " + willtopic);
                    Publish(willtopic, "OFFLINE", true);
                }
                
                // Disconnect the client
                android.util.Log.d("MQTTHandler", "Disconnecting MQTT client");
                v3Client.disconnect();
                android.util.Log.d("MQTTHandler", "Closing MQTT client");
                v3Client.close();
                
                android.util.Log.i("MQTTHandler", "MQTT cleanup completed");
            } catch (MqttException e) {
                android.util.Log.e("MQTTHandler", "Error during MQTT cleanup: " + e.getMessage());
            } finally {
                // Always reset the singleton instance
                android.util.Log.d("MQTTHandler", "Setting v3Client to null");
                v3Client = null;
                android.util.Log.d("MQTTHandler", "Setting instance to null");
                instance = null;
                android.util.Log.d("MQTTHandler", "Setting isCleaningUp to false");
                isCleaningUp = false;
                android.util.Log.d("MQTTHandler", "=== MQTT CLEANUP COMPLETED ===");
            }
        } else {
            android.util.Log.i("MQTTHandler", "MQTT client already null or disconnected, cleanup skipped");
            android.util.Log.d("MQTTHandler", "Setting v3Client to null");
            v3Client = null;
            android.util.Log.d("MQTTHandler", "Setting instance to null");
            instance = null;
            android.util.Log.d("MQTTHandler", "Setting isCleaningUp to false");
            isCleaningUp = false;
            android.util.Log.d("MQTTHandler", "=== MQTT CLEANUP COMPLETED ===");
        }
    }
    
    /**
     * Get the singleton instance (for cleanup purposes)
     */
    public static MQTTHandler getInstance() {
        return instance;
    }
    
    /**
     * Debug method to check the current state of MQTTHandler
     * This can be used to verify cleanup behavior
     */
    public static String getDebugState() {
        StringBuilder state = new StringBuilder();
        state.append("MQTTHandler Debug State:\n");
        state.append("  instance null: ").append(instance == null).append("\n");
        state.append("  v3Client null: ").append(v3Client == null).append("\n");
        state.append("  isCleaningUp: ").append(isCleaningUp).append("\n");
        
        if (v3Client != null) {
            try {
                state.append("  v3Client connected: ").append(v3Client.isConnected()).append("\n");
            } catch (Exception e) {
                state.append("  v3Client connected: ERROR - ").append(e.getMessage()).append("\n");
            }
        }
        
        return state.toString();
    }

}
