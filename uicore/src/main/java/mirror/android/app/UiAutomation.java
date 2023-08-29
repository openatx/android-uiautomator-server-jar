package mirror.android.app;

import mirror.MethodParams;
import mirror.MethodReflectParams;
import mirror.RefClass;
import mirror.RefConstructor;
import mirror.RefMethod;

public class UiAutomation {
    public static Class<?> TYPE = RefClass.load(UiAutomation.class, "android.app.UiAutomation");

    @MethodReflectParams({"android.os.Looper", "android.app.IUiAutomationConnection"})
    public static RefConstructor<android.app.UiAutomation> ctor;

    @MethodParams({int.class, long.class})
    public static RefMethod<Object> connectWithTimeout;

    @MethodParams({int.class})
    public static RefMethod<Object> connect;

    public static RefMethod<Object> disconnect;
}
