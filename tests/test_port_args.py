import shlex
import subprocess
import adbutils
import pytest

_DEVICE_APK = "/data/local/tmp/udt/atx-uia2.jar"
_MAIN = "com.wetest.uia2.Main"
_TEST_PORT = "19009"


@pytest.fixture(scope="module")
def adb_device():
    return adbutils.AdbClient().device()


def _run_main(device, port_arg, timeout=5):
    """Invoke Main on the device with -p port_arg; return (exit_code, combined_output)."""
    result = subprocess.run(
        ["adb", "-s", device.serial, "shell",
         f"CLASSPATH={_DEVICE_APK} app_process / {_MAIN} -p {shlex.quote(port_arg)} 2>&1; echo EXIT:$?"],
        capture_output=True, text=True, timeout=timeout,
    )
    exit_code = None
    for line in result.stdout.splitlines():
        if line.startswith("EXIT:"):
            value = line.split(":", 1)[1]
            try:
                exit_code = int(value)
            except ValueError:
                pytest.fail(f"malformed EXIT: value {value!r} in adb output: {result.stdout!r}")
    assert exit_code is not None, f"no EXIT: line in output — device offline? adb stdout: {result.stdout!r}"
    return exit_code, result.stdout


@pytest.mark.parametrize("port,expected", [
    ("abc",   "Invalid port: abc"),
    ("9k",    "Invalid port: 9k"),
    ("0",     "Invalid port: 0"),
    ("65536", "Invalid port: 65536"),
])
def test_invalid_port_exits_with_friendly_message(adb_device, port, expected):
    exit_code, output = _run_main(adb_device, port)
    assert exit_code == 1, f"expected exit 1, got {exit_code}. output: {output}"
    assert expected in output, f"expected {expected!r} in output: {output}"


def test_valid_port_starts_server(adb_device):
    """Server should start (not exit immediately) when given a valid port."""
    try:
        exit_code, output = _run_main(adb_device, _TEST_PORT, timeout=2)
        pytest.fail(f"server exited unexpectedly with code {exit_code}: {output}")
    except subprocess.TimeoutExpired:
        pass  # server still running — correct
    finally:
        subprocess.run(
            ["adb", "-s", adb_device.serial, "shell",
             f"pkill -f \"com.wetest.uia2.Main -p {_TEST_PORT}\""],
            capture_output=True,
            timeout=10,
        )
