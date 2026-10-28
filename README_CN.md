[English](README.md) | [简体中文](README_CN.md)

# 简介
[![Android CI](https://github.com/openatx/android-uiautomator-server-jar/actions/workflows/test.yml/badge.svg)](https://github.com/openatx/android-uiautomator-server-jar/actions/workflows/test.yml)

[UIAutomator](http://developer.android.com/tools/testing/testing_ui.html) 是做 Android UI 测试的绝佳工具，但要跑起来，你得写 Java 代码、编译、安装 jar 再运行，这对所有测试人员来说步骤都太繁琐了。

本项目在 Android 设备上构建一个轻量的 JSON-RPC 服务器，这样我们只需要编写 PC 端脚本就能完成 UIAutomator 测试。

# 如何构建

## 构建 APK

方法 1：

在 `Android Studio` 中依次点击 `Build` -> `Build App Bundle/APK` -> `Build APK`

方法 2：

```sh
./gradlew assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`

## 安装 APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

应用包名为 `com.github.uiautomator2`。使用内置的 AdbKeyboard 输入法需要先安装 APK。

## 启动 Jar 服务

```bash
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp
adb shell CLASSPATH=/data/local/tmp/app-debug.apk app_process / com.github.uiautomator2.Console [-p port]
```

未指定端口时默认为 9008，使用 `-h` 查看所有选项。

## 测试服务

```
adb forward tcp:9008 tcp:9008
curl -X POST -d '{"jsonrpc": "2.0", "id": "1f0f2655716023254ed2b57ba4198815", "method": "deviceInfo", "params": {}}' 'http://127.0.0.1:9008/jsonrpc/0'
# 期望输出类似
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

## JSON-RPC 测试工具

交互式 JSON-RPC 方法测试脚本，用于快速调试和验证各个 RPC 方法。

```bash
# 交互模式（分类菜单选择方法 → 输入参数 → 查看结果）
python3 tools/test_jsonrpc.py

# 直接调用
python3 tools/test_jsonrpc.py ping
python3 tools/test_jsonrpc.py click 100 200
python3 tools/test_jsonrpc.py dumpWindowHierarchy false
```

脚本自动执行 `adb forward tcp:9008 tcp:9008`，无需手动转发。

方法定义文件 `tools/methods.json` 由 `AutomatorService.java` 接口同步生成，含所有方法的名称、参数类型、描述和示例值。

## 同步 methods.json

当 `AutomatorService.java` 接口有新增、删除或修改方法时，使用 sync-methods skill 同步更新 `tools/methods.json`。

在 opencode 中对 AI 说：

```
同步 methods
```

AI 将自动：解析 Java 源码 → 提取所有方法 → 生成 methods.json → 校验 JSON 合法性。

> 该 skill 位于 `.opencode/skills/sync-methods/SKILL.md`，纯 AI 驱动，无需额外脚本。

# AdbKeyboard（ATX Keyboard）

APK 内置一个用于文本输入的输入法，安装后在输入法列表中显示为 **ATX Keyboard**，组件名为 `com.github.uiautomator2/.AdbKeyboard`。

> 从旧包 `com.github.uiautomator` 升级时，新旧包是两个独立应用，安装新包不会覆盖旧包。建议卸载旧包（`adb uninstall com.github.uiautomator`），避免输入法列表里出现两个同名的 ATX Keyboard。

## 启用并切换输入法

```bash
adb shell ime enable com.github.uiautomator2/.AdbKeyboard
adb shell ime set com.github.uiautomator2/.AdbKeyboard
```

## 通过广播输入文本

`text` 推荐传 base64 编码的 UTF-8 文本；如果不是合法 base64（或解码后不是合法 UTF-8），会按原始文本直接输入。

```bash
TEXT=$(echo -n "Hello World" | base64)
adb shell am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text "$TEXT"
```

也可以直接传原始文本，中文输入更方便：

```bash
adb shell am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text "123你好" --ei interval 100
```

> 文本中带空格时，需要把整条 `am broadcast` 命令包起来，避免被 `adb shell` 拆成多个参数：
> `adb shell "am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text 'hello 世界'"`

可选参数 `interval`（单位 ms）控制逐字符输入的间隔，默认 `0` 表示整段一次输入：

```bash
# 每 100ms 输入一个字符
adb shell am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text "$TEXT" --ei interval 100
```

> 需要先让目标输入框获得焦点（即存在 InputConnection），广播才会生效。

## 支持的广播 action

| action | extras | 说明 |
| --- | --- | --- |
| `ADB_KEYBOARD_INPUT_TEXT` | `--es text <base64>`，可选 `--ei interval <ms>` | 输入文本 |
| `ADB_KEYBOARD_SET_TEXT` | 同上 | 清空当前内容后输入 |
| `ADB_KEYBOARD_CLEAR_TEXT` | - | 清空当前输入框 |
| `ADB_KEYBOARD_INPUT_KEYCODE` | `--ei code <keycode>` | 发送按键，如 `66` 为回车 |
| `ADB_KEYBOARD_EDITOR_CODE` | `--ei code <action>` | 执行编辑器 action，如 `3` 为 Search |
| `ADB_KEYBOARD_GET_CLIPBOARD` | - | 读取剪贴板，结果以 base64 返回 |
| `ADB_KEYBOARD_HIDE` / `ADB_KEYBOARD_SHOW` | - | 隐藏 / 显示输入法 |
| `ADB_KEYBOARD_SMART_ENTER` | - | 智能回车（按当前输入框的 imeOptions 执行） |

## 隐藏输入法

在键盘上快速下滑，或在顶部标题栏下滑即可收起输入法。

# 相关资源
- [Google UiAutomator 教程](https://developer.android.com/training/testing/ui-testing/uiautomator-testing?hl=zh-cn)
- [Google UiAutomator API](https://developer.android.com/reference/kotlin/androidx/test/uiautomator/package-summary)
- [uiautomator Maven 仓库](https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator)
- [androidx.test.uiautomator 更新日志](https://developer.android.com/jetpack/androidx/releases/test-uiautomator)

# 说明

有任何想法，欢迎邮件联系 codeskyblue@gmail.com 或 [提交 issue](https://github.com/openatx/android-uiautomator-server/issues/new)。

# 依赖

- [nanohttpd](https://github.com/NanoHttpd/nanohttpd)
- [jsonrpc4j](https://github.com/briandilley/jsonrpc4j)
- [jackson](https://github.com/FasterXML/jackson)
- [androidx.test.uiautomator](https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator-v18)

# 已实现功能

- [x] 支持 unicode 输入
- [x] AdbKeyboard 支持通过 `interval` 参数控制逐字符输入间隔
- [x] AdbKeyboard 支持下滑隐藏输入法

# 许可证

本项目基于 [MIT License](LICENSE) 开源。

# 致谢
- [xiaocong](https://github.com/xiaocong)
- https://github.com/willerce/WhatsInput
- https://github.com/senzhk/ADBKeyBoard
- https://github.com/amotzte/android-mock-location-for-development
- https://github.com/gladed/gradle-android-git-version
