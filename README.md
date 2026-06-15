# Purpose
[![Android CI](https://github.com/openatx/android-uiautomator-server/actions/workflows/android.yml/badge.svg)](https://github.com/openatx/android-uiautomator-server/actions/workflows/android.yml)

[UIAutomator](http://developer.android.com/tools/testing/testing_ui.html) is a
great tool to perform Android UI testing, but to do it, you have to write java
code, compile it, install the jar, and run. It's a complex steps for all
testers...

This project is to build a light weight jsonrpc server in Android device, so
that we can just write PC side script to write UIAutomator tests.

# How to build

## Build APK

In `Android Studio` -> `Build` -> `Build App Bundle/APK` -> `Build APK`

Now apk file can be found in `app/build/outputs/apk/debug/app-debug.apk`

## Launch Jar server

```bash
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp
adb shell CLASSPATH=/data/local/tmp/app-debug.apk app_process / com.wetest.uia2.Main [-p port]
```
If no port is provided, it defaults to 9008. Use `-h` to see all options.

## Test server

```
adb forward tcp:9008 tcp:9008
curl -X POST -d '{"jsonrpc": "2.0", "id": "1f0f2655716023254ed2b57ba4198815", "method": "deviceInfo", "params": {}}' 'http://127.0.0.1:9008/jsonrpc/0'
# Expect output like
{'currentPackageName': 'com.smartisanos.launcher',
 'displayHeight': 1920,
 'displayRotation': 0,
 'displaySizeDpX': 360,
 'displaySizeDpY': 640,
 'displayWidth': 1080,
 'productName': 'surabaya',
 'screenOn': True,
 'sdkInt': 23,
 'naturalOrientation': True}
```

# Resources
- [Google UiAutomator Tutorial](https://developer.android.com/training/testing/ui-testing/uiautomator-testing?hl=zh-cn)
- [Google UiAutomator API](https://developer.android.com/reference/kotlin/androidx/test/uiautomator/package-summary)
- [Maven repository of uiautomator](https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator)
- [androidx.test.uiautomator release notes](https://developer.android.com/jetpack/androidx/releases/test-uiautomator)

# Notes

If you have any idea, please email codeskyblue@gmail.com or [submit tickets](https://github.com/openatx/android-uiautomator-server/issues/new).

# Dependencies

- [nanohttpd](https://github.com/NanoHttpd/nanohttpd)
- [jsonrpc4j](https://github.com/briandilley/jsonrpc4j)
- [jackson](https://github.com/FasterXML/jackson)
- [androidx.test.uiautomator](https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator-v18)

# Added features

- [x] support unicode input

# Thanks to
- [xiaocong](https://github.com/xiaocong)
- https://github.com/willerce/WhatsInput
- https://github.com/senzhk/ADBKeyBoard
- https://github.com/amotzte/android-mock-location-for-development
- https://github.com/gladed/gradle-android-git-version
