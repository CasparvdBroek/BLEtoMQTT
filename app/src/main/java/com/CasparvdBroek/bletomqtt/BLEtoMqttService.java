package com.CasparvdBroek.bletomqtt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.eclipse.paho.client.mqttv3.MqttException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BLEtoMqttService extends Service {

    public  final String CHANNEL_ID = "BLEtoMqttServiceChannel";

    boolean isCleaningUp = false; // Flag to prevent multiple cleanup calls
    JSONObject jsonObj ;
    JSONArray jsonArray ;
    JSONObject mqttJsonObj ;
    JSONArray bleJsonArr;

    @Override
    public void onCreate() {
        super.onCreate();
        // No longer start Bluetooth or MQTT here
        // All logic moved to onStartCommand
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = "";
        if (intent != null) {
            if (intent.getAction() != null && intent.getAction().equals("RESTARTFOREGROUND")) {
                input = "restart";
            } else {
                input = intent.getStringExtra("inputExtra");
            }
        }

        // Only start service if user requested (STARTFOREGROUND)
        if (intent != null && intent.getAction() != null &&
                (intent.getAction().equals("STARTFOREGROUND") || intent.getAction().equals("RESTARTFOREGROUND"))) {
            // Parse config
            try {
                jsonObj = new JSONObject(readFileFromSDCard());
                jsonArray = jsonObj.getJSONArray("bletomqtt");
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (jsonArray.getJSONObject(i).has("mqtt")) {
                        mqttJsonObj = jsonArray.getJSONObject(i).getJSONObject("mqtt");
                    }
                    if (jsonArray.getJSONObject(i).has("devices")) {
                        bleJsonArr = jsonArray.getJSONObject(1).getJSONArray("devices");
                    }
                }
            } catch (Exception e) {
                Log.e("BLEtoMqttService", "Error parsing config: " + e.getMessage());
                stopSelf();
                return START_NOT_STICKY;
            }

            final String inputFinal = input;
            // Start MQTT connection
            MQTTHandler.getInstance(this, mqttJsonObj, new MqttConnectionListener() {
                @Override
                public void onConnectionSuccess() {
                    createNotificationChannel();
                    Intent notificationIntent = new Intent(BLEtoMqttService.this, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(BLEtoMqttService.this,
                            0, notificationIntent, PendingIntent.FLAG_MUTABLE);
                    Notification notification = new NotificationCompat.Builder(BLEtoMqttService.this, CHANNEL_ID)
                            .setContentTitle("BLEtoMqtt Bridge")
                            .setContentText(inputFinal)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentIntent(pendingIntent)
                            .build();
                    startForeground(1, notification);
                    // Start BLE scanning only after MQTT connects
                    BluetoothHandler.getInstance(BLEtoMqttService.this, bleJsonArr);
                }
                @Override
                public void onConnectionFailure(Exception e) {
                    android.os.Handler handler = new android.os.Handler(getMainLooper());
                    handler.post(() -> android.widget.Toast.makeText(BLEtoMqttService.this, "MQTT connection failed: " + (e != null ? e.getMessage() : "Unknown error"), android.widget.Toast.LENGTH_LONG).show());
                    stopSelf();
                }
            });
            // Return sticky so Android keeps the service alive if killed after foreground is started
            return START_STICKY;
        } else {
            // Handle stop/cleanup as before
            stopForeground(true);
            if (isCleaningUp) {
                Log.i("BLEtoMqttService", "Cleanup already in progress, skipping");
                stopSelf();
                return START_NOT_STICKY;
            }
            Log.i("BLEtoMqttService", "Service stopping, cleaning up connections");
            isCleaningUp = true;
            try {
                MQTTHandler.cleanup();
                BluetoothHandler.cleanup();
                Log.i("BLEtoMqttService", "Service cleanup initiated");
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("BLEtoMqttService", "Stopping service after cleanup delay");
                        stopSelf();
                    }
                }, 2000);
            } catch (Exception e) {
                Log.e("BLEtoMqttService", "Error during service cleanup: " + e.getMessage());
                stopSelf();
            }
            return START_NOT_STICKY;
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Prevent multiple cleanup calls
        if (isCleaningUp) {
            Log.i("BLEtoMqttService", "Cleanup already in progress, skipping");
            return;
        }
        
        // Cleanup BLE connections and MQTT when service is destroyed
        Log.i("BLEtoMqttService", "Service being destroyed, cleaning up connections");
        
        isCleaningUp = true;
        
        try {
            // Set lstart to false to prevent new operations
            // lstart = false; // This line is removed as per the new_code
            
            // Cleanup MQTT connection
            MQTTHandler.cleanup();
            
            // Cleanup BLE connections
            BluetoothHandler.cleanup();
            
            Log.i("BLEtoMqttService", "Service cleanup completed");
        } catch (Exception e) {
            Log.e("BLEtoMqttService", "Error during service cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Final safety cleanup method - call this as a last resort
     * This method forces cleanup even if normal cleanup failed
     */
    public static void forceCleanup() {
        Log.w("BLEtoMqttService", "Force cleanup called - emergency cleanup in progress");
        
        try {
            // Force cleanup BLE
            if (BluetoothHandler.central != null) {
                try {
                    BluetoothHandler.central.close();
                } catch (Exception e) {
                    Log.e("BLEtoMqttService", "Error in force BLE cleanup: " + e.getMessage());
                }
                BluetoothHandler.central = null;
            }
            
            // Force cleanup MQTT
            if (MQTTHandler.v3Client != null) {
                try {
                    MQTTHandler.v3Client.close();
                } catch (Exception e) {
                    Log.e("BLEtoMqttService", "Error in force MQTT cleanup: " + e.getMessage());
                }
                MQTTHandler.v3Client = null;
            }
            
            Log.i("BLEtoMqttService", "Force cleanup completed");
        } catch (Exception e) {
            Log.e("BLEtoMqttService", "Error in force cleanup: " + e.getMessage());
        }
    }
    //    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {       //Safe if already exists as then does nothing
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "BLEtoMqtt Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public String[] Friendly2Raw(@NonNull String topic, String msg){
        JSONObject jsonDevice= null;
        String[] splitString = topic.split("/");
        String[] result = {"","","",""};
        boolean lfound = false;

        result[0] = splitString[0];
        result[1] = splitString[1];
        result[2] = splitString[2];
        result[3] = msg;

        try {
            for (int i = 0; i < bleJsonArr.length(); i++) {
                jsonDevice = bleJsonArr.getJSONObject(i);
                if(jsonDevice.has("topic")){
                    if(jsonDevice.getString("topic").equals(result[0])){
                        result[0] = jsonDevice.getString("address");
                        lfound = true;
                        break;
                    }
                }else {
                    if (jsonDevice.has("address")) {
                        if (jsonDevice.getString("address").equals(result[0])) {
                            lfound = true;
                            break;
                        }
                    }
                }
            }
        }catch(JSONException e){

        }
        JSONArray srvcJsonArr = null;
        JSONObject jsonService = null;
        try {
            if(lfound){
                lfound = false;
                if(jsonDevice.has("services")) {
                    srvcJsonArr = jsonDevice.getJSONArray("services");
                    for (int i = 0; i < srvcJsonArr.length(); i++) {
                        jsonService = srvcJsonArr.getJSONObject(i);
                        if (jsonService.has("topic")) {
                            if (jsonService.getString("topic").equals(result[1])) {
                                result[1] = jsonService.getString("UUID");
                                lfound = true;
                                break;
                            }
                        } else {
                            if (jsonService.has("UUID")) {
                                if (jsonService.getString("UUID").equals(result[1])) {
                                    lfound = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }catch(JSONException e){

        }
        JSONArray charactJsonArr = null;
        JSONObject jsonCharacteristic = null;
        try {
            if(lfound){
                lfound = false;
                if(jsonService.has("characteristics")) {
                    charactJsonArr = jsonService.getJSONArray("characteristics");
                    for (int i = 0; i < charactJsonArr.length(); i++) {
                        jsonCharacteristic = charactJsonArr.getJSONObject(i);
                        if (jsonCharacteristic.has("topic")) {
                            if (jsonCharacteristic.getString("topic").equals(result[2])) {
                                result[2] = jsonCharacteristic.getString("UUID");
                                lfound = true;
                                break;
                            }
                        } else {
                            if (jsonCharacteristic.has("UUID")) {
                                if (jsonCharacteristic.getString("UUID").equals(result[2])) {
                                    lfound = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }catch(JSONException e){

        }

        JSONObject jsonValue = null;
        JSONArray jsonValueArr = null;
        String name;
        int bitwiseMax= 0;
        int initialValue= 0;
        JSONObject jsonMsg=null;
        try {
            jsonMsg = new JSONObject(result[3]);
        }catch(Exception e){

        }
        try{
            if(lfound){
                if (jsonCharacteristic.has("BitWise")) {
                    jsonValue = jsonCharacteristic.getJSONObject("BitWise");
                    jsonValueArr = jsonValue.names();
                    bitwiseMax = jsonValueArr.length() ;
                    for (int i = 0; i < bitwiseMax; i++) {
                        name = jsonValueArr.getString(i);
                        if(jsonMsg.has(name)) {
                            if (jsonMsg.getString(name).equals("1")) {
                                initialValue = initialValue + two2power(bitwiseMax - i - 1);
                            }
                        }else{
                            if(jsonValue.getString(name).equals("1")) {
                                initialValue = initialValue + two2power(bitwiseMax - i - 1);
                            }
                        }
                    }
                    result[3] = Integer.toString(initialValue);
                }
            }
        } catch (Exception e){

        }

        return result;
    }

    public String[] Raw2Friendly(String MACAddress , UUID ServiceUUID, UUID CharacteristicUUID,byte[] value){
        JSONObject jsonDevice = null;
        String[] splitString = {"" ,"" };
        boolean lfound;

        String topicAddress = MACAddress;
        lfound = false;
        try {
            for (int i = 0; i < bleJsonArr.length(); i++) {
                jsonDevice = bleJsonArr.getJSONObject(i);
                if(jsonDevice.has("address")){
                    if(jsonDevice.getString("address").equals(topicAddress)){
                        if(jsonDevice.has("topic")) {
                            topicAddress = jsonDevice.getString("topic");
                        }
                        lfound = true;
                        break;

                    }
                }
            }
        }catch(JSONException e){

        }
        String  topicService = ServiceUUID.toString();
        JSONArray srvcJsonArr = null;
        JSONObject jsonService = null;
        try {
            if (lfound) {
                lfound = false;
                if (jsonDevice.has("services")) {
                    srvcJsonArr = jsonDevice.getJSONArray("services");
                    for (int i = 0; i < srvcJsonArr.length(); i++) {
                        jsonService = srvcJsonArr.getJSONObject(i);
                        if (jsonService.has("UUID")) {
                            if (jsonService.getString("UUID").equals(topicService)) {
                                if (jsonService.has("topic")) {
                                    topicService = jsonService.getString("topic");
                                }
                                lfound = true;
                                break;
                            }
                        }
                    }
                }
            }
        }catch(JSONException e){

        }

        String topicCharacteristic=CharacteristicUUID.toString();
        JSONArray charactJsonArr = null;
        JSONObject jsonCharacteristic = null;
        try {
            if(lfound) {
                lfound = false;
                if (jsonService.has("characteristics")) {
                    charactJsonArr = jsonService.getJSONArray("characteristics");
                    for (int i = 0; i < charactJsonArr.length(); i++) {
                        jsonCharacteristic = charactJsonArr.getJSONObject(i);
                        if (jsonCharacteristic.has("UUID")) {
                            if (jsonCharacteristic.getString("UUID").equals(topicCharacteristic)) {
                                if (jsonCharacteristic.has("topic")) {
                                    topicCharacteristic = jsonCharacteristic.getString("topic");
                                }
                                lfound = true;
                                break;
                            }
                        }
                    }
                }
            }
        }catch(JSONException e){

        }

        String  friendlyValue =new String(value, StandardCharsets.UTF_8);
        JSONObject jsonValue = null;
        JSONArray jsonValueArr = null;
        String name;
        int bitwiseMax= 0;
        int initialValue, testValue = 0;
        try {
            if(lfound) {
                if (jsonCharacteristic.has("BitWise")) {

                    if (friendlyValue.equals(" ") || friendlyValue.equals("")) {
                        friendlyValue = jsonCharacteristic.getString("BitWise");
                    }else{
                        jsonValue = jsonCharacteristic.getJSONObject("BitWise");
                        jsonValueArr = jsonValue.names();
                        bitwiseMax = jsonValueArr.length() ;
                        initialValue = Integer.parseInt(friendlyValue);
                        for (int i = 0; i < bitwiseMax; i++) {
                            testValue = two2power(bitwiseMax - i -1);
                            if(initialValue >= testValue) {
                                name = jsonValueArr.getString(i );
                                jsonValue.put(name,"1");
                                initialValue = initialValue - testValue;
                            }else{
                                name = jsonValueArr.getString(i );
                                jsonValue.put(name,"0");
                            }
                        }
///                        if(initialValue >= 1) {
                        //                           name = jsonValueArr.getString(bitwiseMax );
                        //                           jsonValue.put(name,"1");
                        //                      }else{
                        //                       name = jsonValueArr.getString(bitwiseMax );
                        //                       jsonValue.put(name,"0");
                        //                   }
                        friendlyValue = jsonValue.toString();
                    }
                }
            }
        }catch(JSONException e){

        }

        splitString[0] = String.join("/", topicAddress,topicService, topicCharacteristic);
        splitString[1] = friendlyValue;

        return splitString;
    }

    public void mqttPublish(String MACAddress , UUID ServiceUUID, UUID CharacteristicUUID ,byte[] value){
        if (!MQTTHandler.isAvailable()) {
            Log.w("BLEtoMqttService", "MQTT not available, skipping mqttPublish");
            return;
        }
        
        String[] splitString = Raw2Friendly( MACAddress, ServiceUUID, CharacteristicUUID,value);
        if (MQTTHandler.isAvailable()) {
            MQTTHandler.Publish(splitString[0], splitString[1], false);
        }
    };

    public void BLE_Publish(String topic , String message){
        if (!MQTTHandler.isAvailable()) {
            Log.w("BLEtoMqttService", "MQTT not available, skipping BLE_Publish");
            return;
        }

        String[] splitString = Friendly2Raw(topic,message);
        if (MQTTHandler.isAvailable()) {
            BluetoothHandler.Publish(splitString[0], splitString[1], splitString[2], splitString[3]);

        }
    };
    public void mqttSubscribe(String MACAddress , UUID ServiceUUID, UUID CharacteristicUUID ){
        if (!MQTTHandler.isAvailable()) {
            Log.w("BLEtoMqttService", "MQTT not available, skipping mqttSubscribe");
            return;
        }
        
        byte[] noValue={};
        String[] splitString = Raw2Friendly( MACAddress, ServiceUUID, CharacteristicUUID,noValue);
        if (MQTTHandler.isAvailable()) {
            try {
                MQTTHandler.Subscribe(splitString[0], 1);
            } catch (MqttException e) {
                Log.e("BLEtoMqttService", "Error subscribing to MQTT topic: " + e.getMessage());
            }
        }
    };
    private String readFileFromSDCard() {

        File file = new File("/sdcard/bletomqtt","setup.json");
        if (!file.exists()) {
            throw new RuntimeException("File not found");
        }

        BufferedReader reader = null;
        StringBuilder builder = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }
    public int two2power(int exponent){
        if (exponent == 0 ){
            return 1;
        }
        if (exponent > 15  ){
            return 0;
        }
        return 1 << exponent;

    }


}
