package mirror.android.app;

import mirror.MethodReflectParams;
import mirror.RefClass;
import mirror.RefConstructor;
import mirror.RefMethod;

public class UiAutomationM {
    public static Class<?> TYPE = RefClass.load(UiAutomationM.class, "android.app.UiAutomation");

    @MethodReflectParams({"android.os.Looper", "android.app.IUiAutomationConnection"})
    public static RefConstructor<android.app.UiAutomation> ctor;

    // Android N 以下，只有connect()接口
    // http://aospxref.com/android-6.0.1_r9/xref/frameworks/base/core/java/android/app/UiAutomation.java#186

    public static RefMethod<Object> connect;

    public static RefMethod<Object> disconnect;
}
