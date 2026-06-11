import shlex
import subprocess
import adbutils
import pytest

_DEVICE_APK = "/data/local/tmp/udt/atx-uia2.jar"
_MAIN = "com.wetest.uia2.Main"


@pytest.fixture(scope="module")
def adb_device():
    return adbutils.AdbClient().device()


def _run_main(device, port_arg, timeout=5):
    """Invoke Main on the device with port_arg; return (exit_code, combined_output)."""
    result = subprocess.run(
        ["adb", "-s", device.serial, "shell",
         f"CLASSPATH={_DEVICE_APK} app_process / {_MAIN} {shlex.quote(port_arg)} 2>&1; echo EXIT:$?"],
        capture_output=True, text=True, timeout=timeout,
    )
    exit_code = -1
    for line in result.stdout.splitlines():
        if line.startswith("EXIT:"):
            exit_code = int(line.split(":", 1)[1])
    assert exit_code != -1, f"no EXIT: line in output — device offline? adb stdout: {result.stdout!r}"
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
