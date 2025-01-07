package com.CasparvdBroek.bletomqtt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

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

    boolean lstart = false;      //Used to stop function if MainActivity app still running.
    JSONObject jsonObj ;
    JSONArray jsonArray ;
    JSONObject mqttJsonObj ;
    JSONArray bleJsonArr;

    @Override
    public void onCreate() {
        super.onCreate();

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
        }

//Starts the Bluetooth scanning, callbacks run in UI thread (ie main)
        BluetoothHandler.getInstance(this,bleJsonArr);

        MQTTHandler.getInstance(this,mqttJsonObj);     // connects with broker
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String input;

        lstart = false;
        if (  intent.getAction().equals("RESTARTFOREGROUND")) {
            input = "restart";
            lstart = true;
        } else{
            input = intent.getStringExtra("inputExtra");
            if (intent.getAction().equals("STARTFOREGROUND")) {
                lstart = true;
            }
        }

        if (lstart) {
            createNotificationChannel();
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, PendingIntent.FLAG_MUTABLE);     //FLAG_ added 20241212
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("BLEtoMqtt Bridge")
                    .setContentText(input)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);
            return START_STICKY;
        }
        else {
            stopForeground(true);
            stopSelf();
            //        stopSelfResult(startId);
            return START_NOT_STICKY;
        }

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
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
        String[] splitString = Raw2Friendly( MACAddress, ServiceUUID, CharacteristicUUID,value);
        if (lstart) {
            MQTTHandler.Publish(splitString[0], splitString[1]);
        }
    };

    public void BLE_Publish(String topic , String message){

        String[] splitString = Friendly2Raw(topic,message);
        if (lstart) {
            BluetoothHandler.Publish(splitString[0], splitString[1], splitString[2], splitString[3]);

        }
    };
    public void mqttSubscribe(String MACAddress , UUID ServiceUUID, UUID CharacteristicUUID ){
        byte[] noValue={};
        String[] splitString = Raw2Friendly( MACAddress, ServiceUUID, CharacteristicUUID,noValue);
        if (lstart) {
            try {
                MQTTHandler.Subscribe(splitString[0], 0);
            } catch (MqttException e) {

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
