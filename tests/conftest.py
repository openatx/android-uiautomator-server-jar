# coding: utf-8
# author: codeskyblue

import pytest
import adbutils
import httpx


@pytest.fixture
def device():
    d = adbutils.AdbClient().device()
    d.forward("tcp:9008", "tcp:9008")
    yield d


class JSONRPCError(Exception):
    pass


def jsonrpc_call(device: adbutils.AdbDevice, method: str, params: dict = None):
    response = httpx.post("http://127.0.0.1:9008/jsonrpc/0", json={"id": 1, "method": method, "params": params})
    data = response.json()
    assert data['id'] == 1
    if 'error' in data:
        raise JSONRPCError(data['error'])
    return data['result']
    

class JSONRPCProxy:
    def __init__(self, device: adbutils.AdbDevice):
        self.device = device

    def __getattr__(self, name):
        def wrapper(*args):
            return jsonrpc_call(self.device, name, args)
        return wrapper


@pytest.fixture
def jsonrpc(device: adbutils.AdbDevice):
    return JSONRPCProxy(device)
