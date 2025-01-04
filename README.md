# BLEtoMQTT
## Introduction
 Android foreground service to provide a bidirectional bridge between BLE devices and an MQTT broker. A Setup.json file defines MQTT parameters and BLE MAC addresses to scan for.

 The app has been used on Samsung S9, S9+ and Galaxy Tab A. Running android 10 and 11. 
## Installation
## Setup.json
## Privileges
## Problems
The code is lite on error trapping or user warnings. Initial problems are likely due to 
  - bluetooth not enabled or location privileges not given
  - internet access not available so cannot find the MQTT broker
  - cannot find the setup.json file or file privileges have not been given
  - setup.json data structure problems

For further clues view the LogCat 
