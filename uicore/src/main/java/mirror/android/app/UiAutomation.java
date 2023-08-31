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
    public static RefMethod<Void> connectWithTimeout;

//    @MethodParams({int.class})
    // Android N 以下，只有connect()接口
    // Android N 以上，connect(int)接口
    // http://aospxref.com/android-6.0.1_r9/xref/frameworks/base/core/java/android/app/UiAutomation.java#186
    // http://aospxref.com/android-7.1.2_r39/xref/frameworks/base/core/java/android/app/UiAutomation.java#212

    @MethodParams({int.class})
    public static RefMethod<Object> connect;

    public static RefMethod<Object> disconnect;

    // >= Android 7.0
    public static RefMethod<Boolean> isDestroyed;

    public static RefMethod<Void> destroy;

    public static RefMethod<Integer> getFlags;
}
