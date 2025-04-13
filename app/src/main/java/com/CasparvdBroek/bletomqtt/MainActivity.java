package com.CasparvdBroek.bletomqtt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    Button btnStartService, btnStopService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStartService = findViewById(R.id.butStart);
        btnStopService = findViewById(R.id.butStop);

        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBLEtoMqttService();
            }
        });
        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopBLEtoMqttService();
            }
        });

    }
    public void startBLEtoMqttService() {
        Intent startIntent = new Intent(this, BLEtoMqttService.class);
        startIntent.setAction("STARTFOREGROUND");
        startIntent.putExtra("inputExtra", "BLE scanning");
        ContextCompat.startForegroundService(this, startIntent);

    }

    public void stopBLEtoMqttService() {

        Intent stopIntent = new Intent(this, BLEtoMqttService.class);
        stopIntent.setAction("STOPFOREGROUND");
        ContextCompat.startForegroundService(this, stopIntent);

    }

}