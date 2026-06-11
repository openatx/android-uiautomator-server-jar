# coding: utf-8
# author: codeskyblue

import pytest
import adbutils
import httpx

_DEFAULT_PORT = 9008


@pytest.fixture
def device():
    d = adbutils.AdbClient().device()
    yield d


class JSONRPCError(Exception):
    pass


def jsonrpc_call(device: adbutils.AdbDevice, method: str, params: dict = None, port: int = _DEFAULT_PORT):
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
    return JSONRPCProxy(device)
