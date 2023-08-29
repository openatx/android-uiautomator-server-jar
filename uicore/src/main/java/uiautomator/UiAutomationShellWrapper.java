package uiautomator;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.HandlerThread;
import android.app.UiAutomation;

public class UiAutomationShellWrapper {
    private static final String HANDLER_THREAD_NAME = "UiAutomatorHandlerThread";

    private final HandlerThread mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);

    private UiAutomation mUiAutomation;

    public void connect() {
        if (mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already connected!");
        }
        mHandlerThread.start();

        mUiAutomation = mirror.android.app.UiAutomation.ctor.newInstance(mHandlerThread.getLooper(),
                mirror.android.app.UiAutomationConnection.ctor.newInstance());

        mirror.android.app.UiAutomation.connect.call(mUiAutomation, 0);
    }

    public void disconnect() {
        if (!mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already disconnected!");
        }

        mirror.android.app.UiAutomation.disconnect.call(mUiAutomation);
        mHandlerThread.quit();
    }

    public UiAutomation getUiAutomation() {
        return mUiAutomation;
    }

    public void setCompressedLayoutHierarchy(boolean compressed) {
        AccessibilityServiceInfo info = mUiAutomation.getServiceInfo();
        if (compressed)
            info.flags &= ~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        else
            info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        mUiAutomation.setServiceInfo(info);
    }
}
