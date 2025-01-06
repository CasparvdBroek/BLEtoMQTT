package com.CasparvdBroek.bletomqtt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class BLEtoMqttService extends Service {
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

        MQTTHandler.getInstance(this,mqttJsonObj);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
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
