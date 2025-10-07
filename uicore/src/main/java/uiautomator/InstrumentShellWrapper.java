package uiautomator;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
import android.os.HandlerThread;
import android.app.UiAutomation;

import proxy.Bridge;

public class InstrumentShellWrapper extends Instrumentation {
    private static final String HANDLER_THREAD_NAME = "UiAutomatorHandlerThread";
    private static final long CONNECT_TIMEOUT_MILLIS = 5000;

    private final HandlerThread mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);

    private UiAutomation mUiAutomation;
    private Object mUiAutomationConnection = mirror.android.app.UiAutomationConnection.ctor.newInstance();

    private static InstrumentShellWrapper sInstance;
    private InstrumentShellWrapper() {
        mHandlerThread.start();
    }

    public static InstrumentShellWrapper getInstance() {
        if (sInstance == null) {
            sInstance = new InstrumentShellWrapper();
        }
        return sInstance;
    }

    @Override
    public Context getContext() {
        return Bridge.getInstance().getContext();
    }

    @Override
    public Context getTargetContext() {
        return Bridge.getInstance().getContext();
    }

    public void destroy() {
        if (!mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already disconnected!");
        }
        mHandlerThread.quit();
    }
    /*
    public void connect() {
        if (mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already connected!");
        }
        mHandlerThread.start();

        mUiAutomation = mirror.android.app.UiAutomation.ctor.newInstance(mHandlerThread.getLooper(), mUiAutomationConnection);

        mirror.android.app.UiAutomation.connect.call(mUiAutomation, 0);
    }

    public void disconnect() {
        if (!mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already disconnected!");
        }

        mirror.android.app.UiAutomation.disconnect.call(mUiAutomation);
        mHandlerThread.quit();
    }
     */

    public void setCompressedLayoutHierarchy(boolean compressed) {
        if (mUiAutomation == null) {
            // UiAutomation is not initialized yet
            return;
        }
        AccessibilityServiceInfo info = mUiAutomation.getServiceInfo();
        if (info == null) {
            // Service info can be null if UiAutomation is not connected
            return;
        }
        if (compressed)
            info.flags &= ~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        else
            info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        mUiAutomation.setServiceInfo(info);
    }

    // from android.app.Instrument
    @TargetApi(Build.VERSION_CODES.N)
    public UiAutomation getUiAutomation(int flags) {
        boolean mustCreateNewAutomation = (mUiAutomation == null) || (mirror.android.app.UiAutomation.isDestroyed.call(mUiAutomation));

        if (mUiAutomationConnection != null) {
            if (!mustCreateNewAutomation && (mirror.android.app.UiAutomation.getFlags.call(mUiAutomation) == flags)) {
                return mUiAutomation;
            }
            if (mustCreateNewAutomation) {
                mUiAutomation = mirror.android.app.UiAutomation.ctor.newInstance(mHandlerThread.getLooper(),
                        mUiAutomationConnection);
            } else {
                mirror.android.app.UiAutomation.disconnect.call(mUiAutomation);
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                mirror.android.app.UiAutomation.connect.call(mUiAutomation, flags);
                return mUiAutomation;
            }
            try {
                mirror.android.app.UiAutomation.connectWithTimeout.call(mUiAutomation, flags,  CONNECT_TIMEOUT_MILLIS);
                return mUiAutomation;
            } catch (Exception e) {
                mirror.android.app.UiAutomation.destroy.call(mUiAutomation);
                mUiAutomation = null;
            }
        }
        return null;
    }

    @Override
    public UiAutomation getUiAutomation() {
        if (mUiAutomation != null) {
            return mUiAutomation;
        }

        mUiAutomation = mirror.android.app.UiAutomationM.ctor.newInstance(mHandlerThread.getLooper(),
                mUiAutomationConnection);
        mirror.android.app.UiAutomationM.connect.call(mUiAutomation);
        return mUiAutomation;
    }
}
