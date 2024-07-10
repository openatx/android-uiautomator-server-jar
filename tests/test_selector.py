# coding: utf-8
#

# TODO
# 完成一个测试App，负责测试Click，LongClick, EditText, Toast, Drag, Swipe, Gesture等操作

import time
import pytest
import adbutils
from conftest import JSONRPCError, JSONRPCProxy

# copy from github.com/openatx/uiautomator2 _selector.py
class Selector(dict):
    """The class is to build parameters for UiSelector passed to Android device.
    """
    __fields = {
        "text": (0x01, None),  # MASK_TEXT,
        "textContains": (0x02, None),  # MASK_TEXTCONTAINS,
        "textMatches": (0x04, None),  # MASK_TEXTMATCHES,
        "textStartsWith": (0x08, None),  # MASK_TEXTSTARTSWITH,
        "className": (0x10, None),  # MASK_CLASSNAME
        "classNameMatches": (0x20, None),  # MASK_CLASSNAMEMATCHES
        "description": (0x40, None),  # MASK_DESCRIPTION
        "descriptionContains": (0x80, None),  # MASK_DESCRIPTIONCONTAINS
        "descriptionMatches": (0x0100, None),  # MASK_DESCRIPTIONMATCHES
        "descriptionStartsWith": (0x0200, None),  # MASK_DESCRIPTIONSTARTSWITH
        "checkable": (0x0400, False),  # MASK_CHECKABLE
        "checked": (0x0800, False),  # MASK_CHECKED
        "clickable": (0x1000, False),  # MASK_CLICKABLE
        "longClickable": (0x2000, False),  # MASK_LONGCLICKABLE,
        "scrollable": (0x4000, False),  # MASK_SCROLLABLE,
        "enabled": (0x8000, False),  # MASK_ENABLED,
        "focusable": (0x010000, False),  # MASK_FOCUSABLE,
        "focused": (0x020000, False),  # MASK_FOCUSED,
        "selected": (0x040000, False),  # MASK_SELECTED,
        "packageName": (0x080000, None),  # MASK_PACKAGENAME,
        "packageNameMatches": (0x100000, None),  # MASK_PACKAGENAMEMATCHES,
        "resourceId": (0x200000, None),  # MASK_RESOURCEID,
        "resourceIdMatches": (0x400000, None),  # MASK_RESOURCEIDMATCHES,
        "index": (0x800000, 0),  # MASK_INDEX,
        "instance": (0x01000000, 0)  # MASK_INSTANCE,
    }
    __mask, __childOrSibling, __childOrSiblingSelector = "mask", "childOrSibling", "childOrSiblingSelector"

    def __init__(self, **kwargs):
        super(Selector, self).__setitem__(self.__mask, 0)
        super(Selector, self).__setitem__(self.__childOrSibling, [])
        super(Selector, self).__setitem__(self.__childOrSiblingSelector, [])
        for k in kwargs:
            self[k] = kwargs[k]

    def __str__(self):
        """ remove useless part for easily debugger """
        selector = self.copy()
        selector.pop('mask')
        for key in ('childOrSibling', 'childOrSiblingSelector'):
            if not selector.get(key):
                selector.pop(key)
        args = []
        for (k, v) in selector.items():
            args.append(k + '=' + repr(v))
        return 'Selector [' + ', '.join(args) + ']'

    def __setitem__(self, k, v):
        if k in self.__fields:
            super(Selector, self).__setitem__(k, v)
            super(Selector,
                  self).__setitem__(self.__mask,
                                    self[self.__mask] | self.__fields[k][0])
        else:
            raise ReferenceError("%s is not allowed." % k)

    def __delitem__(self, k):
        if k in self.__fields:
            super(Selector, self).__delitem__(k)
            super(Selector,
                  self).__setitem__(self.__mask,
                                    self[self.__mask] & ~self.__fields[k][0])

    def clone(self):
        kwargs = dict((k, self[k]) for k in self if k not in [
            self.__mask, self.__childOrSibling, self.__childOrSiblingSelector
        ])
        selector = Selector(**kwargs)
        for v in self[self.__childOrSibling]:
            selector[self.__childOrSibling].append(v)
        for s in self[self.__childOrSiblingSelector]:
            selector[self.__childOrSiblingSelector].append(s.clone())
        return selector

    def child(self, **kwargs):
        self[self.__childOrSibling].append("child")
        self[self.__childOrSiblingSelector].append(Selector(**kwargs))
        return self

    def sibling(self, **kwargs):
        self[self.__childOrSibling].append("sibling")
        self[self.__childOrSiblingSelector].append(Selector(**kwargs))
        return self

    def update_instance(self, i):
        # update inside child instance
        if self[self.__childOrSiblingSelector]:
            self[self.__childOrSiblingSelector][-1]['instance'] = i
        else:
            self['instance'] = i

@pytest.fixture(scope='function')
def app():
    # https://github.com/openatx/u2testdemo/releases/download/0.1.0/app-debug.apk
    dev = adbutils.device()
    dev.keyevent('WAKEUP')
    dev.app_start('com.example.u2testdemo')
    yield dev
    dev.app_stop('com.example.u2testdemo')
 

def test_exist_count(jsonrpc: JSONRPCProxy):
    # boolean exist(Selector obj)
    # int count(Selector obj)
    selector = {'mask': 1,
                'childOrSibling': [],
                'childOrSiblingSelector': [],
                'text': '12345'}
    exists = jsonrpc.exist(selector)
    assert exists == False
    assert jsonrpc.count(selector) == 0


add1 = Selector(className='android.widget.EditText', index=0)
add2 = Selector(className='android.widget.EditText', index=1)
button_add = Selector(text='Add')
answer = Selector(className='android.widget.EditText', instance=2)


def test_Selector(app, jsonrpc: JSONRPCProxy):
    # boolean exist(Selector obj)
    # boolean waitForExists(Selector obj, long timeout)
    # boolean setText(Selector obj, String text)
    # String getText(Selector obj)
    # boolean click(Selector obj)
    # void clearTextField(Selector obj)
    exists = jsonrpc.waitForExists(add1, 10000)
    assert exists == True
    
    assert jsonrpc.exist(add2) == True
    
    jsonrpc.setText(add1, '123')
    time.sleep(.5)
    assert jsonrpc.getText(add1) == '123'
    jsonrpc.clearTextField(add1)
    time.sleep(.5)
    assert jsonrpc.getText(add1) == ''
    
    jsonrpc.setText(add1, '1')
    jsonrpc.setText(add2, '2')
    jsonrpc.click(button_add)
    time.sleep(.1)
    assert jsonrpc.getText(answer) == '3'
    
    assert jsonrpc.count(Selector(className='android.widget.EditText')) == 3


def test_objInfo(app, jsonrpc: JSONRPCProxy):
    # ObjInfo objInfo(Selector obj)
    jsonrpc.waitForExists(add1, 10000)
    info = jsonrpc.objInfo(add1)
    assert isinstance(info, dict)
    assert info['className'] == add1['className']
    
    results = jsonrpc.objInfoOfAllInstances(Selector(className='android.widget.EditText'))
    assert len(results) == 3
    
    with pytest.raises(JSONRPCError, match='UiObjectNotFoundException'):
        jsonrpc.objInfo(Selector(text='never-exists'))
    
# TODO
# boolean click(Selector obj, String corner)
# boolean clickAndWaitForNewWindow(Selector obj, long timeout)
# boolean longClick(Selector obj)
# boolean longClick(Selector obj, String corner)
# boolean dragTo(Selector obj, Selector destObj, int steps)
# boolean dragTo(Selector obj, int destX, int destY, int steps)

# ObjInfo[] objInfoOfAllInstances(Selector obj)
# boolean gesture(Selector obj, Point startPoint1, Point startPoint2, Point endPoint1, Point endPoint2, int steps)
# boolean pinchIn(Selector obj, int percent, int steps)
# boolean pinchOut(Selector obj, int percent, int steps)
# boolean swipe(Selector obj, String dir, int steps)
# boolean swipe(Selector obj, String dir, float percent, int steps)

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
