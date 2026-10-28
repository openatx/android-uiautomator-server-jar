[English](README.md) | [简体中文](README_CN.md)

# Purpose
[![Android CI](https://github.com/openatx/android-uiautomator-server-jar/actions/workflows/test.yml/badge.svg)](https://github.com/openatx/android-uiautomator-server-jar/actions/workflows/test.yml)

[UIAutomator](http://developer.android.com/tools/testing/testing_ui.html) is a
great tool to perform Android UI testing, but to do it, you have to write java
code, compile it, install the jar, and run. It's a complex steps for all
testers...

This project is to build a light weight jsonrpc server in Android device, so
that we can just write PC side script to write UIAutomator tests.

# How to build

## Build APK

Method 1:

In `Android Studio` -> `Build` -> `Build App Bundle/APK` -> `Build APK`

Method 2:

```sh
./gradlew assembleDebug
```

Now apk file can be found in `app/build/outputs/apk/debug/app-debug.apk`

## Install APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The application ID is `com.github.uiautomator2`. Installing the APK is required to use the bundled AdbKeyboard input method.

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

## JSON-RPC Test Tool

Interactive JSON-RPC script for quickly debugging and verifying RPC methods.

```bash
# Interactive mode (pick a method from categorized menus, enter params, see results)
python3 tools/test_jsonrpc.py

# Direct invocation
python3 tools/test_jsonrpc.py ping
python3 tools/test_jsonrpc.py click 100 200
python3 tools/test_jsonrpc.py dumpWindowHierarchy false
```

The script runs `adb forward tcp:9008 tcp:9008` automatically, no manual forwarding needed.

Method definitions in `tools/methods.json` are generated from the `AutomatorService.java` interface, including the name, parameter types, description and example values of every method.

## Sync methods.json

Whenever a method in `AutomatorService.java` is added, removed or changed, use the sync-methods skill to update `tools/methods.json`.

Ask the AI in opencode:

```
sync methods
```

The AI will parse the Java sources → extract every method → generate methods.json → validate the JSON.

> The skill lives at `.opencode/skills/sync-methods/SKILL.md`. It is entirely AI driven and needs no extra scripts.

# AdbKeyboard (ATX Keyboard)

The APK bundles an input method for text input. After installation it appears in the IME list as **ATX Keyboard**, with the component name `com.github.uiautomator2/.AdbKeyboard`.

> Upgrading from the old `com.github.uiautomator` package? The two packages are separate apps, so installing the new APK does not replace the old one. Uninstall the old app (`adb uninstall com.github.uiautomator`) to avoid duplicate ATX Keyboard entries in the IME list.

## Enable and switch IME

```bash
adb shell ime enable com.github.uiautomator2/.AdbKeyboard
adb shell ime set com.github.uiautomator2/.AdbKeyboard
```

## Input text via broadcast

The `text` parameter should be base64-encoded UTF-8 text. Plain text is also accepted: if it is not valid base64 or does not decode to valid UTF-8, it is typed as-is.

```bash
TEXT=$(echo -n "Hello World" | base64)
adb shell am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text "$TEXT"
```

Plain text works too, which is handy for Chinese input:

```bash
adb shell am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text "123你好" --ei interval 100
```

> If the text contains spaces, wrap the whole `am broadcast` command so `adb shell` does not split it:
> `adb shell "am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text 'hello 世界'"`

The optional `interval` parameter (in ms) controls the delay between characters. The default `0` commits the whole string at once:

```bash
# Type one character every 100ms
adb shell am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text "$TEXT" --ei interval 100
```

> The target input field must be focused (an InputConnection must exist) for the broadcast to take effect.

## Supported broadcast actions

| action | extras | description |
| --- | --- | --- |
| `ADB_KEYBOARD_INPUT_TEXT` | `--es text <base64>`, optional `--ei interval <ms>` | Type text |
| `ADB_KEYBOARD_SET_TEXT` | same as above | Clear the field, then type |
| `ADB_KEYBOARD_CLEAR_TEXT` | - | Clear the current field |
| `ADB_KEYBOARD_INPUT_KEYCODE` | `--ei code <keycode>` | Send a key event, e.g. `66` for Enter |
| `ADB_KEYBOARD_EDITOR_CODE` | `--ei code <action>` | Perform an editor action, e.g. `3` for Search |
| `ADB_KEYBOARD_GET_CLIPBOARD` | - | Read the clipboard; the result is returned as base64 |
| `ADB_KEYBOARD_HIDE` / `ADB_KEYBOARD_SHOW` | - | Hide / show the input method |
| `ADB_KEYBOARD_SMART_ENTER` | - | Smart Enter (follows the field's imeOptions) |

## Hide the keyboard

Flick down on the keyboard, or swipe down on the title bar.

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
- [x] AdbKeyboard supports a per-character input interval (`interval`)
- [x] AdbKeyboard supports swipe-down to hide

# License

This project is released under the [MIT License](LICENSE).

# Thanks to
- [xiaocong](https://github.com/xiaocong)
- https://github.com/willerce/WhatsInput
- https://github.com/senzhk/ADBKeyBoard
- https://github.com/amotzte/android-mock-location-for-development
- https://github.com/gladed/gradle-android-git-version
