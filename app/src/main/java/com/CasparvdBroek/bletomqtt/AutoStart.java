package com.CasparvdBroek.bletomqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

public class AutoStart  extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            Intent serviceIntent = new Intent(context, BLEtoMqttService.class);
            serviceIntent.setAction("RESTARTFOREGROUND");
            serviceIntent.putExtra("inputExtra", "BLE scanning after restart");
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}
