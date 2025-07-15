package com.CasparvdBroek.bletomqtt;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MainActivity extends AppCompatActivity implements PermissionHelper.PermissionCallback {
    
    private TextView tvServiceStatus;
    private ImageView ivBluetoothStatus, ivLocationStatus, ivNotificationStatus, ivStorageStatus;
    private MaterialButton btnRequestPermissions, btnStartService, btnStopService;
    private PermissionHelper permissionHelper;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize views
        initializeViews();
        
        // Initialize permission helper
        permissionHelper = new PermissionHelper(this, this);
        
        // Set up click listeners
        setupClickListeners();
        
        // Check initial permission status
        updatePermissionStatus();
        
        // Check if service is running
        isServiceRunning = isServiceRunning(BLEtoMqttService.class);
        updateServiceStatus();
    }
    
            @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
        // Check service status again when resuming
        isServiceRunning = isServiceRunning(BLEtoMqttService.class);
        updateServiceStatus();
    }
    
    private void initializeViews() {
        tvServiceStatus = findViewById(R.id.tvServiceStatus);
        ivBluetoothStatus = findViewById(R.id.ivBluetoothStatus);
        ivLocationStatus = findViewById(R.id.ivLocationStatus);
        ivNotificationStatus = findViewById(R.id.ivNotificationStatus);
        ivStorageStatus = findViewById(R.id.ivStorageStatus);
        btnRequestPermissions = findViewById(R.id.btnRequestPermissions);
        btnStartService = findViewById(R.id.btnStartService);
        btnStopService = findViewById(R.id.btnStopService);
    }
    
    private void setupClickListeners() {
        btnRequestPermissions.setOnClickListener(v -> {
            if (permissionHelper.hasAllPermissions()) {
                Toast.makeText(this, R.string.permissions_already_granted, Toast.LENGTH_SHORT).show();
            } else {
                permissionHelper.requestAllPermissions();
            }
        });
        
        btnStartService.setOnClickListener(v -> {
            if (permissionHelper.hasAllPermissions()) {
                startBLEtoMqttService();
            } else {
                Toast.makeText(this, R.string.grant_permissions_first, Toast.LENGTH_LONG).show();
                permissionHelper.requestAllPermissions();
            }
        });
        
        btnStopService.setOnClickListener(v -> stopBLEtoMqttService());
    }
    
    private void updatePermissionStatus() {
        // Check Bluetooth permissions
        boolean bluetoothGranted = checkBluetoothPermissions();
        ivBluetoothStatus.setImageResource(bluetoothGranted ? 
            android.R.drawable.ic_menu_myplaces : android.R.drawable.ic_menu_close_clear_cancel);
        ivBluetoothStatus.setColorFilter(ContextCompat.getColor(this, bluetoothGranted ? 
            android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        
        // Check Location permissions
        boolean locationGranted = checkLocationPermissions();
        ivLocationStatus.setImageResource(locationGranted ? 
            android.R.drawable.ic_menu_myplaces : android.R.drawable.ic_menu_close_clear_cancel);
        ivLocationStatus.setColorFilter(ContextCompat.getColor(this, locationGranted ? 
            android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        
        // Check Notification permissions
        boolean notificationGranted = checkNotificationPermissions();
        ivNotificationStatus.setImageResource(notificationGranted ? 
            android.R.drawable.ic_menu_myplaces : android.R.drawable.ic_menu_close_clear_cancel);
        ivNotificationStatus.setColorFilter(ContextCompat.getColor(this, notificationGranted ? 
            android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        
        // Check Storage permissions
        boolean storageGranted = checkStoragePermissions();
        ivStorageStatus.setImageResource(storageGranted ? 
            android.R.drawable.ic_menu_myplaces : android.R.drawable.ic_menu_close_clear_cancel);
        ivStorageStatus.setColorFilter(ContextCompat.getColor(this, storageGranted ? 
            android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        
        // Update request permissions button
        btnRequestPermissions.setEnabled(!permissionHelper.hasAllPermissions());
    }
    
    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Notifications are granted by default on older versions
    }
    
    private boolean checkStoragePermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void updateServiceStatus() {
        // This is a simplified check - in a real app you might want to check if the service is actually running
        if (isServiceRunning) {
            tvServiceStatus.setText(R.string.service_running);
            tvServiceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnStartService.setEnabled(false);
            btnStopService.setEnabled(true);
        } else {
            tvServiceStatus.setText(R.string.service_stopped);
            tvServiceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            btnStartService.setEnabled(true);
            btnStopService.setEnabled(false);
        }
    }
    
    /**
     * Get the current service status as a string
     * @return Service status string
     */
    private String getServiceStatusText() {
        if (isServiceRunning(BLEtoMqttService.class)) {
            return getString(R.string.service_running);
        } else {
            return getString(R.string.service_stopped);
        }
    }
    
    public void startBLEtoMqttService() {
        Intent startIntent = new Intent(this, BLEtoMqttService.class);
        startIntent.setAction("STARTFOREGROUND");
        startIntent.putExtra("inputExtra", "BLE scanning");
        ContextCompat.startForegroundService(this, startIntent);

        // Wait a moment for the service to start, then refresh status
        new android.os.Handler().postDelayed(() -> {
            refreshServiceStatus();
            if (isServiceRunning) {
                Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.service_start_failed, Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }

    public void stopBLEtoMqttService() {
        // First check if service is running
        if (!isServiceRunning) {
            Toast.makeText(this, R.string.service_stopped_message, Toast.LENGTH_SHORT).show();
            return;
        }

        // Log MQTTHandler state before stopping
        Log.d("MainActivity", "MQTTHandler state before stop: " + MQTTHandler.getDebugState());

        // Send stop intent to the service first
        Intent stopIntent = new Intent(this, BLEtoMqttService.class);
        stopIntent.setAction("STOPFOREGROUND");
        ContextCompat.startForegroundService(this, stopIntent);

        // Then call stopService to actually stop the service
        boolean stopped = stopService(stopIntent);
        
        // Wait a moment for the service to stop, then refresh status
        new android.os.Handler().postDelayed(() -> {
            refreshServiceStatus();
            
            // Log MQTTHandler state after stopping
            Log.d("MainActivity", "MQTTHandler state after stop: " + MQTTHandler.getDebugState());
            
            if (!isServiceRunning) {
                // Service is stopped (either was stopped successfully or was already stopped)
                Toast.makeText(this, R.string.service_stopped_message, Toast.LENGTH_SHORT).show();
            } else {
                // Service is still running after stop attempt
                Toast.makeText(this, R.string.service_stop_failed, Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }
    
    @Override
    public void onPermissionsGranted() {
        Toast.makeText(this, R.string.permissions_granted, Toast.LENGTH_SHORT).show();
        updatePermissionStatus();
    }
    
    @Override
    public void onPermissionsDenied(List<String> deniedPermissions) {
        StringBuilder message = new StringBuilder(getString(R.string.permissions_required_message));
        for (String permission : deniedPermissions) {
            message.append("• ").append(PermissionHelper.getPermissionDescription(this, permission)).append("\n");
        }
        
        Toast.makeText(this, R.string.permissions_denied, Toast.LENGTH_LONG).show();
        updatePermissionStatus();
        
        // Show dialog to guide user to settings
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.permissions_required)
                .setMessage(message.toString())
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    permissionHelper.showSettingsDialog();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    /**
     * Check if a service is currently running
     * @param serviceClass The service class to check
     * @return true if the service is running, false otherwise
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d("MainActivity", "Service " + serviceClass.getSimpleName() + " is running");
                return true;
            }
        }
        Log.d("MainActivity", "Service " + serviceClass.getSimpleName() + " is not running");
        return false;
    }
    
    /**
     * Refresh the service status and update the UI
     */
    private void refreshServiceStatus() {
        isServiceRunning = isServiceRunning(BLEtoMqttService.class);
        updateServiceStatus();
        Log.d("MainActivity", "Service status refreshed: " + (isServiceRunning ? "Running" : "Stopped"));
    }
}