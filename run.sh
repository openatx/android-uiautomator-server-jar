#!/bin/bash

# ref: https://github.com/Genymobile/scrcpy/blob/master/doc/develop.md

set -e

adb forward tcp:5005 tcp:5005
adb forward tcp:9008 tcp:9008
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/u2.apk
adb shell CLASSPATH=/data/local/tmp/u2.apk app_process / com.github.uiautomator2.Console "$@"

# 8 and 8-
# adb shell CLASSPATH=/data/local/tmp/u2.apk app_process -agentlib:jdwp=transport=dt_socket,suspend=y,server=y,address=5005 / com.github.uiautomator2.Console

# 9+
# adb shell CLASSPATH=/data/local/tmp/u2.apk app_process -XjdwpProvider:internal -XjdwpOptions:transport=dt_socket,suspend=y,server=y,address=5005  / com.github.uiautomator2.Console
