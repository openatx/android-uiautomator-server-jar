# 测试说明

## 端到端测试（推荐）

一条命令完成「编译 → 推 APK 到真机 → 启动 server → 跑 pytest」：

```bash
uv venv .venv-tests
uv pip install -r tests/requirements.txt --python .venv-tests/bin/python

tests/run_e2e.sh                     # 自动选第一台在线设备
ANDROID_SERIAL=xxx tests/run_e2e.sh  # 指定设备
tests/run_e2e.sh tests/test_jsonrpc.py -k ping   # 额外参数透传给 pytest
```

脚本会：

1. `./gradlew assembleDebug`
2. 把 APK 推到 `/data/local/tmp/u2.apk`
3. 先跑 `test_port_args.py`（会自行启停 server）
4. 用 `app_process / com.github.uiautomator2.Console -p 9008` 启动 server，`adb forward` 9008
5. 跑其余测试，结束时清理 server 进程和端口转发

注意：同一台设备只允许一个 UiAutomation 连接，所以 port 参数测试必须在主 server 启动前跑。

## 手动跑测试

先安装依赖

```bash
pip install -r requirements.txt
```

安装被测示例应用（`test_selector.py` 需要，未安装时相关用例会 skip）
https://github.com/openatx/u2testdemo/releases/download/0.1.0/app-debug.apk

先将 APK 以 jar 形式 run 起来，然后运行 pytest 测试

```bash
pytest --html report.html
```

已知环境限制：Android 10+ 只有前台应用能读剪贴板，`test_clipboard` 在 API 29+ 上会 skip。
