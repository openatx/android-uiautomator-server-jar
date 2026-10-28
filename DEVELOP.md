## Use APK as commandline

运行

```bash
APK=$(adb shell pm path com.github.uiautomator2 | cut -d: -f2)
adb shell export CLASSPATH="$APK"\; \
    exec app_process / com.wetest.uia2.Main -h
```