#!/usr/bin/env bash
export DEBUG_PORT=4444
adb forward --remove tcp:$DEBUG_PORT
