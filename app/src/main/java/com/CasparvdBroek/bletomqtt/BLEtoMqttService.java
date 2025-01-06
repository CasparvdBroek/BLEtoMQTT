package com.CasparvdBroek.bletomqtt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BLEtoMqttService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();


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

}
