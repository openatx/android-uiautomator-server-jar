package proxy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Service;
import android.app.UiAutomation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.accessibility.AccessibilityWindowInfo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import mirror.android.app.ActivityThread;
import mirror.android.hardware.display.DisplayManagerGlobal;
import proxy.wrappers.ClipboardManager;
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
        info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        uiAutomation.setServiceInfo(info);
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
