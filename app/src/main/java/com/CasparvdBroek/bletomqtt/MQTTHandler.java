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
    private BLEtoMqttService context;
    private final Handler handler = new Handler();
    private MqttConnectOptions conOpts = new MqttConnectOptions();
    private static MqttClient v3Client;

    private String broker = "tcp://192.168.192.40:1883";
    private String user = "ghost";

    private String pswd = "1234567890";
    private String willtopic = "LWT_topic";

    private String clientId = UUID.randomUUID().toString();
    public static MQTTHandler getInstance(BLEtoMqttService context, JSONObject jsonObj) {
        if (instance == null) {
            instance = new MQTTHandler(context, jsonObj);
        }
        return instance;
    }
    private MQTTHandler(BLEtoMqttService context,JSONObject jsonObj) {
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
            v3Client = new MqttClient(broker, clientId, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(user);
            options.setPassword(pswd.toCharArray());
            options.setConnectionTimeout(0);
            options.setCleanSession(false);
            options.setWill(willtopic,"OFFLINE".getBytes(),1,true);
            options.setAutomaticReconnect(true);
            options.setKeepAliveInterval(300); //Default value 60 seconds
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            v3Client.setCallback(new MqttCallbackExtended(){
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {

                    handler.postDelayed(new Runnable() {
                                     @Override
                                     public void run() {
                                         Publish(willtopic, "ONLINE", true);
                                     }
                                 }
                    ,1000);
                }
                @Override
                public void connectionLost(Throwable cause) {}

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    handler.post(new Runnable() {
                                     @Override
                                     public void run() {
                                         context.BLE_Publish(topic,message.toString());
                                     }
                                 }
                    );
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }

            });
            v3Client.connect(options);

        } catch (MqttException e) {
            Toast.makeText(context,"MQTT, "+ e.getMessage(), Toast.LENGTH_LONG).show();

        }
    }

    public static void Publish(String topic, String msg, boolean retain){
        MqttMessage message = new MqttMessage( msg.getBytes());
        message.setQos(1);
        message.setRetained(retain);

        try {
            if (v3Client.isConnected()) {
                v3Client.setTimeToWait(5);
                v3Client.publish(topic, message);
            }
        } catch (MqttException e){

        }
    };
    public static void Subscribe(String topicName, int qos) throws MqttException {

        try {
            if (v3Client.isConnected()) {
                v3Client.setTimeToWait(5);
                v3Client.subscribe(topicName, qos);
            }
        } catch (MqttException e) {

        }

    }


}
