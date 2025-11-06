package proxy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.UiAutomation;
import android.content.Context;
import android.graphics.Point;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import mirror.android.app.ActivityThread;
import mirror.android.hardware.display.DisplayManagerGlobal;
import proxy.wrappers.ClipboardManager;
import proxy.wrappers.InputManager;
import proxy.wrappers.ServiceManager;
import uiautomator.AccessibilityNodeInfoDumper;
import uiautomator.AccessibilityNodeInfoDumper.DumpWindowException;
import uiautomator.InstrumentShellWrapper;
import utils.compat.BuildCompat;

public class Bridge {
    Context context;
    BatteryManager batteryManager;
    Object activityThread;
    Object displayManager;

    ServiceManager serviceManager = new ServiceManager();
    static private Bridge sInstance;
    private Bridge () {
    }

    public static Bridge getInstance() {
        if (sInstance == null) {
            sInstance = new Bridge();
        }
        return sInstance;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public int[] getDisplayIds() {
        if (displayManager == null) {
            displayManager = DisplayManagerGlobal.getInstance.call();
        }
        int[] displayIds = DisplayManagerGlobal.getDisplayIds.call(displayManager);
        return displayIds;
    }

    Display getDisplayById(int displayId) {
        if (displayManager == null) {
            displayManager = DisplayManagerGlobal.getInstance.call();
        }
        return DisplayManagerGlobal.getRealDisplay.call(displayManager, displayId);
    }

    public int getDisplayDensityDpi(int displayId) {
        Object displayManager = DisplayManagerGlobal.getInstance.call();
        Display display = DisplayManagerGlobal.getRealDisplay.call(displayManager, displayId);
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        return metrics.densityDpi;
    }

    public DisplayMetrics getDisplayMetrics(int displayId) {
        Object displayManager = DisplayManagerGlobal.getInstance.call();
        Display display = DisplayManagerGlobal.getRealDisplay.call(displayManager, displayId);
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        return metrics;
    }

    @TargetApi(Build.VERSION_CODES.R)
    public String dumpWindows( int displayId, boolean verboseMode) throws DumpWindowException, IOException {
        UiAutomation uiAutomation = InstrumentShellWrapper.getInstance().getUiAutomation();
        if (verboseMode) {
            // default
            InstrumentShellWrapper.getInstance().setCompressedLayoutHierarchy(false);
        } else {
            InstrumentShellWrapper.getInstance().setCompressedLayoutHierarchy(true);
        }
        AccessibilityServiceInfo info = uiAutomation.getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            uiAutomation.setServiceInfo(info);
        }
        SparseArray<List<AccessibilityWindowInfo>> allWindows = uiAutomation.getWindowsOnAllDisplays();
        if (allWindows.size() == 0) {
            throw new DumpWindowException("windows empty");
        }

        for (int d = 0, nd = allWindows.size(); d < nd; ++d) {
            if (allWindows.keyAt(d) == displayId) {
                return AccessibilityNodeInfoDumper.dumpWindows(allWindows.valueAt(d), displayId);
            }
        }
        throw new DumpWindowException("display not found");
    }

    public String dumpXml(boolean allWindows, boolean verboseMode) {
        InstrumentShellWrapper.getInstance().setCompressedLayoutHierarchy(!verboseMode);
        UiAutomation uiAutomation = InstrumentShellWrapper.getInstance().getUiAutomation();
        // >= Android 11
        if (allWindows && BuildCompat.isR()) {
            AccessibilityServiceInfo info = uiAutomation.getServiceInfo();
            if (info != null) {
                info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
                uiAutomation.setServiceInfo(info);
            }
            return AccessibilityNodeInfoDumper.dumpWindows(uiAutomation.getWindowsOnAllDisplays());
        } else {
            // 注意：这里代码执行顺序不同，如果顺序一致，info会返回空，原因未知
            // http://aospxref.com/android-7.0.0_r7/xref/frameworks/base/cmds/uiautomator/cmds/uiautomator/src/com/android/commands/uiautomator/DumpCommand.java#86
            Object displayManager = DisplayManagerGlobal.getInstance.call();
            Display display = DisplayManagerGlobal.getRealDisplay.call(displayManager, Display.DEFAULT_DISPLAY);
            int rotation = display.getRotation();
            Point size = new Point();
            display.getRealSize(size);

            AccessibilityNodeInfo info = uiAutomation.getRootInActiveWindow();
            if (info == null) {
                System.err.println("ERROR: null root node returned by UiTestAutomationBridge.");
                return "";
            }
            return AccessibilityNodeInfoDumper.dumpWindow(info, rotation, size.x, size.y);
        }
    }

    public static final int BATTERY_STATUS_UNKNOWN = 1;
    public static final int BATTERY_STATUS_CHARGING = 2;
    public static final int BATTERY_STATUS_DISCHARGING = 3;
    public static final int BATTERY_STATUS_NOT_CHARGING = 4;
    public static final int BATTERY_STATUS_FULL = 5;
    public static class BatteryInfo {
        public int capacity; // 当前手机剩余电量百分比 0-100
        public int currentAverage;
        public int currentNow;
        public int chargeCounter;
        public boolean isCharging;
    }
    public BatteryInfo getBatteryInfo() {
//        Object batteryPropertiesRegistrar = IBatteryPropertiesRegistrar.Stub.asInterface.call(ServiceManager.getService.call("batteryproperties"));
        if (batteryManager == null) {
            batteryManager = mirror.android.os.BatteryManager.ctor.newInstance();
        }
        int capctity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        int average = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        int current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        int counter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        BatteryInfo info = new BatteryInfo();
        info.capacity = capctity;
        info.currentAverage = average;
        info.chargeCounter = counter;
        info.currentNow = current;
        if (BuildCompat.isM()) {
            info.isCharging = batteryManager.isCharging();
        }
        return info;
    }

    // 该方案来源：https://testerhome.com/topics/31713
    // 不能确定这个方案是否会导致后台运行时是否足够稳定，十分怀疑perfdog也采用了此方案实现
    public Context getContext() {
        if (activityThread == null) {
            Looper.prepareMainLooper();
            activityThread = ActivityThread.systemMain.call();
            context = ActivityThread.getSystemContext.call(activityThread);
        }
        return context;
    }

    public ClipboardManager getClipboardManager() {
        return serviceManager.getClipboardManager();
    }

    public InputManager getInputManager() {
        return serviceManager.getInputManager();
    }

    public static byte[] takeScreenshot() throws IOException, InterruptedException {
        Process proc = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(new String[]{"screencap", "-p"});
            proc = processBuilder.start();

            byte[] pngBytes = IOUtils.toByteArray(proc.getInputStream());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                proc.waitFor(5*1000, TimeUnit.MILLISECONDS);
            } else {
                proc.waitFor();
            }
            return pngBytes;
        } catch (IOException | InterruptedException e) {
            throw e;
        } finally {
            try {
                if (null != proc) {
                    proc.getInputStream().close();
                    proc.getOutputStream().close();
                    proc.getErrorStream().close();
                }
            } catch (IOException e) {
            }
        }
    }
}
