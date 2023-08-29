package mirror.android.view.accessibility;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class AccessibilityWindowInfo {
    public static Class<?> TYPE = RefClass.load(AccessibilityWindowInfo.class, "android.view.accessibility.AccessibilityWindowInfo");

    @MethodParams({int.class})
    public static RefStaticMethod<String> typeToString;
}
