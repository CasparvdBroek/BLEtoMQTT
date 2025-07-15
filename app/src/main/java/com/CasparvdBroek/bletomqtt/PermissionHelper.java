package com.CasparvdBroek.bletomqtt;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {
    private static final String TAG = "PermissionHelper";
    
    // Permission constants
    public static final String[] BLUETOOTH_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
    };
    
    public static final String[] BLUETOOTH_SCAN_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    };
    
    public static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    public static final String[] STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    public static final String[] NOTIFICATION_PERMISSIONS = {
            Manifest.permission.POST_NOTIFICATIONS
    };
    
    public static final String[] ALL_REQUIRED_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS
    };
    
    private FragmentActivity activity;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private PermissionCallback callback;
    
    public interface PermissionCallback {
        void onPermissionsGranted();
        void onPermissionsDenied(List<String> deniedPermissions);
    }
    
    public PermissionHelper(FragmentActivity activity, PermissionCallback callback) {
        this.activity = activity;
        this.callback = callback;
        setupPermissionLauncher();
    }
    
    private void setupPermissionLauncher() {
        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    List<String> deniedPermissions = new ArrayList<>();
                    boolean allGranted = true;
                    
                    for (String permission : permissions.keySet()) {
                        if (!permissions.get(permission)) {
                            deniedPermissions.add(permission);
                            allGranted = false;
                        }
                    }
                    
                    if (allGranted) {
                        if (callback != null) {
                            callback.onPermissionsGranted();
                        }
                    } else {
                        if (callback != null) {
                            callback.onPermissionsDenied(deniedPermissions);
                        }
                    }
                }
        );
    }
    
    public boolean hasAllPermissions() {
        for (String permission : ALL_REQUIRED_PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12+, check if permission is required
                if (permission.equals(Manifest.permission.BLUETOOTH_SCAN) ||
                    permission.equals(Manifest.permission.BLUETOOTH_CONNECT) ||
                    permission.equals(Manifest.permission.POST_NOTIFICATIONS)) {
                    if (ContextCompat.checkSelfPermission(activity, permission) 
                            != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }
                }
            } else {
                // For older versions, check basic permissions
                if (permission.equals(Manifest.permission.BLUETOOTH) ||
                    permission.equals(Manifest.permission.BLUETOOTH_ADMIN) ||
                    permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (ContextCompat.checkSelfPermission(activity, permission) 
                            != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    public void requestAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : ALL_REQUIRED_PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12+, request all new permissions
                if (permission.equals(Manifest.permission.BLUETOOTH_SCAN) ||
                    permission.equals(Manifest.permission.BLUETOOTH_CONNECT) ||
                    permission.equals(Manifest.permission.POST_NOTIFICATIONS)) {
                    if (ContextCompat.checkSelfPermission(activity, permission) 
                            != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(permission);
                    }
                }
            } else {
                // For older versions, request basic permissions
                if (permission.equals(Manifest.permission.BLUETOOTH) ||
                    permission.equals(Manifest.permission.BLUETOOTH_ADMIN) ||
                    permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (ContextCompat.checkSelfPermission(activity, permission) 
                            != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(permission);
                    }
                }
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            if (callback != null) {
                callback.onPermissionsGranted();
            }
        }
    }
    
    public void showPermissionExplanationDialog(String permission, String message) {
        new AlertDialog.Builder(activity)
                .setTitle("Permission Required")
                .setMessage(message)
                .setPositiveButton("Grant", (dialog, which) -> {
                    requestAllPermissions();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (callback != null) {
                        callback.onPermissionsDenied(List.of(permission));
                    }
                })
                .setCancelable(false)
                .show();
    }
    
    public void showSettingsDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Permissions Required")
                .setMessage("Some permissions are required for this app to function properly. " +
                        "Please grant them in the app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    public static String getPermissionDescription(String permission) {
        switch (permission) {
            case Manifest.permission.BLUETOOTH:
                return "Basic Bluetooth functionality";
            case Manifest.permission.BLUETOOTH_ADMIN:
                return "Bluetooth administration";
            case Manifest.permission.BLUETOOTH_SCAN:
                return "Scan for Bluetooth devices";
            case Manifest.permission.BLUETOOTH_CONNECT:
                return "Connect to Bluetooth devices";
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "Precise location for BLE scanning";
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Approximate location for BLE scanning";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "Read files from storage";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "Write files to storage";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "Show notifications for the service";
            default:
                return "Unknown permission";
        }
    }
    
    public static String getPermissionDescription(Context context, String permission) {
        switch (permission) {
            case Manifest.permission.BLUETOOTH:
                return context.getString(R.string.permission_bluetooth_desc);
            case Manifest.permission.BLUETOOTH_ADMIN:
                return context.getString(R.string.permission_bluetooth_admin_desc);
            case Manifest.permission.BLUETOOTH_SCAN:
                return context.getString(R.string.permission_bluetooth_scan_desc);
            case Manifest.permission.BLUETOOTH_CONNECT:
                return context.getString(R.string.permission_bluetooth_connect_desc);
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return context.getString(R.string.permission_location_fine_desc);
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return context.getString(R.string.permission_location_coarse_desc);
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return context.getString(R.string.permission_storage_read_desc);
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return context.getString(R.string.permission_storage_write_desc);
            case Manifest.permission.POST_NOTIFICATIONS:
                return context.getString(R.string.permission_notification_desc);
            default:
                return "Unknown permission";
        }
    }
} 