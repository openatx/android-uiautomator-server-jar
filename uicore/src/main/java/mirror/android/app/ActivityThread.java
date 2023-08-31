package mirror.android.app;

import android.content.Context;

import mirror.RefClass;
import mirror.RefMethod;
import mirror.RefStaticMethod;

public class ActivityThread {
    public static Class<?> TYPE = RefClass.load(ActivityThread.class, "android.app.ActivityThread");

    public static RefStaticMethod<Object> systemMain;

    public static RefMethod<Context> getSystemContext;
}
