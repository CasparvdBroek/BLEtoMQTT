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

    public static BluetoothCentralManager central;
    private static BluetoothHandler instance = null;
    private BLEtoMqttService context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    public List<String> MACAddressesList = new ArrayList<String>();
    public List<String> passKeyList = new ArrayList<String>();

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
            int index;
            String passKey;
            index =MACAddressesList.indexOf(peripheral.getAddress());
            passKey = passKeyList.get(index);
            if(passKey != "") {
                central.setPinCodeForPeripheral(peripheral.getAddress(), passKey);
            }
            //Have connected so re enable scanning for more
            central.startPairingPopupHack();    //Skipped if samsung device
            startScan(MACAddressesList.toArray(new String[0]));    //Scan for more devices

        }
        public void onConnectingPeripheral(@NotNull BluetoothPeripheral peripheral) {
        }
        @Override
        public void onConnectionFailed(@NotNull BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            central.startPairingPopupHack();    //Skipped if samsung device
            startScan(MACAddressesList.toArray(new String[0]));    //Scan for more devices
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull final BluetoothPeripheral peripheral, final @NotNull HciStatus status) {


            // Reconnect to this device when it becomes available again
            Log.i(TAG,"onDisconnectedPeripheral - " +peripheral.getName() );
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //             central.stopScan();
                    //              peripheral.cancelConnection();
                    central.autoConnectPeripheral(peripheral, peripheralCallback);
                }
            }, 5000);
            //        central.startPairingPopupHack();    //Skipped if samsung device
            //       startScan(MACAddressesList.toArray(new String[0]));    //Scan for more devices

        }

        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {

            central.stopScan(); //Connect whilst doing nothing else can get errors. Restart in connection callbacks
            central.connectPeripheral(peripheral, peripheralCallback);

        }

        @Override
        public void onBluetoothAdapterStateChanged(int state) {

            if (state == BluetoothAdapter.STATE_ON) {
                // Bluetooth is on now, start scanning again
                // Scan for peripherals with a certain service UUIDs
                Log.i(TAG,"BluetoothAdapter.STATE_ON is "+ state);
                central.close();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {


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
                                         context.mqttPublish(peripheral.getAddress(), i.getUuid(), j.getUuid(), noValue);
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
                                             context.mqttSubscribe(peripheral.getAddress(), i.getUuid(), j.getUuid());
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
                                 context.mqttPublish(peripheral.getAddress(),characteristic.getService().getUuid(),characteristic.getUuid(),value);
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
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                central.scanForPeripheralsWithAddresses(MACAddresses);
            }
        }, 1000);
    }
    public static void Publish(String MACAddress, String ServiceUUID, String CharacteristicUUID, String message){

        BluetoothPeripheral peripheral = central.getPeripheral(MACAddress);
        if (peripheral.getState() == ConnectionState.CONNECTED) {
            peripheral.writeCharacteristic(UUID.fromString(ServiceUUID), UUID.fromString(CharacteristicUUID), message.getBytes(StandardCharsets.UTF_8), WriteType.WITH_RESPONSE);

        }
    }
}
