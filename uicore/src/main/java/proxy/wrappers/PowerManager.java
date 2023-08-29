package proxy.wrappers;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.IInterface;

import com.genymobile.scrcpy.Ln;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PowerManager {
    private final IInterface manager;
    private Method isScreenOnMethod;

    public PowerManager(IInterface manager) {
        if (manager == null) {
            Ln.w("18 PowerManager is null");
        } else {
            Ln.w("18 PowerManager is not null");
        }
        this.manager = manager;
    }

    private Method getIsScreenOnMethod() throws NoSuchMethodException {
        if (isScreenOnMethod == null) {
            @SuppressLint("ObsoleteSdkInt") // we may lower minSdkVersion in the future
                    String methodName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH ? "isInteractive" : "isScreenOn";
            if (manager == null) {
                Ln.w("30 PowerManager is null");
            } else {
                Ln.w("30 PowerManager is not null");
                Class clz = manager.getClass();
                Ln.w("clz == null : " + (clz == null));
                if (clz != null) {
                    Method method = clz.getMethod(methodName);
                    Ln.w("method == null : " + (method == null));
                    if (method != null) {
                        isScreenOnMethod = method;
                        return method;
                    }
                }
            }
            isScreenOnMethod = manager.getClass().getMethod(methodName);
        }
        return isScreenOnMethod;
    }

    public boolean isScreenOn() {
        try {
            Method method = getIsScreenOnMethod();
            return (boolean) method.invoke(manager);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            Ln.e("Could not invoke method", e);
            return false;
        }
    }
}
