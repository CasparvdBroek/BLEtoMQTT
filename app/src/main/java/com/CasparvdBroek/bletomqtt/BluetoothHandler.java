package com.CasparvdBroek.bletomqtt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;

import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.ConnectionPriority;

import com.welie.blessed.ConnectionState;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.PhyOptions;
import com.welie.blessed.PhyType;
import com.welie.blessed.ScanFailure;
import com.welie.blessed.WriteType;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BluetoothHandler {

    // === BLE Scan Power Optimization Constants ===
    private static final long SCAN_DURATION_MS = 5000; // 5 seconds
    private static final long SCAN_INTERVAL_MS = 30000; // 30 seconds

    public static BluetoothCentralManager central;
    private static BluetoothHandler instance = null;
    private static boolean isCleaningUp = false;
    private BLEtoMqttService context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    public List<String> MACAddressesList = new ArrayList<String>();
    public List<String> passKeyList = new ArrayList<String>();
    // Track connected peripherals to ensure proper cleanup
    private static List<BluetoothPeripheral> connectedPeripherals = new ArrayList<>();

    // === Scan/Connection State Flags ===
    private boolean isScanning = false;
    private boolean isConnecting = false;
    private Runnable scanTimeoutRunnable = null;
    private Runnable scanIntervalRunnable = null;

    public static BluetoothHandler getInstance(BLEtoMqttService context, JSONArray bleJsonArr)  {   //Makes it a singleton class
        if (instance == null) {
            instance = new BluetoothHandler(context, bleJsonArr);
        }
        return instance;
    }
    private BluetoothHandler(BLEtoMqttService context, JSONArray bleJsonArr) {
        JSONObject jsonDevice;
        String passKey;
        this.context = context;
        // Scan for peripherals with a certain service UUIDs

        try {
            for (int i = 0; i < bleJsonArr.length(); i++) {
                jsonDevice = bleJsonArr.getJSONObject(i);
                MACAddressesList.add(jsonDevice.getString("address"));
                passKey = "";
                if(jsonDevice.has("passkey")) {
                    passKey = jsonDevice.getString("passkey");
                }
                passKeyList.add(passKey);
            }
        }catch(JSONException e){

        }
        InitiateBLE();
    }
    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
        private static final String TAG = "BluetoothCentralManagerCallback";
        @Override
        public void onConnectedPeripheral(@NotNull BluetoothPeripheral peripheral) {
            if (central == null || isCleaningUp) {
                Log.w(TAG, "Central manager is null or cleaning up, skipping onConnectedPeripheral");
                return;
            }
            
            // Track connected peripheral for cleanup
            if (!connectedPeripherals.contains(peripheral)) {
                connectedPeripherals.add(peripheral);
                Log.i(TAG, "Added peripheral to tracking list: " + peripheral.getName() + " (" + peripheral.getAddress() + ")");
            }
            
            int index;
            String passKey;
            index =MACAddressesList.indexOf(peripheral.getAddress());
            passKey = passKeyList.get(index);
            if(passKey != "") {
                central.setPinCodeForPeripheral(peripheral.getAddress(), passKey);
            }
            // Stop scanning while connecting
            stopScanImmediately();
            isConnecting = false; // Connection is now established, allow scanning for more
            central.startPairingPopupHack();    //Skipped if samsung device
            // Immediately start a new scan for more devices
            startScan(MACAddressesList.toArray(new String[0]));
        }
        public void onConnectingPeripheral(@NotNull BluetoothPeripheral peripheral) {
            // Set connecting flag
            isConnecting = true;
        }
        @Override
        public void onConnectionFailed(@NotNull BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            if (central == null || isCleaningUp) {
                Log.w(TAG, "Central manager is null or cleaning up, skipping onConnectionFailed");
                return;
            }
            isConnecting = false;
            // After connection attempt, immediately start a new scan
            onConnectionAttemptFinished();
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull final BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            if (central == null || isCleaningUp) {
                Log.w(TAG, "Central manager is null or cleaning up, skipping onDisconnectedPeripheral");
                return;
            }

            // Remove from tracking list
            connectedPeripherals.remove(peripheral);
            Log.i(TAG, "Removed peripheral from tracking list: " + peripheral.getName() + " (" + peripheral.getAddress() + ")");

            // Reconnect to this device when it becomes available again
            Log.i(TAG,"onDisconnectedPeripheral - " +peripheral.getName() );
            isConnecting = false;
            // After connection attempt, immediately start a new scan
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onConnectionAttemptFinished();
                }
            });
            //        central.startPairingPopupHack();    //Skipped if samsung device
            //       startScan(MACAddressesList.toArray(new String[0]));    //Scan for more devices

        }

        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            if (central == null || isCleaningUp) {
                Log.w(TAG, "Central manager is null or cleaning up, skipping onDiscoveredPeripheral");
                return;
            }

            // Stop scanning and connect
            stopScanImmediately();
            isConnecting = true;
            central.connectPeripheral(peripheral, peripheralCallback);

        }

        @Override
        public void onBluetoothAdapterStateChanged(int state) {
            if (central == null || isCleaningUp) {
                Log.w(TAG, "Central manager is null or cleaning up, skipping onBluetoothAdapterStateChanged");
                return;
            }

            if (state == BluetoothAdapter.STATE_ON) {
                // Bluetooth is on now, start scanning again
                // Scan for peripherals with a certain service UUIDs
                Log.i(TAG,"BluetoothAdapter.STATE_ON is "+ state);
                central.close();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isCleaningUp) {
                            Log.w(TAG, "Cleaning up, skipping InitiateBLE");
                            return;
                        }
                        InitiateBLE();

                    }
                },1000);

//                central.startPairingPopupHack();
//                startScan(MACAddressesList.toArray(new String[0]));
            }
        }

        @Override
        public void onScanFailed(@NotNull ScanFailure scanFailure) {

        }
    };
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        private  final String TAG = "BluetoothPeripheralCallback";
        @Override
        public void onServicesDiscovered(@NotNull BluetoothPeripheral peripheral) {
            if (context == null || isCleaningUp) {
                Log.w(TAG, "Context is null or cleaning up, skipping onServicesDiscovered");
                return;
            }

            byte[] noValue = {' '};
            String sName;
            //    peripheral.createBond(); maybe for oreo and wait for bonding success
            // Request a higher MTU, iOS always asks for 185
            peripheral.requestMtu(185);
            // Request a new connection priority
            peripheral.requestConnectionPriority(ConnectionPriority.HIGH);
            peripheral.setPreferredPhy(PhyType.LE_2M, PhyType.LE_2M, PhyOptions.S2);
            peripheral.readPhy();
            sName = peripheral.getName();
            for (BluetoothGattService i : peripheral.getServices()){

                for (BluetoothGattCharacteristic j : i.getCharacteristics()) {
                    handler.post(new Runnable() {
                                     @Override
                                     public void run() {
                                         if (context != null && !isCleaningUp) {
                                         context.mqttPublish(peripheral.getAddress(), i.getUuid(), j.getUuid(), noValue);
                                         }
                                     }
                                 }
                    );

                    if ((j.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) !=0) {
                        try {
                            peripheral.setNotify(i.getUuid(), j.getUuid(), true);
                        }catch(Exception  e){

                        }
                    }
                    Log.i(TAG,"onServicesDiscovered - "+  j.getProperties() + "  UUID" + j.getUuid() );
                    if ((j.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) !=0) {
                        handler.post(new Runnable() {
                                         @Override
                                         public void run() {
                                             if (context != null && !isCleaningUp) {
                                             context.mqttSubscribe(peripheral.getAddress(), i.getUuid(), j.getUuid());
                                             }
                                         }
                                     }
                        );
                    }

                    if ((j.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) !=0)  {
                        //                      peripheral.readCharacteristic(j);//Causes errors
                    }
                }
            }
        }

        @Override
        public void onNotificationStateUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {

        }

        @Override
        public void onCharacteristicWrite(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
        }

        @Override
        public void onCharacteristicUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            //
            if (status != GattStatus.SUCCESS) return;
            handler.post(new Runnable() {
                             @Override
                             public void run() {
                                 if (context != null && !isCleaningUp) {
                                 context.mqttPublish(peripheral.getAddress(),characteristic.getService().getUuid(),characteristic.getUuid(),value);
                                 }
                             }
                         }
            );
        }

        @Override
        public void onMtuChanged(@NotNull BluetoothPeripheral peripheral, int mtu, @NotNull GattStatus status) {
        }

    };

    private void InitiateBLE(){
        central = new BluetoothCentralManager(context, bluetoothCentralManagerCallback, new Handler());
        central.startPairingPopupHack();    //Skipped if samsung device
        startScan( MACAddressesList.toArray(new String[0]));
    }

    private void startScan(String[] MACAddresses) {
        if (isScanning || isConnecting || isCleaningUp) {
            Log.i("BluetoothHandler", "Scan not started: already scanning, connecting, or cleaning up");
            return;
        }
        isScanning = true;
        Log.i("BluetoothHandler", "Starting BLE scan");
        central.scanForPeripheralsWithAddresses(MACAddresses);
        // Set up scan timeout
        if (scanTimeoutRunnable != null) handler.removeCallbacks(scanTimeoutRunnable);
        scanTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    Log.i("BluetoothHandler", "Scan duration expired, stopping scan");
                    stopScanAndScheduleNext();
                }
            }
        };
        handler.postDelayed(scanTimeoutRunnable, SCAN_DURATION_MS);
    }

    private void stopScanAndScheduleNext() {
        if (!isScanning) return;
        isScanning = false;
        try {
            central.stopScan();
        } catch (Exception e) {
            Log.w("BluetoothHandler", "Error stopping scan: " + e.getMessage());
        }
        // Schedule next scan after interval, but only if not connecting
        if (!isConnecting) {
            if (scanIntervalRunnable != null) handler.removeCallbacks(scanIntervalRunnable);
            scanIntervalRunnable = new Runnable() {
                @Override
                public void run() {
                    startScan(MACAddressesList.toArray(new String[0]));
                }
            };
            handler.postDelayed(scanIntervalRunnable, SCAN_INTERVAL_MS);
        }
    }

    private void stopScanImmediately() {
        if (!isScanning) return;
        isScanning = false;
        try {
            central.stopScan();
        } catch (Exception e) {
            Log.w("BluetoothHandler", "Error stopping scan: " + e.getMessage());
        }
        if (scanTimeoutRunnable != null) handler.removeCallbacks(scanTimeoutRunnable);
    }

    private void onConnectionAttemptFinished() {
        isConnecting = false;
        // Immediately start a new scan (even if already connected to other devices)
        startScan(MACAddressesList.toArray(new String[0]));
    }
    public static void Publish(String MACAddress, String ServiceUUID, String CharacteristicUUID, String message){
        if (central == null || isCleaningUp) {
            Log.w("BluetoothHandler", "Central manager is null or cleaning up, skipping Publish");
            return;
        }

        BluetoothPeripheral peripheral = central.getPeripheral(MACAddress);
        if (peripheral != null && peripheral.getState() == ConnectionState.CONNECTED) {
            peripheral.writeCharacteristic(UUID.fromString(ServiceUUID), UUID.fromString(CharacteristicUUID), message.getBytes(StandardCharsets.UTF_8), WriteType.WITH_RESPONSE);
        }
        }
    
    /**
     * Cleanup method to disconnect all BLE devices and close the central manager
     * Call this when the service is stopping
     */
    public static void cleanup() {
        if (isCleaningUp) {
            Log.i("BluetoothHandler", "Cleanup already in progress, skipping");
            return;
        }
        
        if (central != null) {
            isCleaningUp = true;
            Log.i("BluetoothHandler", "Cleaning up BLE connections");
            
            // Use a handler to perform cleanup with delays
            Handler cleanupHandler = new Handler(Looper.getMainLooper());
            
            cleanupHandler.post(new Runnable() {
                @Override
                public void run() {
                    performCleanup();
                }
            });
        } else {
            Log.i("BluetoothHandler", "Central manager already null, cleanup skipped");
        }
    }
    
    /**
     * Perform the actual cleanup with proper timing
     */
    private static void performCleanup() {
        try {
            // Stop scanning first
            central.stopScan();
            Log.i("BluetoothHandler", "Scanning stopped");
        } catch (Exception e) {
            Log.w("BluetoothHandler", "Error stopping scan: " + e.getMessage());
        }
        
        // Delay before disconnecting peripherals to allow current operations to complete
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                disconnectPeripherals();
            }
        }, 500);
    }
    
    /**
     * Disconnect all peripherals with proper timing
     */
    private static void disconnectPeripherals() {
        try {
            // Use our tracked peripherals list
            Log.i("BluetoothHandler", "Found " + connectedPeripherals.size() + " connected peripherals to cleanup");
            
            // Create a copy of the list to avoid concurrent modification
            List<BluetoothPeripheral> peripheralsToDisconnect = new ArrayList<>(connectedPeripherals);
            
            for (BluetoothPeripheral peripheral : peripheralsToDisconnect) {
                Log.i("BluetoothHandler", "Cleaning up peripheral: " + peripheral.getName() + " (" + peripheral.getAddress() + ")");
                try {
                    // Cancel connection
                    central.cancelConnection(peripheral);
                    Log.i("BluetoothHandler", "Successfully disconnected peripheral: " + peripheral.getName());
                } catch (Exception e) {
                    Log.w("BluetoothHandler", "Error disconnecting peripheral " + peripheral.getName() + ": " + e.getMessage());
                }
            }
            
            // Clear the tracking list
            connectedPeripherals.clear();
            
        } catch (Exception e) {
            Log.w("BluetoothHandler", "Error disconnecting peripherals: " + e.getMessage());
        }
        
        // Delay before closing central manager to allow peripheral cleanup to complete
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                closeCentralManager();
            }
        }, 2000); // Increased delay to 2 seconds for better cleanup
    }
    
    /**
     * Close the central manager
     */
    private static void closeCentralManager() {
        try {
            // Close the central manager
            central.close();
            Log.i("BluetoothHandler", "Central manager closed successfully");
        } catch (Exception e) {
            Log.w("BluetoothHandler", "Error closing central manager: " + e.getMessage());
        } finally {
            // Always reset the singleton instance and central reference
            central = null;
            instance = null;
            isCleaningUp = false;
            Log.i("BluetoothHandler", "BLE cleanup completed");
        }
    }
    
    /**
     * Get the singleton instance (for cleanup purposes)
     */
    public static BluetoothHandler getInstance() {
        return instance;
    }
}
