#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import base64
import time
import adbutils
import pytest
import httpx


@pytest.fixture
def device():
    d =  adbutils.AdbClient().device()
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


def test_raise_jsonrpc_error(jsonrpc: JSONRPCProxy):
    with pytest.raises(JSONRPCError):
        jsonrpc.not_exist_method()


def test_ping(jsonrpc: JSONRPCProxy):
    result = jsonrpc.ping()
    assert result == "pong"


@pytest.mark.skip(reason="need to fix")
def test_toast(jsonrpc: JSONRPCProxy):
    result = jsonrpc.clearLastToast()
    assert result
    
    result = jsonrpc.makeToast(["hello world", 1])
    assert result == True

    result = jsonrpc.getLastToast([1])
    assert result == "hello world"
    

def test_deviceInfo(jsonrpc: JSONRPCProxy):
    result = jsonrpc.deviceInfo()
    assert isinstance(result, dict)


@pytest.mark.parametrize("x, y", [(100, 100), (200, 200)])
def test_click(jsonrpc: JSONRPCProxy, x, y):
    result = jsonrpc.click(x, y)
    assert result == True


def test_click_with_duration(jsonrpc: JSONRPCProxy):
    result = jsonrpc.click(1, 1, 0.2)
    assert result == True


def test_drag(jsonrpc: JSONRPCProxy):
    result = jsonrpc.drag(100, 100, 200, 200, 20)
    assert result == True


def test_swipe(jsonrpc: JSONRPCProxy):
    result = jsonrpc.swipe(100, 100, 200, 200, 20)
    assert result == True
    

def test_swipePoints(jsonrpc: JSONRPCProxy):
    result = jsonrpc.swipePoints([[100, 100], [200, 200]], 20)
    assert result == True


@pytest.mark.skip(reason="useless")
def test_injectInputEvent(jsonrpc: JSONRPCProxy):
    result = jsonrpc.injectInputEvent(3, 100, 100, 0)
    assert result == False
    
    result = jsonrpc.injectInputEvent(0, 100, 100, 0) # down
    assert result == True
    
    result = jsonrpc.injectInputEvent(2, 100, 100, 0) # move
    assert result == True
    
    result = jsonrpc.injectInputEvent(1, 100, 100, 0) # up
    assert result == True
    

def test_dumpWindowHierarchy(jsonrpc: JSONRPCProxy):
    for args in [(True,), (False,), (False, 10), (False, 0)]:
        result = jsonrpc.dumpWindowHierarchy(*args)
        assert result.startswith("<?xml") and result.rstrip().endswith("</hierarchy>")


def test_takeScreenshot(jsonrpc: JSONRPCProxy):
    result = jsonrpc.takeScreenshot(1, 80)
    # check if is JPEG
    jpg_raw = base64.b64decode(result)
    assert jpg_raw.startswith(b"\xff\xd8\xff\xe0\x00\x10JFIF")


def test_freezeRotation(jsonrpc: JSONRPCProxy):
    jsonrpc.freezeRotation(True)
    jsonrpc.freezeRotation(False)


@pytest.mark.parametrize("value", ["left", "l", "right", "r", "natural", "n"])
def test_setOrientation(jsonrpc: JSONRPCProxy, value: str):
    jsonrpc.setOrientation(value)


def test_getLastTraversedText(jsonrpc: JSONRPCProxy):
    jsonrpc.clearLastTraversedText()
    
    result = jsonrpc.getLastTraversedText()
    assert result is None


def test_openNotification(jsonrpc: JSONRPCProxy):
    result = jsonrpc.openNotification()
    assert result == True


def test_openQuickSettings(jsonrpc: JSONRPCProxy):
    result = jsonrpc.openQuickSettings()
    assert result == True


def test_watcher(jsonrpc: JSONRPCProxy):
    jsonrpc.resetWatcherTriggers()
    jsonrpc.removeWatcher("not_exist")
    
    triggered = jsonrpc.hasWatcherTriggered("not_exist")
    assert isinstance(triggered, bool)
    assert not triggered
    
    triggered = jsonrpc.hasAnyWatcherTriggered()
    assert isinstance(triggered, bool)
    
    jsonrpc.runWatchers()
    
    jsonrpc.getWatchers() # -> List[str]
    # TODO
    # void registerClickUiObjectWatcher(String name, Selector[] conditions, Selector target)
    # void registerPressKeyskWatcher(String name, Selector[] conditions, String[] keys)


@pytest.mark.parametrize("key", ["home", "back", "left", "right", "up", "down", "center", "menu", "search", "enter", "delete", "del", "recent", "volume_up", "volume_down", "volume_mute", "camera", "power"])
def test_pressKey(jsonrpc: JSONRPCProxy, key: str):
    # boolean pressKey(String key) 
    result = jsonrpc.pressKey(key)
    assert result == True
    time.sleep(.1)


def test_pressKeyCode(jsonrpc: JSONRPCProxy):
    # boolean pressKeyCode(int keyCode)
    assert jsonrpc.pressKeyCode(3) is True
    
    # boolean pressKeyCode(int keyCode, int metaState)
    assert jsonrpc.pressKeyCode(3, 0) is True
    assert jsonrpc.pressKeyCode(3, 1) is True


def test_wakeUp(jsonrpc: JSONRPCProxy):
    jsonrpc.wakeUp()


def test_sleep(jsonrpc: JSONRPCProxy):
    jsonrpc.sleep()


def test_isScreenOn(jsonrpc: JSONRPCProxy):
    result = jsonrpc.isScreenOn()
    assert isinstance(result, bool)


def test_waitForIdle(jsonrpc: JSONRPCProxy):
    jsonrpc.waitForIdle(10) # ms


def test_waitForWindowUpdate(jsonrpc: JSONRPCProxy):
    boolean = jsonrpc.waitForWindowUpdate("com.android.systemui", 10) # ms
    assert boolean == False


def test_Configurator(jsonrpc: JSONRPCProxy):
    conf = jsonrpc.getConfigurator()
    conf['actionAcknowledgmentTimeout'] = conf['actionAcknowledgmentTimeout'] + 1
    assert isinstance(conf, dict)
    new_conf = jsonrpc.setConfigurator(conf)
    assert new_conf == conf


# TODO
# void setClipboard(String label, String text)
# String getClipboard()

# TODO
# void clearTextField(Selector obj)
# String getText(Selector obj)
# boolean setText(Selector obj, String text)
# boolean click(Selector obj)
# boolean click(Selector obj, String corner)
# boolean clickAndWaitForNewWindow(Selector obj, long timeout)
# boolean longClick(Selector obj)
# boolean longClick(Selector obj, String corner)
# boolean dragTo(Selector obj, Selector destObj, int steps)
# boolean dragTo(Selector obj, int destX, int destY, int steps)
# boolean exist(Selector obj)
# ObjInfo objInfo(Selector obj)
# int count(Selector obj)
# ObjInfo[] objInfoOfAllInstances(Selector obj)
# boolean gesture(Selector obj, Point startPoint1, Point startPoint2, Point endPoint1, Point endPoint2, int steps)
# boolean pinchIn(Selector obj, int percent, int steps)
# boolean pinchOut(Selector obj, int percent, int steps)
# boolean swipe(Selector obj, String dir, int steps)
# boolean swipe(Selector obj, String dir, float percent, int steps)
# boolean waitForExists(Selector obj, long timeout) 
# boolean waitUntilGone(Selector obj, long timeout)
# boolean flingBackward(Selector obj, boolean isVertical)
# boolean flingForward(Selector obj, boolean isVertical)
# boolean flingToBeginning(Selector obj, boolean isVertical, int maxSwipes)
# boolean flingToEnd(Selector obj, boolean isVertical, int maxSwipes)
# boolean scrollBackward(Selector obj, boolean isVertical, int steps)
# boolean scrollForward(Selector obj, boolean isVertical, int steps)
# boolean scrollToBeginning(Selector obj, boolean isVertical, int maxSwipes, int steps)
# boolean scrollToEnd(Selector obj, boolean isVertical, int maxSwipes, int steps)
# boolean scrollTo(Selector obj, Selector targetObj, boolean isVertical)
# String childByText(Selector collection, Selector child, String text)
# String childByText(Selector collection, Selector child, String text, boolean allowScrollSearch)
# String childByDescription(Selector collection, Selector child, String text)
# String childByDescription(Selector collection, Selector child, String text, boolean allowScrollSearch)
# String childByInstance(Selector collection, Selector child, int instance)
# String getChild(String obj, Selector selector) # obj      The ID string represent the parent UiObject.
# String getFromParent(String obj, Selector selector)
# String getUiObject(Selector selector)

#
# 接口感觉有点多呀，感觉可以删除一点了
#
# void removeUiObject(String obj) # Remove the UiObject from memory.
# String[] getUiObjects()
# void clearTextField(String obj)
# String getText(String obj)
# boolean setText(String obj, String text)
# boolean click(String obj)
# boolean click(String obj, String corner)
# boolean clickAndWaitForNewWindow(String obj, long timeout)
# boolean longClick(String obj)
# boolean longClick(String obj, String corner)
# boolean dragTo(String obj, Selector destObj, int steps)
# boolean dragTo(String obj, int destX, int destY, int steps)
# boolean exist(String obj)
# ObjInfo objInfo(String obj)
# boolean gesture(String obj, Point startPoint1, Point startPoint2, Point endPoint1, Point endPoint2, int steps)
# boolean pinchIn(String obj, int percent, int steps)
# boolean pinchOut(String obj, int percent, int steps)
# boolean swipe(String obj, String dir, int steps)
# boolean waitForExists(String obj, long timeout)
# boolean waitUntilGone(String obj, long timeout)




