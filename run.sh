#!/bin/bash

# ref: https://github.com/Genymobile/scrcpy/blob/master/doc/develop.md

set -e

adb forward tcp:5005 tcp:5005
adb forward tcp:9008 tcp:9008
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/udt/atx-uia2.jar
adb shell CLASSPATH=/data/local/tmp/udt/atx-uia2.jar app_process / com.wetest.uia2.Main "$@"

# 8 and 8-
# adb shell CLASSPATH=/data/local/tmp/udt/atx-uia2.jar app_process -agentlib:jdwp=transport=dt_socket,suspend=y,server=y,address=5005 / com.wetest.uia2.Main

# 9+
# adb shell CLASSPATH=/data/local/tmp/udt/atx-uia2.jar app_process -XjdwpProvider:internal -XjdwpOptions:transport=dt_socket,suspend=y,server=y,address=5005  / com.wetest.uia2.Main
