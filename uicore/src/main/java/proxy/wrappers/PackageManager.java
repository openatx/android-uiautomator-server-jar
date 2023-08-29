package com.genymobile.scrcpy.wrappers;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.IInterface;

import com.genymobile.scrcpy.Ln;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class PackageManager {
    private final IInterface manager;
    private Method getPackageInfoMethod;
    private Method getAllPackagesMethod;

    public PackageManager(IInterface manager) {
        this.manager = manager;
    }

    private Method getAllPackagesMethod() throws NoSuchMethodException {
        if (getAllPackagesMethod == null) {
            String methodName = "getAllPackages";
            getAllPackagesMethod = manager.getClass().getMethod(methodName);
        }
        return getAllPackagesMethod;
    }

    private Method getPackageInfoMethod() throws NoSuchMethodException {
        if (getPackageInfoMethod == null) {
            String methodName = "getPackageInfo";
            getPackageInfoMethod = manager.getClass().getMethod(methodName,
                    String.class, int.class, int.class);
        }
        return getPackageInfoMethod;
    }

    @SuppressWarnings("unchecked")
    public List<String> getAllPackages() {
        List<String> pkgList = new ArrayList<>();
        try {
            Method method = getAllPackagesMethod();
            pkgList = (List<String>)method.invoke(manager);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            Ln.e("Could not invoke method", e);
        }
        return pkgList;
    }

    @SuppressWarnings("unchecked")
    public List<PackageInfo> getInstalledPackages() {
        List<PackageInfo> packageInfos = new ArrayList();
        try {
            List<String> allPackages = getAllPackages();
            Method pkgInfoMethod = getPackageInfoMethod();
            for (int i = 0; i < allPackages.size(); i++) {
                String pkg = allPackages.get(i);
                PackageInfo pkgInfo = (PackageInfo) pkgInfoMethod.invoke(manager, pkg, 0 , 0);
                if (pkgInfo != null && (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    packageInfos.add(pkgInfo);
                }
            }
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            Ln.e("Could not invoke method", e);
        }
        return packageInfos;
    }
}
