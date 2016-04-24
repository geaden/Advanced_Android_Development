#!/usr/bin/env bash
export DEBUG_PORT=4444
adb -s $1 forward --remove tcp:$DEBUG_PORT
