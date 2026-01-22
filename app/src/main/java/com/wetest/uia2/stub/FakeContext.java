package com.wetest.uia2.stub;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;

import com.genymobile.scrcpy.Workarounds;

import java.lang.reflect.Field;

public final class FakeContext extends ContextWrapper {

    private static final FakeContext INSTANCE = new FakeContext();
    public static final String PACKAGE_NAME = "com.android.shell";
    public static final int ROOT_UID = 0; // Like android.os.Process.ROOT_UID, but before API 29

    public static FakeContext get() {
        return INSTANCE;
    }

    private FakeContext() {
        super(Workarounds.getSystemContext());
    }


    @Override
    public String getPackageName() {
        return PACKAGE_NAME;
    }

    @Override
    public String getOpPackageName() {
        return PACKAGE_NAME;
    }


    // @Override to be added on SDK upgrade for Android 14
    @SuppressWarnings("unused")
    public int getDeviceId() {
        return 0;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public Context createPackageContext(String packageName, int flags) {
        return this;
    }

    @SuppressLint("SoonBlockedPrivateApi")
    @Override
    public Object getSystemService(String name) {
        Object service = super.getSystemService(name);
        if (service == null) {
            return null;
        }

        // "semclipboard" is a Samsung-internal service
        // See:
        //  - <https://github.com/Genymobile/scrcpy/issues/6224>
        //  - <https://github.com/Genymobile/scrcpy/issues/6523>
        if (Context.CLIPBOARD_SERVICE.equals(name) || "semclipboard".equals(name) || Context.ACTIVITY_SERVICE.equals(name)) {
            try {
                Field field = service.getClass().getDeclaredField("mContext");
                field.setAccessible(true);
                field.set(service, this);
            }
            catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        return service;
    }
}
