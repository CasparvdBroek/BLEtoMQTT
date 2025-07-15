# MQTTHandler Cleanup Verification Guide

## Overview
This guide helps verify that the MQTTHandler object is properly cleared when the stop service button is pressed.

## What to Look For

### 1. Start Service
When you press "Start Service", you should see these log messages:
```
MQTTHandler: === GET INSTANCE CALLED ===
MQTTHandler: instance null: true
MQTTHandler: v3Client null: true
MQTTHandler: Creating new MQTTHandler instance
MQTTHandler: === CONSTRUCTOR CALLED ===
MQTTHandler: Creating new MqttClient with broker: [your_broker_url], clientId: [uuid]
MQTTHandler: MqttClient created successfully
MQTTHandler: Connecting to MQTT broker...
MQTTHandler: === CONSTRUCTOR COMPLETED ===
MQTTHandler: === GET INSTANCE COMPLETED ===
MQTTHandler: MQTT connected successfully. Reconnect: false
```

### 2. Stop Service
When you press "Stop Service", you should see these log messages:
```
MQTTHandler: === MQTT CLEANUP STARTED ===
MQTTHandler: isCleaningUp: false
MQTTHandler: v3Client null: false
MQTTHandler: instance null: false
MQTTHandler: MQTT client is connected, performing cleanup
MQTTHandler: Publishing OFFLINE message to: [your_lwt_topic]
MQTTHandler: Disconnecting MQTT client
MQTTHandler: Closing MQTT client
MQTTHandler: Setting v3Client to null
MQTTHandler: Setting instance to null
MQTTHandler: Setting isCleaningUp to false
MQTTHandler: === MQTT CLEANUP COMPLETED ===
```

### 3. Restart Service
When you press "Start Service" again, you should see the same startup messages as step 1, confirming that a new instance is created.

## How to Test

1. **Install the debug APK** on your device
2. **Open Logcat** in Android Studio or use `adb logcat`
3. **Filter logs** by tag: `MQTTHandler`
4. **Start the service** and observe the startup logs
5. **Stop the service** and observe the cleanup logs
6. **Restart the service** and verify new instance creation

## Expected Behavior

✅ **Correct Behavior:**
- New MqttClient created on each service start
- MqttClient properly disconnected and closed on service stop
- Singleton instance reset to null after cleanup
- New instance created on subsequent service start

❌ **Incorrect Behavior:**
- Reusing existing MqttClient instance
- MqttClient not properly closed
- Singleton instance not reset
- Connection errors on restart

## Logcat Command
```bash
adb logcat | grep "MQTTHandler"
```

## Verification Points

1. **Instance Creation**: `instance null: true` should appear before each new service start
2. **Client Creation**: `Creating new MqttClient` should appear on each service start
3. **Cleanup**: `Setting v3Client to null` and `Setting instance to null` should appear on service stop
4. **Fresh Start**: After stop, the next start should show `instance null: true` again

## Troubleshooting

If you don't see the expected logs:
1. Check that the debug APK is installed
2. Verify logcat filtering is correct
3. Ensure the service is actually starting/stopping
4. Check for any MQTT connection errors in the logs 