# coding: utf-8
# author: codeskyblue

import os

import pytest
import adbutils
import httpx

_DEFAULT_PORT = 9008  # must match Console.DEFAULT_PORT in Console.java


def get_device() -> adbutils.AdbDevice:
    """Pick the device from ANDROID_SERIAL, otherwise the only connected one."""
    client = adbutils.AdbClient()
    serial = os.environ.get("ANDROID_SERIAL")
    if serial:
        return client.device(serial)
    return client.device()


@pytest.fixture
def device():
    d = get_device()
    yield d


class JSONRPCError(Exception):
    pass


def jsonrpc_call(device: adbutils.AdbDevice, method: str, params=None, port: int = _DEFAULT_PORT):
    response = httpx.post(f"http://127.0.0.1:{port}/jsonrpc/0", json={"id": 1, "method": method, "params": params})
    data = response.json()
    assert data['id'] == 1
    if 'error' in data:
        raise JSONRPCError(data['error'])
    return data['result']


class JSONRPCProxy:
    def __init__(self, device: adbutils.AdbDevice, port: int = _DEFAULT_PORT):
        self.device = device
        self.port = port
        device.forward(f"tcp:{port}", f"tcp:{port}")

    def __getattr__(self, name):
        def wrapper(*args):
            return jsonrpc_call(self.device, name, args, port=self.port)
        return wrapper


@pytest.fixture
def jsonrpc(device: adbutils.AdbDevice):
    proxy = JSONRPCProxy(device)
    yield proxy
    try:
        device.forward_remove(f"tcp:{proxy.port}")
    except Exception:
        pass
