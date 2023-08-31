package mirror.android.hardware.display;

import android.os.IInterface;
import android.view.Display;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;
import mirror.RefObject;
import mirror.RefStaticMethod;

public class DisplayManagerGlobal {
    public static Class<?> TYPE = RefClass.load(DisplayManagerGlobal.class, "android.hardware.display.DisplayManagerGlobal");
    public static RefStaticMethod<Object> getInstance;
    public static RefObject<IInterface> mDm;

    @MethodParams({int.class})
    public static RefMethod<Display> getRealDisplay;

    public static RefMethod<int[]> getDisplayIds;
}
