package mirror.android.app;

import mirror.MethodReflectParams;
import mirror.RefClass;
import mirror.RefConstructor;

public class ApplicationPackageManager {
    public static Class<?> TYPE = RefClass.load(ApplicationPackageManager.class, "android.app.ApplicationPackageManager");

    @MethodReflectParams({"android.app.ContextImpl", "android.content.pm.IPackageManager"})
    public static RefConstructor<?> ctor;
}
