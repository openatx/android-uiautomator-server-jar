#!/bin/bash
# 端到端测试：编译 debug APK -> 推送到真机 -> 用 app_process 启动 server -> 跑 pytest。
#
# 用法：
#   tests/run_e2e.sh                     # 自动选第一台在线设备
#   ANDROID_SERIAL=xxx tests/run_e2e.sh
#   tests/run_e2e.sh tests/test_jsonrpc.py -k ping   # 额外参数透传给 pytest
#
# 注意：device 上同一时间只允许一个 UiAutomation 连接，因此 port 参数测试
# （会自行启停 server）必须在主 server 启动前跑完。
#
# 环境变量：
#   ANDROID_SERIAL  指定设备序列号
#   PYTHON          指定 python（默认 <repo>/.venv-tests/bin/python）
#   PORT            被测服务端口（默认 9008）

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PYTHON="${PYTHON:-$ROOT/.venv-tests/bin/python}"
PORT="${PORT:-9008}"
DEVICE_APK="/data/local/tmp/u2.apk"

if [ ! -x "$PYTHON" ]; then
    echo "python not found: $PYTHON" >&2
    echo "请先执行: uv venv .venv-tests && uv pip install -r tests/requirements.txt --python .venv-tests/bin/python" >&2
    exit 1
fi

SERIAL="${ANDROID_SERIAL:-$(adb devices | awk '$2 == "device" {print $1; exit}')}"
if [ -z "${SERIAL:-}" ]; then
    echo "没有在线的 adb 设备" >&2
    exit 1
fi
export ANDROID_SERIAL="$SERIAL"

SERVER_LOG="$(mktemp -t u2-server)"
SERVER_PID=""

kill_server() {
    adb -s "$SERIAL" shell pkill -f "com.github.uiautomator2.Console" >/dev/null 2>&1 || true
}

cleanup() {
    if [ -n "$SERVER_PID" ]; then
        kill "$SERVER_PID" >/dev/null 2>&1 || true
        wait "$SERVER_PID" >/dev/null 2>&1 || true
    fi
    kill_server
    adb -s "$SERIAL" forward --remove "tcp:$PORT" >/dev/null 2>&1 || true
    if [ -s "$SERVER_LOG" ]; then
        echo "--- device server log ($SERVER_LOG)"
        tail -20 "$SERVER_LOG"
    fi
}
trap cleanup EXIT

echo "==> device: $SERIAL"
echo "==> build debug apk"
./gradlew assembleDebug -q

echo "==> push apk to $DEVICE_APK"
adb -s "$SERIAL" push app/build/outputs/apk/debug/app-debug.apk "$DEVICE_APK" >/dev/null

kill_server

echo "==> run port argument tests (no server running)"
"$PYTHON" -m pytest tests/test_port_args.py -v
kill_server

adb -s "$SERIAL" forward "tcp:$PORT" "tcp:$PORT" >/dev/null

echo "==> start server on port $PORT"
adb -s "$SERIAL" shell "CLASSPATH=$DEVICE_APK app_process / com.github.uiautomator2.Console -p $PORT" >"$SERVER_LOG" 2>&1 &
SERVER_PID=$!

echo "==> wait for server"
SERVER_READY=0
for _ in $(seq 1 30); do
    if "$PYTHON" - "$PORT" <<'EOF' >/dev/null 2>&1
import sys
import httpx
port = sys.argv[1]
try:
    resp = httpx.post(f"http://127.0.0.1:{port}/jsonrpc/0",
                      json={"id": 1, "method": "ping"}, timeout=1)
    sys.exit(0 if resp.json().get("result") == "pong" else 1)
except Exception:
    sys.exit(1)
EOF
    then
        SERVER_READY=1
        break
    fi
    sleep 1
done

if [ "$SERVER_READY" != "1" ]; then
    echo "server 启动失败（端口 $PORT 未响应 ping）" >&2
    exit 1
fi

echo "==> run e2e tests"
"$PYTHON" -m pytest tests/ -v --ignore=tests/test_port_args.py "$@"
