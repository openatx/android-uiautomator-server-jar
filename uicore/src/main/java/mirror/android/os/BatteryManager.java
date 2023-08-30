package mirror.android.os;

import mirror.MethodParams;
import mirror.MethodReflectParams;
import mirror.RefClass;
import mirror.RefConstructor;

public class BatteryManager {
    public static Class<?> TYPE = RefClass.load(BatteryManager.class, android.os.BatteryManager.class);
    public static RefConstructor<android.os.BatteryManager> ctor;
}
