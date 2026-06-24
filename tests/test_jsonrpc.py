#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import base64
import time
import pytest

from conftest import JSONRPCProxy, JSONRPCError


def test_raise_jsonrpc_error(jsonrpc: JSONRPCProxy):
    with pytest.raises(JSONRPCError):
        jsonrpc.not_exist_method()


def test_ping(jsonrpc: JSONRPCProxy):
    result = jsonrpc.ping()
    assert result == "pong"


def test_toast(jsonrpc: JSONRPCProxy):
    result = jsonrpc.clearLastToast()
    assert jsonrpc.getLastToast() is None
    

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
    result = jsonrpc.swipePoints([100, 100, 200, 200], 20)
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
    for args in [(True,), (False,), (False, 10), (False, 0), (False, 10, True), (False, 10, False)]:
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


# skip "power"
@pytest.mark.parametrize("key", ["home", "back", "left", "right", "up", "down", "center", "menu", "search", "enter", "delete", "del", "recent", "volume_up", "volume_down", "volume_mute", "camera"])
def test_pressKey(jsonrpc: JSONRPCProxy, key: str):
    # boolean pressKey(String key) 
    result = jsonrpc.pressKey(key)
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


def test_clipboard(jsonrpc: JSONRPCProxy):
    # void setClipboard(String label, String text)
    # String getClipboard()
    text = f"time is {time.time()}"
    jsonrpc.setClipboard("label", text)
    assert jsonrpc.getClipboard() == text


#
# String childByText(Selector collection, Selector child, String text)
# String childByText(Selector collection, Selector child, String text, boolean allowScrollSearch)
# String childByDescription(Selector collection, Selector child, String text)
# String childByDescription(Selector collection, Selector child, String text, boolean allowScrollSearch)
# String childByInstance(Selector collection, Selector child, int instance)
# String getChild(String obj, Selector selector) # obj      The ID string represent the parent UiObject.
# String getFromParent(String obj, Selector selector)
# String getUiObject(Selector selector)
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
