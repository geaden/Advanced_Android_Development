#!/usr/bin/env bash
# Enable Bluetooth debugging for Android Wear
# Make bluetooth debug is enabled on devices.
export DEBUG_PORT=4444
adb forward tcp:$DEBUG_PORT localabstract:/adb-hub
adb connect 127.0.0.1:$DEBUG_PORT
