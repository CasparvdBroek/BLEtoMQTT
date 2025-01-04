# BLEtoMQTT
## Introduction
 Android foreground service to provide a bidirectional bridge between BLE devices and an MQTT broker. A Setup.json file defines MQTT parameters and BLE MAC addresses to scan for.

 The app has been tried on Samsung S9, S9+ and 'Galaxy Tab A'. Running Android 10 and 11. 
## Installation
Clone repository and open in Android IDE. Build apk and install on your device.
Using an Android file manager create a new directory in the user root called 'ble2mqtt' (/mnt/sdcard/ble2mqtt) in which to place the setup.json file.

Go in to Settings->App->BLEtoMQTT and grant location and file privileges

Restart app and on user screen press service start button - a permanent notification should be generated. If no notification is generated then refer to Problems section.

## setup.json
## Privileges
## Problems
The code is lite on error trapping or user warnings. Initial problems are likely due to 
  - bluetooth not enabled or location privileges not given
  - internet access not available so cannot find the MQTT broker
  - cannot find the setup.json file or file privileges have not been given
  - setup.json data structure problems

For further clues view the LogCat 
