# BLEtoMQTT
## Introduction
 Android foreground service to provide a bidirectional bridge between BLE devices and an MQTT broker. A setup.json file defines MQTT parameters and BLE MAC addresses to scan for.

For each BLE device detected BLEtoMQTT will discover all exposed services and characteristics creating MQTT topics for each. Those characteristics which are writeable will automatically be 'subscribed' from the MQTT broker.

Two external libraries are used :
  - org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5
  - com.github.weliem:blessed-android

The author uses this application to connect remote custom ESP32 devices to his Home Assistant server via a Zerotier VPN. 
The GitHub repository contains an example setup.json and a prebuilt apk used to monitor/control a boat on a mooring or at anchor.

 The app has been tried on Samsung S6, S9, S9+ and 'Galaxy Tab A'. Running Android 7.0, 10 and 11. 
## Installation
Clone repository and open in Android IDE. Build apk and install on your device.
Using an Android file manager create a new directory in the user root called 'bletomqtt' (/mnt/sdcard/bletomqtt) in which to place the setup.json file.

Go in to Settings->App->BLEtoMQTT and grant location and file privileges

Restart app and on user screen press 'service start' button - a permanent notification should be generated. If no notification is generated then refer to Problems section.

## setup.json

Minimum setup.json would just contain mqtt credentials and BLE MAC addresses to scan for :
###
		{
			"bletomqtt": [{
				"mqtt": {
					"IP": "tcp://192.168.192.40:1883",
					"user": "Guest",
					"pswd": "0123456789"
				}
			},
			{
				"devices": [{
					"address": "B0:B2:1C:A6:19:BA"
				},
				
					"address": "B5:B2:1C:C6:19:11"
				}]
			}]
		}


The example setup.json included in the main directory shows how to convert UUIDs into human friendly MQTT topics. A data template formatter, called BITWISE, is also shown which can be used to convert integer (ASCII) fields into json with individual binary sensors and controls.

## BLE Pairing/Bonding

The code makes no special allowances for bonding to the BLE device, this is taken care of in the BLESSED library. If a device requires a passkey to BOND then a user interface screen is presented. The bonding memory is lost if the application is stopped or the Android device is rebooted.

On occasion I have noticed that a bonded BLE device needs to be power cycled to be seen by a new Android device. 

## Permissions

    RECEIVE_BOOT_COMPLETED
    POST_NOTIFICATIONS
    SYSTEM_ALERT_WINDOW
    READ_EXTERNAL_STORAGE
    MANAGE_EXTERNAL_STORAGE
    ACTION_MANAGE_OVERLAY_PERMISSION
    INTERNET
    ACCESS_COARSE_LOCATION
    ACCESS_FINE_LOCATION
    FOREGROUND_SERVICE
    FOREGROUND_SERVICE_LOCATION

## Problems
The code is lite on error trapping or user warnings. Initial problems will likely be due to 
  - bluetooth not enabled or location privileges not given
  - internet access not available so cannot find the MQTT broker
  - cannot find the setup.json file or file privileges have not been given
  - setup.json data structure problems

For further clues view the LogCat 


